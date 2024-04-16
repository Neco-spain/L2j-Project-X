package com.px.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import com.px.commons.math.MathUtil;
import com.px.commons.pool.ThreadPool;

import com.px.Config;
import com.px.gameserver.data.manager.CursedWeaponManager;
import com.px.gameserver.enums.BossInfoType;
import com.px.gameserver.enums.DropType;
import com.px.gameserver.model.actor.Attackable;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.model.actor.Playable;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.actor.Summon;
import com.px.gameserver.model.actor.container.monster.OverhitState;
import com.px.gameserver.model.actor.container.monster.SeedState;
import com.px.gameserver.model.actor.container.monster.SpoilState;
import com.px.gameserver.model.actor.container.npc.AbsorbInfo;
import com.px.gameserver.model.actor.container.npc.AggroInfo;
import com.px.gameserver.model.actor.container.npc.RewardInfo;
import com.px.gameserver.model.actor.template.NpcTemplate;
import com.px.gameserver.model.group.CommandChannel;
import com.px.gameserver.model.group.Party;
import com.px.gameserver.model.holder.IntIntHolder;
import com.px.gameserver.model.item.DropCategory;
import com.px.gameserver.model.item.instance.ItemInstance;
import com.px.gameserver.network.SystemMessageId;
import com.px.gameserver.network.serverpackets.SystemMessage;
import com.px.gameserver.skills.L2Skill;

/**
 * A monster extends {@link Attackable} class.<br>
 * <br>
 * It is an attackable {@link Creature}, with the capability to hold minions/master.
 */
public class Monster extends Attackable
{
	private final Map<Integer, AbsorbInfo> _absorbersList = new ConcurrentHashMap<>();
	
	private final OverhitState _overhitState = new OverhitState(this);
	private final SpoilState _spoilState = new SpoilState();
	private final SeedState _seedState = new SeedState(this);
	
	private ScheduledFuture<?> _ccTask;
	
	private CommandChannel _firstCcAttacker;
	
	private long _lastCcAttack;
	
	private boolean _isRaidRelated;
	
	public Monster(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}
	
	@Override
	protected void calculateRewards(Creature creature)
	{
		if (getAI().getAggroList().isEmpty())
			return;
		
		// Creates an empty list of rewards.
		final Map<Creature, RewardInfo> rewards = new ConcurrentHashMap<>();
		
		Player maxDealer = null;
		double maxDamage = 0.;
		double totalDamage = 0.;
		
		// Go through the aggro list.
		for (AggroInfo info : getAI().getAggroList().values())
		{
			if (!(info.getAttacker() instanceof Playable))
				continue;
			
			// Get the Playable corresponding to this attacker.
			final Playable attacker = (Playable) info.getAttacker();
			
			// Get damages done by this attacker.
			final double damage = info.getDamage();
			if (damage <= 1)
				continue;
			
			// Check if attacker isn't too far from this.
			if (!MathUtil.checkIfInRange(Config.PARTY_RANGE, this, attacker, true))
				continue;
			
			final Player attackerPlayer = attacker.getActingPlayer();
			
			totalDamage += damage;
			
			// Calculate real damages (Summoners should get own damage plus summon's damage).
			RewardInfo reward = rewards.get(attacker);
			if (reward == null)
			{
				reward = new RewardInfo(attacker);
				rewards.put(attacker, reward);
			}
			reward.addDamage(damage);
			
			if (attacker instanceof Summon)
			{
				reward = rewards.get(attackerPlayer);
				if (reward == null)
				{
					reward = new RewardInfo(attackerPlayer);
					rewards.put(attackerPlayer, reward);
				}
				reward.addDamage(damage);
			}
			
			if (reward.getDamage() > maxDamage)
			{
				maxDealer = attackerPlayer;
				maxDamage = reward.getDamage();
			}
		}
		
		// Command channel restriction ; if a CC is registered, the main contributor is the channel leader, no matter the participation of the channel, and no matter the damage done by other participants.
		if (_firstCcAttacker != null)
			maxDealer = _firstCcAttacker.getLeader();
		
		// Manage Base, Quests and Sweep drops.
		doItemDrop((maxDealer != null && maxDealer.isOnline()) ? maxDealer : creature);
		
		for (RewardInfo reward : rewards.values())
		{
			if (reward.getAttacker() instanceof Summon)
				continue;
			
			// Attacker to be rewarded.
			final Player attacker = reward.getAttacker().getActingPlayer();
			
			// Total amount of damage done.
			final double damage = reward.getDamage();
			
			// Get party.
			final Party attackerParty = attacker.getParty();
			if (attackerParty == null)
			{
				// Calculate Exp and SP rewards.
				if (!attacker.isDead() && attacker.knows(this))
				{
					final int levelDiff = attacker.getStatus().getLevel() - getStatus().getLevel();
					final float penalty = (attacker.hasServitor()) ? ((Servitor) attacker.getSummon()).getExpPenalty() : 0;
					final int[] expSp = calculateExpAndSp(levelDiff, damage, totalDamage);
					
					long exp = expSp[0];
					int sp = expSp[1];
					
					exp *= 1 - penalty;
					
					// Test over-hit.
					if (_overhitState.isValidOverhit(attacker))
					{
						attacker.sendPacket(SystemMessageId.OVER_HIT);
						exp += _overhitState.calculateOverhitExp(exp);
					}
					
					// Set new karma.
					attacker.updateKarmaLoss(exp);
					
					// Distribute the Exp and SP.
					attacker.addExpAndSp(exp, sp, rewards);
				}
			}
			// Share with party members.
			else
			{
				double partyDmg = 0.;
				double partyMul = 1.;
				
				int partyLvl = 0;
				
				final List<Player> rewardedMembers = new ArrayList<>();
				final Map<Creature, RewardInfo> playersWithPets = new HashMap<>();
				
				// Iterate every Party member.
				for (Player partyPlayer : (attackerParty.isInCommandChannel()) ? attackerParty.getCommandChannel().getMembers() : attackerParty.getMembers())
				{
					if (partyPlayer == null || partyPlayer.isDead())
						continue;
					
					// Add Player of the Party (that have attacked or not) to members that can be rewarded and in range of the monster.
					final boolean isInRange = MathUtil.checkIfInRange(Config.PARTY_RANGE, this, partyPlayer, true);
					if (isInRange)
					{
						rewardedMembers.add(partyPlayer);
						
						if (partyPlayer.getStatus().getLevel() > partyLvl)
							partyLvl = (attackerParty.isInCommandChannel()) ? attackerParty.getCommandChannel().getLevel() : partyPlayer.getStatus().getLevel();
					}
					
					// Retrieve the associated RewardInfo, if any.
					final RewardInfo reward2 = rewards.get(partyPlayer);
					if (reward2 != null)
					{
						// Add Player damages to Party damages.
						if (isInRange)
							partyDmg += reward2.getDamage();
						
						// Remove the Player from the rewards.
						rewards.remove(partyPlayer);
						
						playersWithPets.put(partyPlayer, reward2);
						if (partyPlayer.hasPet() && rewards.containsKey(partyPlayer.getSummon()))
							playersWithPets.put(partyPlayer.getSummon(), rewards.get(partyPlayer.getSummon()));
					}
				}
				
				// If the Party didn't kill this Monster alone, calculate their part.
				if (partyDmg < totalDamage)
					partyMul = partyDmg / totalDamage;
				
				// Calculate the level difference between Party and this Monster.
				final int levelDiff = partyLvl - getStatus().getLevel();
				
				// Calculate Exp and SP rewards.
				final int[] expSp = calculateExpAndSp(levelDiff, partyDmg, totalDamage);
				
				long exp = (long) (expSp[0] * partyMul);
				int sp = (int) (expSp[1] * partyMul);
				
				// Test over-hit.
				if (_overhitState.isValidOverhit(attacker))
				{
					attacker.sendPacket(SystemMessageId.OVER_HIT);
					exp += _overhitState.calculateOverhitExp(exp);
				}
				
				// Distribute Experience and SP rewards to Player Party members in the known area of the last attacker.
				if (partyDmg > 0)
					attackerParty.distributeXpAndSp(exp, sp, rewardedMembers, partyLvl, playersWithPets);
			}
		}
	}
	
	@Override
	public boolean isAggressive()
	{
		return getTemplate().getAggroRange() > 0;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		// Clear over-hit state.
		_overhitState.clear();
		
		// Clear spoil state.
		_spoilState.clear();
		
		// Clear seed state.
		_seedState.clear();
		
		_absorbersList.clear();
	}
	
	@Override
	public boolean isRaidBoss()
	{
		return _isRaidRelated && !hasMaster();
	}
	
	@Override
	public void reduceCurrentHp(double damage, Creature attacker, boolean awake, boolean isDOT, L2Skill skill)
	{
		if (attacker != null && isRaidBoss())
		{
			final Party party = attacker.getParty();
			if (party != null)
			{
				final CommandChannel cc = party.getCommandChannel();
				if (BossInfoType.isCcMeetCondition(cc, getNpcId()))
				{
					if (_ccTask == null)
					{
						_ccTask = ThreadPool.scheduleAtFixedRate(this::checkCcLastAttack, 1000, 1000);
						_lastCcAttack = System.currentTimeMillis();
						_firstCcAttacker = cc;
						
						// Broadcast message.
						broadcastOnScreen(10000, BossInfoType.getBossInfo(getNpcId()).getCcRightsMsg(), cc.getLeader().getName());
					}
					else if (_firstCcAttacker.equals(cc))
						_lastCcAttack = System.currentTimeMillis();
				}
			}
		}
		super.reduceCurrentHp(damage, attacker, awake, isDOT, skill);
	}
	
	@Override
	public boolean isAttackableWithoutForceBy(Playable attacker)
	{
		return isAttackableBy(attacker);
	}
	
	@Override
	public boolean isRaidRelated()
	{
		return _isRaidRelated;
	}
	
	/**
	 * Set this object as part of raid (it can be either a boss or a minion).<br>
	 * <br>
	 * This state affects behaviors such as auto loot configs, Command Channel acquisition, or even Config related to raid bosses.<br>
	 * <br>
	 * A raid boss can't be lethal-ed, and a raid curse occurs if the level difference is too high.
	 */
	public void setRaidRelated()
	{
		_isRaidRelated = true;
	}
	
	public OverhitState getOverhitState()
	{
		return _overhitState;
	}
	
	public SpoilState getSpoilState()
	{
		return _spoilState;
	}
	
	public SeedState getSeedState()
	{
		return _seedState;
	}
	
	/**
	 * Add a {@link Player} that successfully absorbed the soul of this {@link Monster} into the _absorbersList.
	 * @param player : The {@link Player} to test.
	 * @param crystal : The {@link ItemInstance} which was used to register.
	 */
	public void addAbsorber(Player player, ItemInstance crystal)
	{
		// If the Player isn't already in the _absorbersList, add it.
		AbsorbInfo ai = _absorbersList.get(player.getObjectId());
		if (ai == null)
		{
			// Create absorb info.
			_absorbersList.put(player.getObjectId(), new AbsorbInfo(crystal.getObjectId()));
		}
		else
		{
			// Add absorb info, unless already registered.
			if (!ai.isRegistered())
				ai.setItemId(crystal.getObjectId());
		}
	}
	
	/**
	 * Register a {@link Player} into this instance _absorbersList, setting the HP ratio. The {@link AbsorbInfo} must already exist.
	 * @param player : The {@link Player} to test.
	 */
	public void registerAbsorber(Player player)
	{
		// Get AbsorbInfo for user.
		AbsorbInfo ai = _absorbersList.get(player.getObjectId());
		if (ai == null)
			return;
		
		// Check item being used and register player to mob's absorber list.
		if (player.getInventory().getItemByObjectId(ai.getItemId()) == null)
			return;
		
		// Register AbsorbInfo.
		if (!ai.isRegistered())
		{
			ai.setAbsorbedHpPercent((int) getStatus().getHpRatio() * 100);
			ai.setRegistered(true);
		}
	}
	
	public AbsorbInfo getAbsorbInfo(int npcObjectId)
	{
		return _absorbersList.get(npcObjectId);
	}
	
	/**
	 * Calculate the XP and SP to distribute to the attacker of the {@link Monster}.
	 * @param diff : The difference of level between the attacker and the {@link Monster}.
	 * @param damage : The damages done by the attacker.
	 * @param totalDamage : The total damage done.
	 * @return an array consisting of xp and sp values.
	 */
	private int[] calculateExpAndSp(int diff, double damage, double totalDamage)
	{
		// Calculate damage ratio.
		double xp = getExpReward() * damage / totalDamage;
		double sp = getSpReward() * damage / totalDamage;
		
		// Calculate level ratio.
		if (diff > 5)
		{
			double pow = Math.pow((double) 5 / 6, diff - 5);
			xp = xp * pow;
			sp = sp * pow;
		}
		
		// If the XP is inferior or equals 0, don't reward any SP. Both XP and SP can't be inferior to 0.
		if (xp <= 0)
		{
			xp = 0;
			sp = 0;
		}
		else if (sp <= 0)
			sp = 0;
		
		return new int[]
		{
			(int) xp,
			(int) sp
		};
	}
	
	/**
	 * @param player : The {@link Player} to test.
	 * @return The multiplier for drop purpose, based on this instance and the {@link Player} set as parameter.
	 */
	private double calculateLevelMultiplier(Player player)
	{
		if (!Config.DEEPBLUE_DROP_RULES)
			return 1.;
		
		// Retrieve the highest attacker level, minus Monster level and a level limit (3 levels for raids, 6 for monsters).
		int levelDiff = getAttackByList().stream().mapToInt(c -> c.getStatus().getLevel()).max().orElse(player.getStatus().getLevel());
		levelDiff -= getStatus().getLevel();
		levelDiff -= isRaidBoss() ? 2 : 5;
		
		// Calculate the level multiplier based on the level difference. If the level difference is neutral or negative, there is no penalty.
		return (levelDiff <= 0) ? 1. : Math.max(0.1, 1 - 0.18 * levelDiff);
	}
	
	/**
	 * Manage drops of this {@link Monster} using an associated {@link NpcTemplate}.<br>
	 * <br>
	 * This method is called by {@link #calculateRewards}.
	 * @param creature : The {@link Creature} that made the most damage.
	 */
	public void doItemDrop(Creature creature)
	{
		if (creature == null)
			return;
		
		// Don't drop anything if the last attacker or owner isn't a Player.
		final Player player = creature.getActingPlayer();
		if (player == null)
			return;
		
		// Check Cursed Weapons drop.
		CursedWeaponManager.getInstance().checkDrop(this, player);
		
		// Calculate level multiplier.
		final double levelMultiplier = calculateLevelMultiplier(player);
		
		// Evaluate all drop categories.
		final boolean isSpoiled = getSpoilState().isSpoiled();
		final boolean isBlockingDrops = getSeedState().isSeeded() && !getSeedState().getSeed().isAlternative();
		final boolean isRaid = isRaidBoss();
		for (DropCategory category : getTemplate().getDropData())
		{
			final DropType type = category.getDropType();
			
			// Skip spoil categories, if not spoiled.
			if (type == DropType.SPOIL && !isSpoiled)
				continue;
			
			// Skip drop categories, if blocking drops.
			if (type == DropType.DROP && isBlockingDrops)
				continue;
			
			// Calculate drops of this category.
			for (IntIntHolder drop : category.calculateDrop(levelMultiplier, isRaid))
			{
				if (type == DropType.SPOIL)
					getSpoilState().add(drop);
				else if (type == DropType.HERB)
					dropOrAutoLootHerb(player, drop);
				else
					dropOrAutoLootItem(player, drop);
			}
		}
	}
	
	/**
	 * Drop on ground or auto loot a reward item, depending about activated {@link Config}s.
	 * @param player : The {@link Player} who made the highest damage contribution.
	 * @param holder : The {@link IntIntHolder} used for reward (item id / amount).
	 */
	private void dropOrAutoLootItem(Player player, IntIntHolder holder)
	{
		// Check Config.
		if (((isRaidBoss() && Config.AUTO_LOOT_RAID) || (!isRaidBoss() && Config.AUTO_LOOT)) && player.getInventory().validateCapacityByItemId(holder))
		{
			if (player.isInParty())
				player.getParty().distributeItem(player, holder, false, this);
			else if (holder.getId() == 57)
				player.addAdena("Loot", holder.getValue(), this, true);
			else
				player.addItem("Loot", holder.getId(), holder.getValue(), this, true);
		}
		else
			dropItem(holder, player);
		
		// Broadcast message if RaidBoss was defeated.
		if (isRaidBoss())
			broadcastPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DIED_DROPPED_S3_S2).addCharName(this).addItemName(holder.getId()).addNumber(holder.getValue()));
	}
	
	/**
	 * Drop on ground or auto loot a reward item, depending about activated {@link Config}s.
	 * @param player : The {@link Player} who made the highest damage contribution.
	 * @param holder : The {@link IntIntHolder} used for reward (item id / amount).
	 */
	private void dropOrAutoLootHerb(Player player, IntIntHolder holder)
	{
		// Check Config.
		if (Config.AUTO_LOOT_HERBS)
			player.addItem("Loot", holder.getId(), 1, this, true);
		else
		{
			// If multiple similar herbs drop, split them and make a unique drop per item.
			final int count = holder.getValue();
			if (count > 1)
			{
				holder.setValue(1);
				for (int i = 0; i < count; i++)
					dropItem(holder, player);
			}
			else
				dropItem(holder, player);
		}
	}
	
	/**
	 * Drop a reward on the ground, to this {@link Monster} feet.
	 * @param holder : The {@link IntIntHolder} used for reward (item id / amount).
	 */
	public void dropItem(IntIntHolder holder)
	{
		dropItem(holder, null);
	}
	
	/**
	 * Drop a reward on the ground, to this {@link Monster} feet. It is item protected to the {@link Player} set as parameter.
	 * @param player : The {@link Player} used as item protection.
	 * @param holder : The {@link IntIntHolder} used for reward (item id / amount).
	 */
	public void dropItem(IntIntHolder holder, Player player)
	{
		for (int i = 0; i < holder.getValue(); i++)
		{
			// Create the ItemInstance and add it in the world as a visible object.
			final ItemInstance item = ItemInstance.create(holder.getId(), holder.getValue(), player, this);
			
			if (player != null)
				item.setDropProtection(player.getObjectId(), isRaidBoss());
			
			item.dropMe(this, 70);
			
			// If stackable, end loop as entire count is included in 1 instance of item.
			if (item.isStackable() || !Config.MULTIPLE_ITEM_DROP)
				break;
		}
	}
	
	/**
	 * Check CommandChannel loot priority every second. After 5min, the loot priority dissapears.
	 */
	private void checkCcLastAttack()
	{
		// We're still on time, do nothing.
		if (System.currentTimeMillis() - _lastCcAttack <= 300000)
			return;
		
		// Reset variables.
		_firstCcAttacker = null;
		_lastCcAttack = 0;
		
		// Set task to null.
		if (_ccTask != null)
		{
			_ccTask.cancel(false);
			_ccTask = null;
		}
		
		// Broadcast message.
		broadcastOnScreen(10000, BossInfoType.getBossInfo(getNpcId()).getCcNoRightsMsg());
	}
}
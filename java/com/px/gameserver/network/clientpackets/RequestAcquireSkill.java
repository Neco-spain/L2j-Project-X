package com.px.gameserver.network.clientpackets;

import com.px.Config;
import com.px.gameserver.data.SkillTable;
import com.px.gameserver.data.xml.SkillTreeData;
import com.px.gameserver.data.xml.SpellbookData;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.actor.instance.Fisherman;
import com.px.gameserver.model.actor.instance.Folk;
import com.px.gameserver.model.actor.instance.VillageMaster;
import com.px.gameserver.model.holder.skillnode.ClanSkillNode;
import com.px.gameserver.model.holder.skillnode.FishingSkillNode;
import com.px.gameserver.model.holder.skillnode.GeneralSkillNode;
import com.px.gameserver.network.SystemMessageId;
import com.px.gameserver.network.serverpackets.ExStorageMaxCount;
import com.px.gameserver.network.serverpackets.SystemMessage;
import com.px.gameserver.skills.L2Skill;

public class RequestAcquireSkill extends L2GameClientPacket
{
	private int _skillId;
	private int _skillLevel;
	private int _skillType;
	
	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLevel = readD();
		_skillType = readD();
	}
	
	@Override
	protected void runImpl()
	{
		// Not valid skill data, return.
		if (_skillId <= 0 || _skillLevel <= 0)
			return;
		
		// Incorrect player, return.
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		// Incorrect npc, return.
		final Folk folk = player.getCurrentFolk();
		if (folk == null || !player.getAI().canDoInteract(folk))
			return;
		
		// Skill doesn't exist, return.
		final L2Skill skill = SkillTable.getInstance().getInfo(_skillId, _skillLevel);
		if (skill == null)
			return;
		
		switch (_skillType)
		{
			// General skills.
			case 0:
				// Player already has such skill with same or higher level.
				int skillLvl = player.getSkillLevel(_skillId);
				if (skillLvl >= _skillLevel)
					return;
				
				// Requested skill must be 1 level higher than existing skill.
				if (skillLvl != _skillLevel - 1)
					return;
				
				// Search if the asked skill exists on player template.
				final GeneralSkillNode gsn = player.getTemplate().findSkill(_skillId, _skillLevel);
				if (gsn == null)
					return;
				
				// Not enought SP.
				if (player.getStatus().getSp() < gsn.getCorrectedCost())
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_SP_TO_LEARN_SKILL);
					folk.showSkillList(player);
					return;
				}
				
				// Get spellbook and try to consume it.
				final int bookId = SpellbookData.getInstance().getBookForSkill(_skillId, _skillLevel);
				if (bookId > 0 && !player.destroyItemByItemId("SkillLearn", bookId, 1, folk, true))
				{
					player.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
					folk.showSkillList(player);
					return;
				}
				
				// Consume SP.
				player.removeExpAndSp(0, gsn.getCorrectedCost());
				
				// Add skill new skill.
				player.addSkill(skill, true, true);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1).addSkillName(skill));
				
				// Update player and return.
				player.sendSkillList();
				folk.showSkillList(player);
				break;
			
			// Common skills.
			case 1:
				// Player already has such skill with same or higher level.
				skillLvl = player.getSkillLevel(_skillId);
				if (skillLvl >= _skillLevel)
					return;
				
				// Requested skill must be 1 level higher than existing skill.
				if (skillLvl != _skillLevel - 1)
					return;
				
				final FishingSkillNode fsn = SkillTreeData.getInstance().getFishingSkillFor(player, _skillId, _skillLevel);
				if (fsn == null)
					return;
				
				if (!player.destroyItemByItemId("Consume", fsn.getItemId(), fsn.getItemCount(), folk, true))
				{
					player.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
					Fisherman.showFishSkillList(player);
					return;
				}
				
				player.addSkill(skill, true, true);
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.LEARNED_SKILL_S1).addSkillName(skill));
				
				if (_skillId >= 1368 && _skillId <= 1372)
					player.sendPacket(new ExStorageMaxCount(player));
				
				player.sendSkillList();
				Fisherman.showFishSkillList(player);
				break;
			
			// Pledge skills.
			case 2:
				if (!player.isClanLeader())
					return;
				
				final ClanSkillNode csn = SkillTreeData.getInstance().getClanSkillFor(player, _skillId, _skillLevel);
				if (csn == null)
					return;
				
				if (player.getClan().getReputationScore() < csn.getCost())
				{
					player.sendPacket(SystemMessageId.ACQUIRE_SKILL_FAILED_BAD_CLAN_REP_SCORE);
					VillageMaster.showPledgeSkillList(player);
					return;
				}
				
				if (Config.LIFE_CRYSTAL_NEEDED && !player.destroyItemByItemId("Consume", csn.getItemId(), 1, folk, true))
				{
					player.sendPacket(SystemMessageId.ITEM_MISSING_TO_LEARN_SKILL);
					VillageMaster.showPledgeSkillList(player);
					return;
				}
				
				// Remove reputation score.
				final boolean needRefresh = player.getClan().takeReputationScore(csn.getCost());
				
				// Send message to Player.
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DEDUCTED_FROM_CLAN_REP).addNumber(csn.getCost()));
				
				// Reward Player's Clan with new skill. Keep track of the refresh.
				player.getClan().addClanSkill(skill, needRefresh);
				
				VillageMaster.showPledgeSkillList(player);
				return;
		}
	}
}
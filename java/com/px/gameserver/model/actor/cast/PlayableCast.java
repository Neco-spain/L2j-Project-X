package com.px.gameserver.model.actor.cast;

import com.px.gameserver.enums.skills.SkillType;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.model.actor.Playable;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.item.instance.ItemInstance;
import com.px.gameserver.network.SystemMessageId;
import com.px.gameserver.network.serverpackets.MagicSkillUse;
import com.px.gameserver.network.serverpackets.SystemMessage;
import com.px.gameserver.skills.L2Skill;

/**
 * This class groups all cast data related to a {@link Player}.
 * @param <T> : The {@link Playable} used as actor.
 */
public class PlayableCast<T extends Playable> extends CreatureCast<T>
{
	public PlayableCast(T actor)
	{
		super(actor);
	}
	
	@Override
	public void doInstantCast(L2Skill skill, ItemInstance item)
	{
		if (!item.isHerb() && !_actor.destroyItem("Consume", item.getObjectId(), (skill.getItemConsumeId() == 0 && skill.getItemConsume() > 0) ? skill.getItemConsume() : 1, null, false))
		{
			_actor.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return;
		}
		
		int reuseDelay = skill.getReuseDelay();
		if (reuseDelay > 10)
			_actor.disableSkill(skill, reuseDelay);
		
		_actor.broadcastPacket(new MagicSkillUse(_actor, _actor, skill.getId(), skill.getLevel(), 0, 0));
		
		callSkill(skill, new Creature[]
		{
			_actor
		}, item);
	}
	
	@Override
	public void doCast(L2Skill skill, Creature target, ItemInstance itemInstance)
	{
		if (itemInstance != null)
		{
			// Consume item if needed.
			if (!(itemInstance.isHerb() || itemInstance.isSummonItem()) && !_actor.destroyItem("Consume", itemInstance.getObjectId(), 1, null, false))
			{
				_actor.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
				return;
			}
			
			// Set item timestamp.
			_actor.addItemSkillTimeStamp(skill, itemInstance);
		}
		
		super.doCast(skill, target, itemInstance);
	}
	
	@Override
	public boolean canCast(Creature target, L2Skill skill, boolean isCtrlPressed, int itemObjectId)
	{
		if (!super.canCast(target, skill, isCtrlPressed, itemObjectId))
			return false;
		
		if (!skill.checkCondition(_actor, target, false))
			return false;
		
		final Player player = _actor.getActingPlayer();
		
		if (player.isInOlympiadMode() && (skill.isHeroSkill() || skill.getSkillType() == SkillType.RESURRECT))
		{
			player.sendPacket(SystemMessageId.THIS_SKILL_IS_NOT_AVAILABLE_FOR_THE_OLYMPIAD_EVENT);
			return false;
		}
		
		// Check item consumption validity.
		if (itemObjectId != 0 && player.getInventory().getItemByObjectId(itemObjectId) == null)
		{
			player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS);
			return false;
		}
		
		if (skill.getItemConsumeId() > 0)
		{
			final ItemInstance requiredItems = player.getInventory().getItemByItemId(skill.getItemConsumeId());
			if (requiredItems == null || requiredItems.getCount() < skill.getItemConsume())
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addSkillName(skill));
				return false;
			}
		}
		
		return skill.meetCastConditions(_actor, target, isCtrlPressed);
	}
	
	@Override
	public void stop()
	{
		super.stop();
		
		_actor.getAI().tryToIdle();
	}
}
package com.px.gameserver.handler.targethandlers;

import com.px.gameserver.enums.ZoneId;
import com.px.gameserver.enums.skills.SkillTargetType;
import com.px.gameserver.handler.ITargetHandler;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.model.actor.Playable;
import com.px.gameserver.model.actor.instance.Door;
import com.px.gameserver.model.actor.instance.Folk;
import com.px.gameserver.model.actor.instance.Guard;
import com.px.gameserver.model.actor.instance.Monster;
import com.px.gameserver.network.SystemMessageId;
import com.px.gameserver.skills.L2Skill;

public class TargetOne implements ITargetHandler
{
	@Override
	public SkillTargetType getTargetType()
	{
		return SkillTargetType.ONE;
	}
	
	@Override
	public Creature[] getTargetList(Creature caster, Creature target, L2Skill skill)
	{
		return new Creature[]
		{
			target
		};
	}
	
	@Override
	public Creature getFinalTarget(Creature caster, Creature target, L2Skill skill)
	{
		return target;
	}
	
	@Override
	public boolean meetCastConditions(Playable caster, Creature target, L2Skill skill, boolean isCtrlPressed)
	{
		if (target == null)
			return false;
		
		if (skill.isOffensive())
		{
			if (target.isDead() || target == caster)
			{
				caster.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
			
			if (target instanceof Playable)
			{
				final Playable playableTarget = (Playable) target;
				if (!caster.getActingPlayer().canCastOffensiveSkillOnPlayable(playableTarget, skill, isCtrlPressed))
				{
					caster.sendPacket(SystemMessageId.INVALID_TARGET);
					return false;
				}
				
				if (caster.getActingPlayer().isInOlympiadMode() && !caster.getActingPlayer().isOlympiadStart())
				{
					caster.sendPacket(SystemMessageId.INVALID_TARGET);
					return false;
				}
				
				if (caster.isInsideZone(ZoneId.PEACE))
				{
					caster.sendPacket(SystemMessageId.CANT_ATK_PEACEZONE);
					return false;
				}
				
				if (playableTarget.isInsideZone(ZoneId.PEACE))
				{
					caster.sendPacket(SystemMessageId.TARGET_IN_PEACEZONE);
					return false;
				}
			}
			else if (target instanceof Folk || target instanceof Guard)
			{
				// You can damage Folk and Guard with CTRL, but nothing else.
				if (!skill.isDamage() || !isCtrlPressed)
				{
					caster.sendPacket(SystemMessageId.INVALID_TARGET);
					return false;
				}
			}
			else if (target instanceof Door && !target.isAttackableBy(caster))
			{
				caster.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
		}
		else
		{
			if (target instanceof Playable)
			{
				if (!caster.getActingPlayer().canCastBeneficialSkillOnPlayable((Playable) target, skill, isCtrlPressed))
				{
					caster.sendPacket(SystemMessageId.INVALID_TARGET);
					return false;
				}
			}
			else if (target instanceof Monster && !isCtrlPressed)
			{
				caster.sendPacket(SystemMessageId.INVALID_TARGET);
				return false;
			}
		}
		return true;
	}
}
package com.px.gameserver.skills.effects;

import com.px.gameserver.enums.AiEventType;
import com.px.gameserver.enums.skills.EffectFlag;
import com.px.gameserver.enums.skills.EffectType;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.skills.AbstractEffect;
import com.px.gameserver.skills.L2Skill;

public class EffectImmobileUntilAttacked extends AbstractEffect
{
	public EffectImmobileUntilAttacked(EffectTemplate template, L2Skill skill, Creature effected, Creature effector)
	{
		super(template, skill, effected, effector);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.IMMOBILE_UNTIL_ATTACKED;
	}
	
	@Override
	public boolean onStart()
	{
		// Abort attack, cast and move.
		getEffected().abortAll(false);
		
		// Refresh abnormal effects.
		getEffected().updateAbnormalEffect();
		
		return true;
	}
	
	@Override
	public void onExit()
	{
		getEffected().removeEffect(this);
		getEffected().stopSkillEffects(getSkill().getId());
		
		getEffected().getAI().notifyEvent(AiEventType.THINK, null, null);
		
		// Refresh abnormal effects.
		getEffected().updateAbnormalEffect();
	}
	
	@Override
	public boolean onActionTime()
	{
		getEffected().removeEffect(this);
		getEffected().stopSkillEffects(getSkill().getId());
		
		getEffected().getAI().notifyEvent(AiEventType.THINK, null, null);
		
		// Refresh abnormal effects.
		getEffected().updateAbnormalEffect();
		return false;
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.MEDITATING.getMask();
	}
}
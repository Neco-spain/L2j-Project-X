package com.l2jpx.gameserver.skills.effects;

import com.l2jpx.gameserver.enums.skills.EffectFlag;
import com.l2jpx.gameserver.enums.skills.EffectType;
import com.l2jpx.gameserver.model.actor.Creature;
import com.l2jpx.gameserver.model.actor.Playable;
import com.l2jpx.gameserver.skills.AbstractEffect;
import com.l2jpx.gameserver.skills.L2Skill;

public class EffectCharmOfLuck extends AbstractEffect
{
	public EffectCharmOfLuck(EffectTemplate template, L2Skill skill, Creature effected, Creature effector)
	{
		super(template, skill, effected, effector);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.CHARM_OF_LUCK;
	}
	
	@Override
	public boolean onStart()
	{
		return true;
	}
	
	@Override
	public void onExit()
	{
		((Playable) getEffected()).stopCharmOfLuck(this);
	}
	
	@Override
	public boolean onActionTime()
	{
		return false;
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.CHARM_OF_LUCK.getMask();
	}
}
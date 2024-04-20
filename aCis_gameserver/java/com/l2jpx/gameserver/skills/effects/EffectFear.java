package com.l2jpx.gameserver.skills.effects;

import com.l2jpx.commons.util.ArraysUtil;

import com.l2jpx.gameserver.enums.skills.EffectFlag;
import com.l2jpx.gameserver.enums.skills.EffectType;
import com.l2jpx.gameserver.model.actor.Creature;
import com.l2jpx.gameserver.model.actor.Playable;
import com.l2jpx.gameserver.model.actor.instance.Folk;
import com.l2jpx.gameserver.model.actor.instance.SiegeFlag;
import com.l2jpx.gameserver.model.actor.instance.SiegeSummon;
import com.l2jpx.gameserver.skills.AbstractEffect;
import com.l2jpx.gameserver.skills.L2Skill;

public class EffectFear extends AbstractEffect
{
	private static final int[] REDUCED_DURATION_ON_PLAYABLE =
	{
		65, // Horror
		1092, // Fear
		1169 // Curse Fear
	};
	
	public static final int[] DOESNT_AFFECT_PLAYABLE =
	{
		98, // Sword Symphony
		1272, // Word of Fear
		1381 // Mass Fear
	};
	
	public EffectFear(EffectTemplate template, L2Skill skill, Creature effected, Creature effector)
	{
		super(template, skill, effected, effector);
		
		if (getEffected() instanceof Playable && ArraysUtil.contains(REDUCED_DURATION_ON_PLAYABLE, skill.getId()))
			setCount(getCount() / 2);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.FEAR;
	}
	
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof Folk || getEffected() instanceof SiegeFlag || getEffected() instanceof SiegeSummon)
			return false;
		
		if (getEffected().isAfraid())
			return false;
		
		// Abort attack, cast and move.
		getEffected().abortAll(false);
		
		// Refresh abnormal effects.
		getEffected().updateAbnormalEffect();
		
		onActionTime();
		
		return true;
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopEffects(EffectType.FEAR);
		
		// Refresh abnormal effects.
		getEffected().updateAbnormalEffect();
	}
	
	@Override
	public boolean onActionTime()
	{
		getEffected().fleeFrom(getEffector(), 500);
		return true;
	}
	
	@Override
	public boolean onSameEffect(AbstractEffect effect)
	{
		return false;
	}
	
	@Override
	public int getEffectFlags()
	{
		return EffectFlag.FEAR.getMask();
	}
}
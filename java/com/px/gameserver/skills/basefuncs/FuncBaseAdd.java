package com.px.gameserver.skills.basefuncs;

import com.px.gameserver.enums.skills.Stats;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.skills.L2Skill;
import com.px.gameserver.skills.conditions.Condition;

/**
 * @see Func
 */
public class FuncBaseAdd extends Func
{
	public FuncBaseAdd(Object owner, Stats stat, double value, Condition cond)
	{
		super(owner, stat, 2, value, cond);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, L2Skill skill, double base, double value)
	{
		// Condition does not exist or it fails, no change.
		if (getCond() != null && !getCond().test(effector, effected, skill))
			return value;
		
		// Update value.
		return value + getValue();
	}
}
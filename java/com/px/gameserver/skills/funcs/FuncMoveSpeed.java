package com.px.gameserver.skills.funcs;

import com.px.gameserver.enums.skills.Stats;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.skills.Formulas;
import com.px.gameserver.skills.L2Skill;
import com.px.gameserver.skills.basefuncs.Func;

/**
 * @see Func
 */
public class FuncMoveSpeed extends Func
{
	private static final FuncMoveSpeed INSTANCE = new FuncMoveSpeed();
	
	private FuncMoveSpeed()
	{
		super(null, Stats.RUN_SPEED, 10, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, L2Skill skill, double base, double value)
	{
		return value * Formulas.DEX_BONUS[effector.getStatus().getDEX()];
	}
	
	public static Func getInstance()
	{
		return INSTANCE;
	}
}
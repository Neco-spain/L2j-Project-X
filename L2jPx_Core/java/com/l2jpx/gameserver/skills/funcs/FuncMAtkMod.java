package com.l2jpx.gameserver.skills.funcs;

import com.l2jpx.gameserver.enums.skills.Stats;
import com.l2jpx.gameserver.model.actor.Creature;
import com.l2jpx.gameserver.skills.Formulas;
import com.l2jpx.gameserver.skills.L2Skill;
import com.l2jpx.gameserver.skills.basefuncs.Func;

/**
 * @see Func
 */
public class FuncMAtkMod extends Func
{
	private static final FuncMAtkMod INSTANCE = new FuncMAtkMod();
	
	private FuncMAtkMod()
	{
		super(null, Stats.MAGIC_ATTACK, 10, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, L2Skill skill, double base, double value)
	{
		final double intMod = Formulas.INT_BONUS[effector.getStatus().getINT()];
		final double lvlMod = effector.getStatus().getLevelMod();
		
		return value * ((lvlMod * lvlMod) * (intMod * intMod));
	}
	
	public static Func getInstance()
	{
		return INSTANCE;
	}
}
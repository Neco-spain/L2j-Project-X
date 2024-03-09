package net.l2jpx.gameserver.skills.funcs;

import net.l2jpx.gameserver.skills.Env;
import net.l2jpx.gameserver.skills.Stats;

public class FuncMul extends Func
{
	private final Lambda lambda;
	
	public FuncMul(final Stats pStat, final int pOrder, final Object owner, final Lambda lambda)
	{
		super(pStat, pOrder, owner);
		this.lambda = lambda;
	}
	
	@Override
	public void calc(final Env env)
	{
		if (cond == null || cond.test(env))
		{
			env.value *= lambda.calc(env);
		}
	}
}

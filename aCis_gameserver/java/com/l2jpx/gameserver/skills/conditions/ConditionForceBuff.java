package com.l2jpx.gameserver.skills.conditions;

import com.l2jpx.gameserver.model.actor.Creature;
import com.l2jpx.gameserver.model.item.kind.Item;
import com.l2jpx.gameserver.skills.AbstractEffect;
import com.l2jpx.gameserver.skills.L2Skill;
import com.l2jpx.gameserver.skills.effects.EffectFusion;

public class ConditionForceBuff extends Condition
{
	private static final short BATTLE_FORCE = 5104;
	private static final short SPELL_FORCE = 5105;
	
	private final byte[] _forces;
	
	public ConditionForceBuff(byte[] forces)
	{
		_forces = forces;
	}
	
	@Override
	public boolean testImpl(Creature effector, Creature effected, L2Skill skill, Item item)
	{
		if (_forces[0] > 0)
		{
			final AbstractEffect effect = effector.getFirstEffect(BATTLE_FORCE);
			if (effect == null || ((EffectFusion) effect)._effect < _forces[0])
				return false;
		}
		
		if (_forces[1] > 0)
		{
			final AbstractEffect effect = effector.getFirstEffect(SPELL_FORCE);
			if (effect == null || ((EffectFusion) effect)._effect < _forces[1])
				return false;
		}
		return true;
	}
}
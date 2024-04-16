package com.px.gameserver.scripting.script.ai.individual.Monster.WarriorBase.Warrior.PartyLeaderWarrior;

import com.px.commons.random.Rnd;

import com.px.gameserver.enums.IntentionType;
import com.px.gameserver.enums.actors.NpcSkillType;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.model.actor.Npc;
import com.px.gameserver.model.actor.Playable;
import com.px.gameserver.skills.L2Skill;

public class PartyLeaderPhysicalSpecialAggressive extends PartyLeaderPhysicalSpecial
{
	
	public PartyLeaderPhysicalSpecialAggressive()
	{
		super("ai/individual/Monster/WarriorBase/Warrior/PartyLeaderWarrior");
	}
	
	public PartyLeaderPhysicalSpecialAggressive(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		20983,
		20959,
		20758,
		20994,
		20950,
		20956,
		21075,
		20941,
		20939,
		20991,
		20398,
		20738,
		20745,
		20751,
		20755,
		20763,
		20953,
		20980
	};
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (!(creature instanceof Playable))
			return;
		
		if (npc.getAI().getLifeTime() > 7 && npc.getAI().getCurrentIntention().getType() != IntentionType.ATTACK && npc.isInMyTerritory())
		{
			if (npc.distance2D(creature) > 100 && npc._i_ai0 == 0 && Rnd.get(100) < 33)
			{
				final L2Skill physicalSpecial = getNpcSkillByType(npc, NpcSkillType.PHYSICAL_SPECIAL);
				npc.getAI().addCastDesire(creature, physicalSpecial, 1000000);
				
				npc._i_ai0 = 1;
			}
		}
		
		tryToAttack(npc, creature);
		
		super.onSeeCreature(npc, creature);
	}
}
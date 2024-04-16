package com.px.gameserver.scripting.script.ai.individual.Monster.WizardBase.Wizard.WizardDDMagic2.WizardCorpseNecro;

import com.px.commons.random.Rnd;

import com.px.gameserver.enums.actors.NpcSkillType;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.model.actor.Npc;
import com.px.gameserver.model.actor.Playable;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.skills.L2Skill;

public class WizardCorpseNecroSummon extends WizardCorpseNecro
{
	public WizardCorpseNecroSummon()
	{
		super("ai/individual/Monster/WizardBase/Wizard/WizardDDMagic2/WizardCorpseNecro");
	}
	
	public WizardCorpseNecroSummon(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21563,
		21583,
		21557,
		21572
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimerAtFixedRate("3456", npc, null, 5000, 5000);
		
		super.onCreated(npc);
	}
	
	@Override
	public void onSeeCreature(Npc npc, Creature creature)
	{
		if (creature instanceof Npc && creature.isDead() && Rnd.get(100) < 30 && npc.distance2D(creature) < 200)
		{
			final Creature mostHated = npc.getAI().getHateList().getMostHatedCreature();
			if (mostHated != null)
			{
				final L2Skill clearCorpse = getNpcSkillByType(npc, NpcSkillType.CLEAR_CORPSE);
				npc.getCast().doCast(clearCorpse, creature, null);
				
				createOnePrivateEx(npc, getNpcIntAIParam(npc, "SummonPrivate"), creature.getX(), creature.getY(), creature.getZ(), 0, 0, false, 1000, creature.getObjectId(), 0);
			}
			return;
		}
		
		if (!(creature instanceof Playable))
			return;
		
		super.onSeeCreature(npc, creature);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (attacker instanceof Playable)
		{
			double f0 = getHateRatio(npc, attacker);
			f0 = (((1.0 * damage) / (npc.getStatus().getLevel() + 7)) + ((f0 / 100) * ((1.0 * damage) / (npc.getStatus().getLevel() + 7))));
			
			npc.getAI().addAttackDesire(attacker, f0 * 100);
		}
		super.onAttacked(npc, attacker, damage, skill);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3456"))
		{
			npc.lookNeighbor();
		}
		return super.onTimer(name, npc, player);
	}
}
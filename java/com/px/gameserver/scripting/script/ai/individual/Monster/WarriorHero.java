package com.px.gameserver.scripting.script.ai.individual.Monster;

import com.px.commons.random.Rnd;

import com.px.gameserver.enums.actors.NpcSkillType;
import com.px.gameserver.model.World;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.model.actor.Npc;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.network.NpcStringId;
import com.px.gameserver.skills.L2Skill;

public class WarriorHero extends MonsterAI
{
	public WarriorHero()
	{
		super("ai/individual/Monster");
	}
	
	public WarriorHero(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		21260
	};
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("3001"))
		{
			npc.deleteMe();
		}
		else if (name.equalsIgnoreCase("3002"))
		{
			npc.broadcastNpcSay(NpcStringId.get(1000434 + Rnd.get(7)));
		}
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onCreated(Npc npc)
	{
		final Creature c0 = (Creature) World.getInstance().getObject(npc._param3);
		if (c0 != null)
		{
			final L2Skill heroSkill = getNpcSkillByType(npc, NpcSkillType.HERO_SKILL);
			npc.getAI().addCastDesire(c0, heroSkill, 1000000);
			
			npc.getAI().addAttackDesire(c0, 1000);
		}
		
		startQuestTimer("3001", npc, null, 15000);
		startQuestTimer("3002", npc, null, 8000);
		
		super.onCreated(npc);
	}
}
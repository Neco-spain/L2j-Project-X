package com.px.gameserver.scripting.script.ai.boss.frintezza;

import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.model.actor.Npc;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.scripting.script.ai.individual.DefaultNpc;
import com.px.gameserver.skills.L2Skill;

public class EvilateB extends DefaultNpc
{
	public EvilateB()
	{
		super("ai/boss/frintezza");
	}
	
	public EvilateB(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		29048
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		startQuestTimer("1000", npc, null, 45000);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		final int spawnPosX = getNpcIntAIParam(npc, "SpawnPosX");
		final int spawnPosY = getNpcIntAIParam(npc, "SpawnPosY");
		final int spawnPosZ = getNpcIntAIParam(npc, "SpawnPosZ");
		final int spawnAngle = getNpcIntAIParam(npc, "SpawnAngle");
		
		if (name.equalsIgnoreCase("1000"))
		{
			createOnePrivateEx(npc, 29051, spawnPosX, spawnPosY, spawnPosZ, spawnAngle, 0, true);
			startQuestTimer("1001", npc, null, 20000);
		}
		else if (name.equalsIgnoreCase("1001"))
		{
			createOnePrivateEx(npc, 29051, spawnPosX, spawnPosY, spawnPosZ, spawnAngle, 0, true);
		}
		
		return super.onTimer(name, npc, player);
	}
	
	@Override
	public void onAttacked(Npc npc, Creature attacker, int damage, L2Skill skill)
	{
		if (skill != null && skill.getId() == 2276)
			npc.doDie(npc);
	}
	
	@Override
	public void onPartyDied(Npc caller, Npc called)
	{
		if (called != caller && called.isMaster() && called.isDead())
			caller.scheduleRespawn(20000);
	}
}

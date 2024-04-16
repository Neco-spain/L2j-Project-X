package com.px.gameserver.scripting.script.ai.boss.frintezza;

import com.px.gameserver.data.SkillTable;
import com.px.gameserver.model.actor.Npc;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.scripting.script.ai.individual.DefaultNpc;

public class FollowerDummy extends DefaultNpc
{
	public FollowerDummy()
	{
		super("ai/boss/frintezza");
	}
	
	public FollowerDummy(String descr)
	{
		super(descr);
	}
	
	protected final int[] _npcIds =
	{
		29053
	};
	
	@Override
	public void onCreated(Npc npc)
	{
		npc.getAI().addCastDesire(npc, SkillTable.getInstance().getInfo(5004, 1), 1000000);
		startQuestTimer("1001", npc, null, 10200);
	}
	
	@Override
	public String onTimer(String name, Npc npc, Player player)
	{
		if (name.equalsIgnoreCase("1001"))
			npc.deleteMe();
		
		return super.onTimer(name, npc, player);
	}
}

package com.px.gameserver.scripting.script.maker;

import com.px.commons.random.Rnd;

import com.px.gameserver.data.manager.SpawnManager;
import com.px.gameserver.model.actor.Npc;
import com.px.gameserver.model.spawn.MultiSpawn;
import com.px.gameserver.model.spawn.NpcMaker;

public class InstantSpawnRandomMaker extends DefaultMaker
{
	public InstantSpawnRandomMaker(String name)
	{
		super(name);
	}
	
	@Override
	public void onNpcDeleted(Npc npc, MultiSpawn ms, NpcMaker maker)
	{
		if (maker.getNpcsAlive() == 0)
		{
			final int i1 = Rnd.get(maker.getMakerMemo().getInteger("maker_cnt"));
			if (i1 == 0)
			{
				final NpcMaker maker0 = SpawnManager.getInstance().getNpcMaker(maker.getMakerMemo().get("maker_name1"));
				if (maker0 != null)
					maker0.getMaker().onMakerScriptEvent("1001", maker0, (int) ms.calculateRespawnDelay(), 0);
			}
			else if (i1 == 1)
			{
				final NpcMaker maker0 = SpawnManager.getInstance().getNpcMaker(maker.getMakerMemo().get("maker_name2"));
				if (maker0 != null)
					maker0.getMaker().onMakerScriptEvent("1001", maker0, (int) ms.calculateRespawnDelay(), 0);
			}
			else if (i1 == 2)
			{
				final NpcMaker maker0 = SpawnManager.getInstance().getNpcMaker(maker.getMakerMemo().get("maker_name3"));
				if (maker0 != null)
					maker0.getMaker().onMakerScriptEvent("1001", maker0, (int) ms.calculateRespawnDelay(), 0);
			}
		}
	}
}
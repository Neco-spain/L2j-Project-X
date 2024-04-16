package com.px.gameserver.scripting.script.maker;

import com.px.gameserver.model.spawn.MultiSpawn;
import com.px.gameserver.model.spawn.NpcMaker;
import com.px.gameserver.model.spawn.SpawnData;

public class DefaultUseDBMakerForFrintezza extends DefaultUseDBMaker
{
	public DefaultUseDBMakerForFrintezza(String name)
	{
		super(name);
	}
	
	@Override
	public void onNpcDBInfo(MultiSpawn ms, SpawnData spawnData, NpcMaker maker)
	{
		ms.doSpawn(true);
	}
}

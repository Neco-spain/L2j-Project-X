package com.px.gameserver.network.clientpackets;

import com.px.gameserver.data.xml.ScriptData;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.scripting.Quest;
import com.px.gameserver.scripting.QuestState;

public final class RequestQuestAbort extends L2GameClientPacket
{
	private int _questId;
	
	@Override
	protected void readImpl()
	{
		_questId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final Quest quest = ScriptData.getInstance().getQuest(_questId);
		if (quest == null)
			return;
		
		final QuestState qs = player.getQuestList().getQuestState(_questId);
		if (qs != null)
			qs.exitQuest(true);
	}
}
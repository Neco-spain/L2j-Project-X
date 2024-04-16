package com.px.gameserver.network.clientpackets;

import com.px.gameserver.model.actor.Player;
import com.px.gameserver.scripting.QuestState;

public class RequestTutorialPassCmdToServer extends L2GameClientPacket
{
	private String _bypass;
	
	@Override
	protected void readImpl()
	{
		_bypass = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final QuestState qs = player.getQuestList().getQuestState("Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent(_bypass, null, player);
	}
}
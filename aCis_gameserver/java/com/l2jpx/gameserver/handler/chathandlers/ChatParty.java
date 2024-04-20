package com.l2jpx.gameserver.handler.chathandlers;

import com.l2jpx.gameserver.enums.SayType;
import com.l2jpx.gameserver.handler.IChatHandler;
import com.l2jpx.gameserver.model.actor.Player;
import com.l2jpx.gameserver.model.group.Party;
import com.l2jpx.gameserver.network.serverpackets.CreatureSay;

public class ChatParty implements IChatHandler
{
	private static final SayType[] COMMAND_IDS =
	{
		SayType.PARTY
	};
	
	@Override
	public void handleChat(SayType type, Player player, String target, String text)
	{
		final Party party = player.getParty();
		if (party == null)
			return;
		
		party.broadcastPacket(new CreatureSay(player, type, text));
	}
	
	@Override
	public SayType[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}
package com.l2jpx.gameserver.handler.chathandlers;

import com.l2jpx.gameserver.enums.SayType;
import com.l2jpx.gameserver.handler.IChatHandler;
import com.l2jpx.gameserver.model.actor.Player;
import com.l2jpx.gameserver.model.group.CommandChannel;
import com.l2jpx.gameserver.model.group.Party;
import com.l2jpx.gameserver.network.serverpackets.CreatureSay;

public class ChatPartyRoomCommander implements IChatHandler
{
	private static final SayType[] COMMAND_IDS =
	{
		SayType.PARTYROOM_COMMANDER
	};
	
	@Override
	public void handleChat(SayType type, Player player, String target, String text)
	{
		final Party party = player.getParty();
		if (party == null)
			return;
		
		final CommandChannel channel = party.getCommandChannel();
		if (channel == null || !channel.isLeader(player))
			return;
		
		channel.broadcastCreatureSay(new CreatureSay(player, type, text), player);
	}
	
	@Override
	public SayType[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}
package com.l2jpx.gameserver.network.clientpackets;

import com.l2jpx.gameserver.model.actor.Player;
import com.l2jpx.gameserver.network.serverpackets.HennaEquipList;

public final class RequestHennaItemList extends L2GameClientPacket
{
	@SuppressWarnings("unused")
	private int _unknown;
	
	@Override
	protected void readImpl()
	{
		_unknown = readD(); // ??
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		player.sendPacket(new HennaEquipList(player));
	}
}
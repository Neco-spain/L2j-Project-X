package com.px.gameserver.network.clientpackets;

import com.px.gameserver.model.actor.Player;
import com.px.gameserver.network.serverpackets.StopRotation;

public final class FinishRotating extends L2GameClientPacket
{
	private int _degree;
	
	@Override
	protected void readImpl()
	{
		_degree = readD();
		readD(); // Not used.
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		player.broadcastPacket(new StopRotation(player.getObjectId(), _degree, 0));
	}
}
package com.l2jpx.gameserver.network.clientpackets;

import com.l2jpx.gameserver.model.actor.Player;
import com.l2jpx.gameserver.model.pledge.Clan;
import com.l2jpx.gameserver.model.pledge.ClanMember;
import com.l2jpx.gameserver.network.serverpackets.PledgeReceivePowerInfo;

public final class RequestPledgeMemberPowerInfo extends L2GameClientPacket
{
	private String _player;
	
	@Override
	protected void readImpl()
	{
		readD(); // Not used for security reason. Pledge type.
		_player = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final Clan clan = player.getClan();
		if (clan == null)
			return;
		
		final ClanMember member = clan.getClanMember(_player);
		if (member == null)
			return;
		
		player.sendPacket(new PledgeReceivePowerInfo(member));
	}
}
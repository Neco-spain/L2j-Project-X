package com.l2jpx.loginserver.network.loginserverpackets;

import com.l2jpx.loginserver.data.manager.GameServerManager;
import com.l2jpx.loginserver.network.serverpackets.ServerBasePacket;

public class AuthResponse extends ServerBasePacket
{
	public AuthResponse(int serverId)
	{
		writeC(0x02);
		writeC(serverId);
		writeS(GameServerManager.getInstance().getServerNames().get(serverId));
	}
	
	@Override
	public byte[] getContent()
	{
		return getBytes();
	}
}
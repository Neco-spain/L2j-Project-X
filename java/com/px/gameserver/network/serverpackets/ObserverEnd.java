package com.px.gameserver.network.serverpackets;

import com.px.gameserver.model.location.Location;

public class ObserverEnd extends L2GameServerPacket
{
	private final Location _location;
	
	public ObserverEnd(Location loc)
	{
		_location = loc;
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xe0);
		
		writeLoc(_location);
	}
}
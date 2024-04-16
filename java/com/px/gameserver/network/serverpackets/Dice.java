package com.px.gameserver.network.serverpackets;

import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.location.SpawnLocation;

public class Dice extends L2GameServerPacket
{
	private final int _objectId;
	private final int _itemId;
	private final int _number;
	private final SpawnLocation _loc;
	
	public Dice(Player player, int itemId, int number)
	{
		_objectId = player.getObjectId();
		_itemId = itemId;
		_number = number;
		
		_loc = player.getPosition().clone();
		_loc.addOffsetBasedOnHeading(30);
	}
	
	@Override
	protected final void writeImpl()
	{
		writeC(0xD4);
		writeD(_objectId);
		writeD(_itemId);
		writeD(_number);
		writeLoc(_loc);
	}
}
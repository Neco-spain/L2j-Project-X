package com.px.gameserver.network.serverpackets;

import java.util.Collection;

import com.px.gameserver.model.actor.Playable;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.item.instance.ItemInfo;
import com.px.gameserver.model.item.kind.Item;

public class AbstractInventoryUpdate extends L2GameServerPacket
{
	private final Collection<ItemInfo> _items;
	private final boolean _isPlayer;
	
	public AbstractInventoryUpdate(Playable playable)
	{
		_items = playable.getInventory().getUpdateList();
		_isPlayer = playable instanceof Player;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(_isPlayer ? 0x27 : 0xb3);
		writeH(_items.size());
		
		for (ItemInfo temp : _items)
		{
			final Item item = temp.getItem();
			
			writeH(temp.getState().ordinal());
			writeH(item.getType1());
			writeD(temp.getObjectId());
			writeD(item.getItemId());
			writeD(temp.getCount());
			writeH(item.getType2());
			writeH(temp.getCustomType1());
			writeH(temp.getEquipped());
			writeD(item.getBodyPart());
			writeH(temp.getEnchant());
			writeH(temp.getCustomType2());
			
			if (_isPlayer)
			{
				writeD(temp.getAugmentation());
				writeD(temp.getDisplayedManaLeft());
			}
		}
		
		_items.clear();
	}
}
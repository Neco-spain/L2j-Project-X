package com.l2jpx.gameserver.handler.itemhandlers;

import com.l2jpx.gameserver.handler.IItemHandler;
import com.l2jpx.gameserver.model.actor.Playable;
import com.l2jpx.gameserver.model.actor.Player;
import com.l2jpx.gameserver.model.item.instance.ItemInstance;
import com.l2jpx.gameserver.network.serverpackets.ActionFailed;
import com.l2jpx.gameserver.network.serverpackets.NpcHtmlMessage;

public class Books implements IItemHandler
{
	@Override
	public void useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!(playable instanceof Player))
			return;
		
		final Player player = (Player) playable;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile("data/html/help/" + item.getItemId() + ".htm");
		html.setItemId(item.getItemId());
		player.sendPacket(html);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
}
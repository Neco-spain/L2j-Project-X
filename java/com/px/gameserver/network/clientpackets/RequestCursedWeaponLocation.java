package com.px.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;

import com.px.gameserver.data.manager.CursedWeaponManager;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.entity.CursedWeapon;
import com.px.gameserver.model.location.Location;
import com.px.gameserver.network.serverpackets.ExCursedWeaponLocation;
import com.px.gameserver.network.serverpackets.ExCursedWeaponLocation.CursedWeaponInfo;

public final class RequestCursedWeaponLocation extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final List<CursedWeaponInfo> list = new ArrayList<>();
		for (CursedWeapon cw : CursedWeaponManager.getInstance().getCursedWeapons())
		{
			if (!cw.isActive())
				continue;
			
			final Location loc = cw.getWorldPosition();
			if (loc != null)
				list.add(new CursedWeaponInfo(loc, cw.getItemId(), (cw.isActivated()) ? 1 : 0));
		}
		
		if (!list.isEmpty())
			player.sendPacket(new ExCursedWeaponLocation(list));
	}
}
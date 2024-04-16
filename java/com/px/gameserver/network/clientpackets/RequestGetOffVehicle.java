package com.px.gameserver.network.clientpackets;

import com.px.gameserver.model.actor.Boat;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.actor.container.player.BoatInfo;
import com.px.gameserver.model.location.Point2D;
import com.px.gameserver.model.location.SpawnLocation;
import com.px.gameserver.network.serverpackets.ActionFailed;
import com.px.gameserver.network.serverpackets.GetOffVehicle;

public final class RequestGetOffVehicle extends L2GameClientPacket
{
	private int _boatId;
	private int _x;
	private int _y;
	private int _z;
	
	@Override
	protected void readImpl()
	{
		_boatId = readD();
		_x = readD();
		_y = readD();
		_z = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final BoatInfo info = player.getBoatInfo();
		
		if (!info.isInBoat() || info.getBoat().getObjectId() != _boatId)
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Boat boat = info.getBoat();
		
		final Point2D point = boat.getDock().getAdjustedBoardingPoint(player.getPosition(), new Point2D(_x, _y), true);
		
		final SpawnLocation destination = new SpawnLocation(point.getX(), point.getY(), -3624, player.getHeading());
		
		boat.removePassenger(player);
		info.stopMoveInVehicle(_boatId);
		info.setBoat(null);
		info.getBoatPosition().clean();
		info.setBoatMovement(false);
		
		player.broadcastPacket(new GetOffVehicle(player.getObjectId(), _boatId, _x, _y, _z));
		player.setXYZ(destination);
		player.revalidateZone(true);
		player.getAI().tryToMoveTo(destination, null);
	}
}
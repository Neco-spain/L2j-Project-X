package com.px.gameserver.model.zone.type.subtype;

import com.px.gameserver.data.manager.CastleManager;
import com.px.gameserver.model.World;
import com.px.gameserver.model.WorldObject;
import com.px.gameserver.model.WorldRegion;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.residence.castle.Castle;
import com.px.gameserver.network.serverpackets.EventTrigger;

/**
 * A zone type extending {@link ZoneType} used for castle zones.
 */
public abstract class CastleZoneType extends ZoneType
{
	private int _castleId;
	private Castle _castle;
	
	private boolean _enabled;
	private int _eventId;
	
	protected CastleZoneType(int id)
	{
		super(id);
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("castleId"))
			_castleId = Integer.parseInt(value);
		else if (name.equals("eventId"))
			_eventId = Integer.parseInt(value);
		else
			super.setParameter(name, value);
	}
	
	@Override
	public void addKnownObject(WorldObject object)
	{
		if (_eventId > 0 && _enabled && object instanceof Player)
			((Player) object).sendPacket(new EventTrigger(getEventId(), true));
	}
	
	@Override
	public void removeKnownObject(WorldObject object)
	{
		if (_eventId > 0 && object instanceof Player)
			((Player) object).sendPacket(new EventTrigger(getEventId(), false));
	}
	
	public Castle getCastle()
	{
		if (_castleId > 0 && _castle == null)
			_castle = CastleManager.getInstance().getCastleById(_castleId);
		
		return _castle;
	}
	
	public int getEventId()
	{
		return _eventId;
	}
	
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	public void setEnabled(boolean val)
	{
		_enabled = val;
		
		// Broadcast effect for all regions surrounding this zone if an eventId is found.
		if (_eventId > 0)
		{
			final WorldRegion region = World.getInstance().getRegion(this);
			for (WorldRegion reg : region.getSurroundingRegions())
			{
				for (WorldObject obj : reg.getObjects())
					if (obj instanceof Player)
						((Player) obj).sendPacket(new EventTrigger(_eventId, val));
			}
		}
	}
}
package com.px.gameserver.network.serverpackets;

import java.util.List;

import com.px.gameserver.data.manager.CastleManorManager;
import com.px.gameserver.model.manor.Seed;

public class ExShowManorDefaultInfo extends L2GameServerPacket
{
	private final List<Seed> _crops;
	private final boolean _hideButtons;
	
	public ExShowManorDefaultInfo(boolean hideButtons)
	{
		_crops = CastleManorManager.getInstance().getCrops();
		_hideButtons = hideButtons;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x1E);
		
		writeC(_hideButtons ? 0x01 : 0x00);
		writeD(_crops.size());
		
		for (Seed crop : _crops)
		{
			writeD(crop.getCropId());
			writeD(crop.getLevel());
			writeD(crop.getSeedReferencePrice());
			writeD(crop.getCropReferencePrice());
			writeC(1);
			writeD(crop.getReward1());
			writeC(1);
			writeD(crop.getReward2());
		}
	}
}
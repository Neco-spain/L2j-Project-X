package com.px.gameserver.network.clientpackets;

import java.util.ArrayList;
import java.util.List;

import com.px.Config;
import com.px.gameserver.data.manager.CastleManorManager;
import com.px.gameserver.data.xml.ItemData;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.actor.instance.Folk;
import com.px.gameserver.model.actor.instance.ManorManagerNpc;
import com.px.gameserver.model.holder.IntIntHolder;
import com.px.gameserver.model.item.instance.ItemInstance;
import com.px.gameserver.model.item.kind.Item;
import com.px.gameserver.model.manor.CropProcure;
import com.px.gameserver.network.SystemMessageId;
import com.px.gameserver.network.serverpackets.ActionFailed;
import com.px.gameserver.network.serverpackets.SystemMessage;

public class RequestProcureCropList extends L2GameClientPacket
{
	private static final int BATCH_LENGTH = 16;
	
	private List<CropHolder> _items;
	
	@Override
	protected void readImpl()
	{
		final int count = readD();
		if (count <= 0 || count > Config.MAX_ITEM_IN_PACKET || count * BATCH_LENGTH != _buf.remaining())
			return;
		
		_items = new ArrayList<>(count);
		for (int i = 0; i < count; i++)
		{
			final int objId = readD();
			final int itemId = readD();
			final int manorId = readD();
			final int cnt = readD();
			
			if (objId < 1 || itemId < 1 || manorId < 0 || cnt < 0)
			{
				_items = null;
				return;
			}
			
			_items.add(new CropHolder(objId, itemId, cnt, manorId));
		}
	}
	
	@Override
	protected void runImpl()
	{
		if (_items == null)
			return;
		
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final CastleManorManager manor = CastleManorManager.getInstance();
		if (manor.isUnderMaintenance())
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Folk folk = player.getCurrentFolk();
		if (!(folk instanceof ManorManagerNpc) || !player.getAI().canDoInteract(folk))
		{
			sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final int castleId = folk.getCastle().getCastleId();
		
		// Calculate summary values
		int slots = 0;
		int weight = 0;
		
		for (CropHolder i : _items)
		{
			final ItemInstance item = player.getInventory().getItemByObjectId(i.getObjectId());
			if (item == null || item.getCount() < i.getValue() || item.getItemId() != i.getId())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			final CropProcure cp = i.getCropProcure();
			if (cp == null || cp.getAmount() < i.getValue())
			{
				sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
			
			final Item template = ItemData.getInstance().getTemplate(i.getRewardId());
			weight += (i.getValue() * template.getWeight());
			
			if (!template.isStackable())
				slots += i.getValue();
			else if (player.getInventory().getItemByItemId(i.getRewardId()) == null)
				slots++;
		}
		
		if (!player.getInventory().validateWeight(weight))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.WEIGHT_LIMIT_EXCEEDED));
			return;
		}
		
		if (!player.getInventory().validateCapacity(slots))
		{
			sendPacket(SystemMessage.getSystemMessage(SystemMessageId.SLOTS_FULL));
			return;
		}
		
		// Proceed the purchase
		for (CropHolder i : _items)
		{
			final int rewardPrice = ItemData.getInstance().getTemplate(i.getRewardId()).getReferencePrice();
			if (rewardPrice == 0)
				continue;
			
			final int rewardItemCount = i.getPrice() / rewardPrice;
			if (rewardItemCount < 1)
			{
				player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1).addItemName(i.getId()).addItemNumber(i.getValue()));
				continue;
			}
			
			// Fee for selling to other manors
			final int fee = (castleId == i.getManorId()) ? 0 : ((int) (i.getPrice() * 0.05));
			if (fee != 0 && player.getAdena() < fee)
			{
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.FAILED_IN_TRADING_S2_OF_CROP_S1).addItemName(i.getId()).addItemNumber(i.getValue()));
				sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_NOT_ENOUGH_ADENA));
				continue;
			}
			
			final CropProcure cp = i.getCropProcure();
			if (!cp.decreaseAmount(i.getValue()) || (fee > 0 && !player.reduceAdena("Manor", fee, folk, true)) || !player.destroyItem("Manor", i.getObjectId(), i.getValue(), folk, true))
				continue;
			
			player.addItem("Manor", i.getRewardId(), rewardItemCount, folk, true);
		}
	}
	
	private final class CropHolder extends IntIntHolder
	{
		private final int _manorId;
		private final int _objectId;
		
		private CropProcure _cp;
		private int _rewardId = 0;
		
		public CropHolder(int objectId, int id, int count, int manorId)
		{
			super(id, count);
			
			_objectId = objectId;
			_manorId = manorId;
		}
		
		public final int getManorId()
		{
			return _manorId;
		}
		
		public int getObjectId()
		{
			return _objectId;
		}
		
		public final int getPrice()
		{
			return getValue() * _cp.getPrice();
		}
		
		public final CropProcure getCropProcure()
		{
			if (_cp == null)
				_cp = CastleManorManager.getInstance().getCropProcure(_manorId, getId(), false);
			
			return _cp;
		}
		
		public final int getRewardId()
		{
			if (_rewardId == 0)
				_rewardId = CastleManorManager.getInstance().getSeedRewardByCrop(_cp.getId(), _cp.getReward());
			
			return _rewardId;
		}
	}
}
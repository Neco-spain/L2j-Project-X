package com.px.gameserver.model.itemcontainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.px.gameserver.data.manager.HeroManager;
import com.px.gameserver.data.xml.ItemData;
import com.px.gameserver.enums.Paperdoll;
import com.px.gameserver.enums.ShortcutType;
import com.px.gameserver.enums.StatusType;
import com.px.gameserver.enums.items.EtcItemType;
import com.px.gameserver.enums.items.ItemLocation;
import com.px.gameserver.model.WorldObject;
import com.px.gameserver.model.actor.Playable;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.holder.IntIntHolder;
import com.px.gameserver.model.item.instance.ItemInstance;
import com.px.gameserver.model.item.kind.Item;
import com.px.gameserver.model.itemcontainer.listeners.ArmorSetListener;
import com.px.gameserver.model.itemcontainer.listeners.BowRodListener;
import com.px.gameserver.model.itemcontainer.listeners.ItemPassiveSkillsListener;
import com.px.gameserver.model.itemcontainer.listeners.OnEquipListener;
import com.px.gameserver.model.trade.BuyProcessItem;
import com.px.gameserver.model.trade.SellProcessItem;
import com.px.gameserver.model.trade.TradeItem;
import com.px.gameserver.model.trade.TradeList;
import com.px.gameserver.network.serverpackets.StatusUpdate;
import com.px.gameserver.taskmanager.ShadowItemTaskManager;

public class PcInventory extends Inventory
{
	public static final int ADENA_ID = 57;
	public static final int ANCIENT_ADENA_ID = 5575;
	
	private ItemInstance _adena;
	private ItemInstance _ancientAdena;
	
	public PcInventory(Player owner)
	{
		super(owner);
		
		addPaperdollListener(ArmorSetListener.getInstance());
		addPaperdollListener(BowRodListener.getInstance());
		addPaperdollListener(ItemPassiveSkillsListener.getInstance());
		addPaperdollListener(ShadowItemTaskManager.getInstance());
	}
	
	@Override
	public Player getOwner()
	{
		return (Player) _owner;
	}
	
	@Override
	protected ItemLocation getBaseLocation()
	{
		return ItemLocation.INVENTORY;
	}
	
	@Override
	protected ItemLocation getEquipLocation()
	{
		return ItemLocation.PAPERDOLL;
	}
	
	@Override
	public void equipItem(ItemInstance item)
	{
		// Can't equip item if you are in shop mod or hero item and you're not hero.
		if (getOwner().isOperating() || (item.isHeroItem() && !HeroManager.getInstance().isActiveHero(getOwnerId())))
			return;
		
		// Check if player wears formal wear.
		if (getOwner().isWearingFormalWear())
		{
			switch (item.getItem().getBodyPart())
			{
				case Item.SLOT_LR_HAND:
				case Item.SLOT_L_HAND:
				case Item.SLOT_R_HAND:
					unequipItemInBodySlotAndRecord(Item.SLOT_ALLDRESS);
					break;
				
				case Item.SLOT_LEGS:
				case Item.SLOT_FEET:
				case Item.SLOT_GLOVES:
				case Item.SLOT_HEAD:
					return;
			}
		}
		
		super.equipItem(item);
	}
	
	@Override
	public void equipPetItem(ItemInstance item)
	{
		// Can't equip item if you are in shop mod.
		if (getOwner().isOperating())
			return;
		
		super.equipPetItem(item);
	}
	
	@Override
	public boolean updateWeight()
	{
		if (!super.updateWeight())
			return false;
		
		// Send StatusUpdate packet.
		final StatusUpdate su = new StatusUpdate(getOwner());
		su.addAttribute(StatusType.CUR_LOAD, _totalWeight);
		getOwner().sendPacket(su);
		
		// Test weight penalty.
		getOwner().refreshWeightPenalty();
		return true;
	}
	
	public ItemInstance getAdenaInstance()
	{
		return _adena;
	}
	
	@Override
	public int getAdena()
	{
		return _adena != null ? _adena.getCount() : 0;
	}
	
	public ItemInstance getAncientAdenaInstance()
	{
		return _ancientAdena;
	}
	
	public int getAncientAdena()
	{
		return (_ancientAdena != null) ? _ancientAdena.getCount() : 0;
	}
	
	public List<ItemInstance> getUniqueItems(boolean checkEnchantAndAugment, boolean allowAdena, boolean allowAncientAdena, boolean allowStoreBuy, boolean allowNewbieWeapons)
	{
		final List<ItemInstance> list = new ArrayList<>();
		
		for (ItemInstance item : _items)
		{
			if (!allowAdena && item.getItemId() == ADENA_ID)
				continue;
			
			if (!allowAncientAdena && item.getItemId() == ANCIENT_ADENA_ID)
				continue;
			
			if (!item.isStackable() && list.stream().anyMatch(i -> i.getItemId() == item.getItemId() && (!checkEnchantAndAugment || (i.getEnchantLevel() == item.getEnchantLevel() && Objects.equals(i.getAugmentation(), item.getAugmentation())))))
				continue;
			
			if (item.isAvailable(getOwner(), allowAdena, (checkEnchantAndAugment && item.isAugmented()) || (allowNewbieWeapons && item.isWeapon() && (item.getWeaponItem().isApprenticeWeapon() || item.getWeaponItem().isTravelerWeapon())) || item.isSellable(), allowStoreBuy))
				list.add(item);
		}
		
		return list;
	}
	
	/**
	 * @param itemId
	 * @return
	 * @see com.px.gameserver.model.itemcontainer.PcInventory#getAllItemsByItemId(int, boolean)
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId)
	{
		return getAllItemsByItemId(itemId, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id.
	 * @param itemId : ID of item
	 * @param includeEquipped : include equipped items
	 * @return ItemInstance[] : matching items from inventory
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId, boolean includeEquipped)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item == null)
				continue;
			
			if (item.getItemId() == itemId && (includeEquipped || !item.isEquipped()))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * @param itemId
	 * @param enchantment
	 * @return
	 * @see com.px.gameserver.model.itemcontainer.PcInventory#getAllItemsByItemId(int, int, boolean)
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId, int enchantment)
	{
		return getAllItemsByItemId(itemId, enchantment, true);
	}
	
	/**
	 * Returns the list of all items in inventory that have a given item id AND a given enchantment level.
	 * @param itemId : ID of item
	 * @param enchantment : enchant level of item
	 * @param includeEquipped : include equipped items
	 * @return ItemInstance[] : matching items from inventory
	 */
	public ItemInstance[] getAllItemsByItemId(int itemId, int enchantment, boolean includeEquipped)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item == null)
				continue;
			
			if ((item.getItemId() == itemId) && (item.getEnchantLevel() == enchantment) && (includeEquipped || !item.isEquipped()))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction
	 * @param allowAdena
	 * @param allowNonTradeable
	 * @param allowStoreBuy
	 * @return ItemInstance : items in inventory
	 */
	public ItemInstance[] getAvailableItems(boolean allowAdena, boolean allowNonTradeable, boolean allowStoreBuy)
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item != null && item.isAvailable(getOwner(), allowAdena, allowNonTradeable, allowStoreBuy))
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * @return a List of all sellable items.
	 */
	public List<ItemInstance> getSellableItems()
	{
		return _items.stream().filter(i -> !i.isEquipped() && i.isSellable() && (getOwner().getSummon() == null || i.getObjectId() != getOwner().getSummon().getControlItemId())).collect(Collectors.toList());
	}
	
	/**
	 * Get all augmented items
	 * @return
	 */
	public ItemInstance[] getAugmentedItems()
	{
		List<ItemInstance> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item != null && item.isAugmented())
				list.add(item);
		}
		return list.toArray(new ItemInstance[list.size()]);
	}
	
	/**
	 * Returns the list of items in inventory available for transaction adjusetd by tradeList
	 * @param tradeList
	 * @param allowStoreBuy
	 * @return ItemInstance : items in inventory
	 */
	public TradeItem[] getAvailableItems(TradeList tradeList, boolean allowStoreBuy)
	{
		List<TradeItem> list = new ArrayList<>();
		for (ItemInstance item : _items)
		{
			if (item != null && item.isAvailable(getOwner(), false, false, allowStoreBuy))
			{
				TradeItem adjItem = tradeList.adjustAvailableItem(item);
				if (adjItem != null)
					list.add(adjItem);
			}
		}
		return list.toArray(new TradeItem[list.size()]);
	}
	
	/**
	 * Adjust TradeItem according his status in inventory
	 * @param item : ItemInstance to be adjusten
	 */
	public void adjustAvailableItem(TradeItem item)
	{
		// For all ItemInstance with same item id.
		for (ItemInstance adjItem : getItemsByItemId(item.getItem().getItemId()))
		{
			// If enchant level is different, bypass.
			if (adjItem.getEnchantLevel() != item.getEnchant())
				continue;
			
			// If item isn't equipable, or equipable but not equiped it is a success.
			if (!adjItem.isEquipable() || (adjItem.isEquipable() && !adjItem.isEquipped()))
			{
				item.setObjectId(adjItem.getObjectId());
				item.setEnchant(adjItem.getEnchantLevel());
				item.setCount(Math.min(adjItem.getCount(), item.getQuantity()));
				return;
			}
		}
		// None item matched conditions ; return as invalid count.
		item.setCount(0);
	}
	
	/**
	 * Adds adena to PCInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : Player Player requesting the item add
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAdena(String process, int count, Player actor, WorldObject reference)
	{
		if (count > 0)
			addItem(process, ADENA_ID, count, actor, reference);
	}
	
	/**
	 * Removes adena to PCInventory
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : Player Player requesting the item add
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return true if successful.
	 */
	public boolean reduceAdena(String process, int count, Player actor, WorldObject reference)
	{
		if (count > 0)
			return destroyItemByItemId(process, ADENA_ID, count, actor, reference) != null;
		
		return false;
	}
	
	/**
	 * Adds specified amount of ancient adena to player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be added
	 * @param actor : Player Player requesting the item add
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void addAncientAdena(String process, int count, Player actor, WorldObject reference)
	{
		if (count > 0)
			addItem(process, ANCIENT_ADENA_ID, count, actor, reference);
	}
	
	/**
	 * Removes specified amount of ancient adena from player inventory.
	 * @param process : String Identifier of process triggering this action
	 * @param count : int Quantity of adena to be removed
	 * @param actor : Player Player requesting the item add
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return true if successful.
	 */
	public boolean reduceAncientAdena(String process, int count, Player actor, WorldObject reference)
	{
		if (count > 0)
			return destroyItemByItemId(process, ANCIENT_ADENA_ID, count, actor, reference) != null;
		
		return false;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be added
	 * @param actor : Player Player requesting the item add
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public ItemInstance addItem(String process, ItemInstance item, Playable actor, WorldObject reference)
	{
		item = super.addItem(process, item, actor, reference);
		if (item == null)
			return null;
		
		if (item.getItemId() == ADENA_ID && !item.equals(_adena))
			_adena = item;
		else if (item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
			_ancientAdena = item;
		
		return item;
	}
	
	/**
	 * Adds item in inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item Identifier of the item to be added
	 * @param count : int Quantity of items to be added
	 * @param actor : Player Player requesting the item creation
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public ItemInstance addItem(String process, int itemId, int count, Playable actor, WorldObject reference)
	{
		ItemInstance item = super.addItem(process, itemId, count, actor, reference);
		if (item == null)
			return null;
		
		if (item.getItemId() == ADENA_ID && !item.equals(_adena))
			_adena = item;
		else if (item.getItemId() == ANCIENT_ADENA_ID && !item.equals(_ancientAdena))
			_ancientAdena = item;
		
		return item;
	}
	
	/**
	 * Transfers item to another inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Identifier of the item to be transfered
	 * @param count : int Quantity of items to be transfered
	 * @param target : The {@link ItemContainer} to transfer the item.
	 * @param actor : Player Player requesting the item transfer
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the new item or the updated item in inventory
	 */
	@Override
	public ItemInstance transferItem(String process, int objectId, int count, ItemContainer target, Player actor, WorldObject reference)
	{
		ItemInstance item = super.transferItem(process, objectId, count, target, actor, reference);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		
		return item;
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be destroyed
	 * @param actor : Player Player requesting the item destroy
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItem(String process, ItemInstance item, Player actor, WorldObject reference)
	{
		return destroyItem(process, item, item.getCount(), actor, reference);
	}
	
	/**
	 * Destroy item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be destroyed
	 * @param actor : Player Player requesting the item destroy
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItem(String process, ItemInstance item, int count, Player actor, WorldObject reference)
	{
		item = super.destroyItem(process, item, count, actor, reference);
		
		if (_adena != null && _adena.getCount() <= 0)
			_adena = null;
		
		if (_ancientAdena != null && _ancientAdena.getCount() <= 0)
			_ancientAdena = null;
		
		return item;
	}
	
	/**
	 * Destroys item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : Player Player requesting the item destroy
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItem(String process, int objectId, int count, Player actor, WorldObject reference)
	{
		ItemInstance item = getItemByObjectId(objectId);
		if (item == null)
			return null;
		
		return destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Destroy item from inventory by using its <B>itemId</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param itemId : int Item identifier of the item to be destroyed
	 * @param count : int Quantity of items to be destroyed
	 * @param actor : Player Player requesting the item destroy
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance destroyItemByItemId(String process, int itemId, int count, Player actor, WorldObject reference)
	{
		ItemInstance item = getItemByItemId(itemId);
		if (item == null)
			return null;
		
		return destroyItem(process, item, count, actor, reference);
	}
	
	/**
	 * Drop item from inventory and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param item : ItemInstance to be dropped
	 * @param actor : Player Player requesting the item drop
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance dropItem(String process, ItemInstance item, Player actor, WorldObject reference)
	{
		item = super.dropItem(process, item, actor, reference);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		
		return item;
	}
	
	/**
	 * Drop item from inventory by using its <B>objectID</B> and checks _adena and _ancientAdena
	 * @param process : String Identifier of process triggering this action
	 * @param objectId : int Item Instance identifier of the item to be dropped
	 * @param count : int Quantity of items to be dropped
	 * @param actor : Player Player requesting the item drop
	 * @param reference : WorldObject Object referencing current action like NPC selling item or previous item in transformation
	 * @return ItemInstance corresponding to the destroyed item or the updated item in inventory
	 */
	@Override
	public ItemInstance dropItem(String process, int objectId, int count, Player actor, WorldObject reference)
	{
		ItemInstance item = super.dropItem(process, objectId, count, actor, reference);
		
		if (_adena != null && (_adena.getCount() <= 0 || _adena.getOwnerId() != getOwnerId()))
			_adena = null;
		
		if (_ancientAdena != null && (_ancientAdena.getCount() <= 0 || _ancientAdena.getOwnerId() != getOwnerId()))
			_ancientAdena = null;
		
		return item;
	}
	
	/**
	 * Delete all existing shortcuts refering to this {@link ItemInstance}, aswell as active enchants.
	 */
	@Override
	protected boolean removeItem(ItemInstance item)
	{
		// Delete all existing shortcuts refering to this object id.
		getOwner().getShortcutList().deleteShortcuts(item.getObjectId(), ShortcutType.ITEM);
		
		// Removes active Enchant Scroll
		if (item.equals(getOwner().getActiveEnchantItem()))
			getOwner().setActiveEnchantItem(null);
		
		if (item.getItemId() == ADENA_ID)
			_adena = null;
		else if (item.getItemId() == ANCIENT_ADENA_ID)
			_ancientAdena = null;
		
		return super.removeItem(item);
	}
	
	@Override
	public void restore()
	{
		super.restore();
		
		_adena = getItemByItemId(ADENA_ID);
		_ancientAdena = getItemByItemId(ANCIENT_ADENA_ID);
	}
	
	@Override
	public ItemInstance unequipItemInBodySlot(int slot)
	{
		final ItemInstance old = super.unequipItemInBodySlot(slot);
		if (old != null)
			getOwner().refreshExpertisePenalty();
		
		return old;
	}
	
	public boolean validateCapacity(ItemInstance item)
	{
		int slots = 0;
		if (!(item.isStackable() && getItemByItemId(item.getItemId()) != null) && item.getItemType() != EtcItemType.HERB)
			slots++;
		
		return validateCapacity(slots);
	}
	
	public boolean validateCapacityByItemId(IntIntHolder holder)
	{
		return validateCapacityByItemId(holder.getId(), holder.getValue());
	}
	
	public boolean validateCapacityByItemId(int itemId, int itemCount)
	{
		return validateCapacity(calculateUsedSlots(itemId, itemCount));
	}
	
	public boolean validateCapacityByItemIds(List<IntIntHolder> holders)
	{
		int slots = 0;
		for (IntIntHolder holder : holders)
			slots += calculateUsedSlots(holder.getId(), holder.getValue());
		
		return validateCapacity(slots);
	}
	
	/**
	 * @param tradeList : The {@link TradeList} to test.
	 * @return True if the {@link TradeList} set as parameter can pass a {@link #validateCapacity(int)} check.
	 */
	public boolean validateTradeListCapacity(TradeList tradeList)
	{
		int slots = 0;
		for (TradeItem tradeItem : tradeList)
			slots += calculateUsedSlots(tradeItem.getItem(), tradeItem.getCount());
		
		return validateCapacity(slots);
	}
	
	/**
	 * @param template : The {@link Item} to test.
	 * @param itemCount : The {@link Item} count to add.
	 * @return The number of used slots for a given {@link Item}.
	 */
	private int calculateUsedSlots(Item template, int itemCount)
	{
		final ItemInstance item = getItemByItemId(template.getItemId());
		if (item != null)
			return (item.isStackable()) ? 0 : itemCount;
		
		return (template.isStackable()) ? 1 : itemCount;
	}
	
	/**
	 * @param itemId : The {@link Item} id to test.
	 * @param itemCount : The {@link Item} count to add.
	 * @return The number of used slots for a given {@link Item} id.
	 */
	private int calculateUsedSlots(int itemId, int itemCount)
	{
		final ItemInstance item = getItemByItemId(itemId);
		if (item != null)
			return (item.isStackable()) ? 0 : itemCount;
		
		final Item template = ItemData.getInstance().getTemplate(itemId);
		return (template.isStackable()) ? 1 : itemCount;
	}
	
	@Override
	public boolean validateCapacity(int slotCount)
	{
		if (slotCount == 0)
			return true;
		
		return (_items.size() + slotCount <= getOwner().getStatus().getInventoryLimit());
	}
	
	@Override
	public boolean validateWeight(int weight)
	{
		return _totalWeight + weight <= _owner.getWeightLimit();
	}
	
	/**
	 * @param tradeList : The {@link TradeList} to test.
	 * @return True if the {@link TradeList} set as parameter can pass a {@link #validateWeight(int)} check.
	 */
	public boolean validateTradeListWeight(TradeList tradeList)
	{
		int weight = 0;
		for (TradeItem tradeItem : tradeList)
			weight += tradeItem.getItem().getWeight() * tradeItem.getCount();
		
		return validateWeight(weight);
	}
	
	/**
	 * @param tradeList : The {@link TradeList} to test.
	 * @return True if each item count from the {@link TradeList} set as parameter can pass an Integer.MAX_VALUE check, or false otherwise.
	 */
	public boolean validateTradeListCount(TradeList tradeList)
	{
		for (TradeItem tradeItem : tradeList)
		{
			long count = tradeItem.getCount();
			
			if (tradeItem.getItem().isStackable())
			{
				final ItemInstance item = getItemByItemId(tradeItem.getItem().getItemId());
				if (item != null)
					count += item.getCount();
			}
			
			if (count > Integer.MAX_VALUE)
				return false;
		}
		return true;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + _owner + "]";
	}
	
	/**
	 * @param itemsToCheck : The {@link BuyProcessItem} array to test.
	 * @return True if the {@link BuyProcessItem} array set as parameter successfully pass inventory checks, false otherwise.
	 */
	public boolean canPassBuyProcess(BuyProcessItem[] itemsToCheck)
	{
		for (BuyProcessItem itemToCheck : itemsToCheck)
		{
			if (itemToCheck.getCount() < 1 || itemToCheck.getPrice() < 0)
				return false;
			
			final ItemInstance item = getItemByItemId(itemToCheck.getItemId());
			if (item == null || item.getEnchantLevel() != itemToCheck.getEnchant())
				return false;
		}
		return true;
	}
	
	/**
	 * @param itemsToCheck : The {@link SellProcessItem} array to test.
	 * @return True if the {@link SellProcessItem} array set as parameter successfully pass inventory checks, false otherwise.
	 */
	public boolean canPassSellProcess(SellProcessItem[] itemsToCheck)
	{
		for (SellProcessItem itemToCheck : itemsToCheck)
		{
			if (itemToCheck.getCount() < 1 || itemToCheck.getPrice() < 0)
				return false;
			
			final ItemInstance item = getItemByObjectId(itemToCheck.getObjectId());
			if (item == null || item.getCount() < itemToCheck.getCount())
				return false;
		}
		return true;
	}
	
	/**
	 * Re-notify to paperdoll listeners every equipped item
	 */
	public void reloadEquippedItems()
	{
		for (ItemInstance item : getPaperdollItems())
		{
			final Paperdoll slot = Paperdoll.getEnumById(item.getLocationSlot());
			
			for (OnEquipListener listener : _paperdollListeners)
			{
				listener.onUnequip(slot, item, getOwner());
				listener.onEquip(slot, item, getOwner());
			}
		}
	}
}
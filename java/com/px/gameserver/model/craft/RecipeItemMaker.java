package com.px.gameserver.model.craft;

import com.px.commons.random.Rnd;

import com.px.Config;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.holder.IntIntHolder;
import com.px.gameserver.model.item.Recipe;
import com.px.gameserver.model.item.instance.ItemInstance;
import com.px.gameserver.model.itemcontainer.Inventory;
import com.px.gameserver.network.SystemMessageId;
import com.px.gameserver.network.serverpackets.ActionFailed;
import com.px.gameserver.network.serverpackets.RecipeItemMakeInfo;
import com.px.gameserver.network.serverpackets.RecipeShopItemInfo;
import com.px.gameserver.network.serverpackets.SystemMessage;
import com.px.gameserver.skills.L2Skill;

/**
 * The core of craft system, which allow {@link Player} to exchange materials for a final product. Numerous checks are made (integrity checks, such as item existence, mana left, adena cost).<br>
 * <br>
 * Required mats / final product infos are controlled by a {@link Recipe}.
 */
public class RecipeItemMaker implements Runnable
{
	public boolean _isValid;
	
	protected final Recipe _recipe;
	protected final Player _player; // "crafter"
	protected final Player _target; // "customer"
	protected final int _skillId;
	protected final int _skillLevel;
	protected double _manaRequired;
	protected int _price;
	
	public RecipeItemMaker(Player player, Recipe recipe, Player target)
	{
		_player = player;
		_target = target;
		_recipe = recipe;
		
		_isValid = false;
		_skillId = (_recipe.isDwarven()) ? L2Skill.SKILL_CREATE_DWARVEN : L2Skill.SKILL_CREATE_COMMON;
		_skillLevel = _player.getSkillLevel(_skillId);
		
		_manaRequired = _recipe.getMpCost();
		
		_player.setCrafting(true);
		
		if (_player.isAlikeDead() || _target.isAlikeDead())
		{
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			abort();
			return;
		}
		
		if (_player.isProcessingTransaction() || _target.isProcessingTransaction())
		{
			_target.sendPacket(ActionFailed.STATIC_PACKET);
			abort();
			return;
		}
		
		// Validate skill level.
		if (_recipe.getLevel() > _skillLevel)
		{
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			abort();
			return;
		}
		
		// Check if that customer can afford to pay for creation services. Also check manufacturer integrity.
		if (_player != _target)
		{
			for (ManufactureItem temp : _player.getManufactureList())
			{
				// Find recipe for item we want manufactured.
				if (temp.getId() == _recipe.getId())
				{
					_price = temp.getValue();
					if (_target.getAdena() < _price)
					{
						_target.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
						abort();
						return;
					}
					break;
				}
			}
		}
		
		// Check if inventory got all required materials.
		if (!listItems(false))
		{
			abort();
			return;
		}
		
		// Initial mana check requires MP as written on recipe.
		if (_player.getStatus().getMp() < _manaRequired)
		{
			_target.sendPacket(SystemMessageId.NOT_ENOUGH_MP);
			abort();
			return;
		}
		
		_player.setCrafting(false);
		_isValid = true;
	}
	
	@Override
	public void run()
	{
		if (!Config.IS_CRAFTING_ENABLED)
		{
			_target.sendMessage("Item creation is currently disabled.");
			abort();
			return;
		}
		
		if (_player == null || _target == null)
		{
			abort();
			return;
		}
		
		if (!_player.isOnline() || !_target.isOnline())
		{
			abort();
			return;
		}
		
		_player.getStatus().reduceMp(_manaRequired);
		
		// First take adena for manufacture ; customer must pay for services.
		if (_target != _player && _price > 0)
		{
			final ItemInstance adenaTransfer = _target.transferItem("PayManufacture", _target.getInventory().getAdenaInstance().getObjectId(), _price, _player);
			if (adenaTransfer == null)
			{
				_target.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
				abort();
				return;
			}
		}
		
		// Inventory check failed.
		if (!listItems(true))
		{
			abort();
			return;
		}
		
		// Success ; we reward the player and update the craft window.
		if (Rnd.get(100) < _recipe.getSuccessRate())
		{
			rewardPlayer();
			updateMakeInfo(true);
		}
		// Fail ; we only send messages and update craft window.
		else
		{
			if (_target != _player)
			{
				_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CREATION_OF_S2_FOR_S1_AT_S3_ADENA_FAILED).addCharName(_target).addItemName(_recipe.getProduct().getId()).addItemNumber(_price));
				_target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_FAILED_TO_CREATE_S2_FOR_S3_ADENA).addCharName(_player).addItemName(_recipe.getProduct().getId()).addItemNumber(_price));
			}
			else
				_target.sendPacket(SystemMessageId.ITEM_MIXING_FAILED);
			
			updateMakeInfo(false);
		}
		
		_player.setCrafting(false);
	}
	
	/**
	 * Send to the {@link Player} customer {@link RecipeItemMakeInfo} (self crafting) or {@link RecipeShopItemInfo} (private workshop) packet.
	 * @param success : The result under a boolean, used by packet.
	 */
	private void updateMakeInfo(boolean success)
	{
		if (_target == _player)
			_target.sendPacket(new RecipeItemMakeInfo(_recipe.getId(), _target, (success) ? 1 : 0));
		else
			_target.sendPacket(new RecipeShopItemInfo(_player, _recipe.getId()));
	}
	
	/**
	 * List all required materials.
	 * @param remove : If true we also delete items from customer inventory.
	 * @return true if the {@link Player} customer got every item (with correct amount) on inventory.
	 */
	private boolean listItems(boolean remove)
	{
		final Inventory inv = _target.getInventory();
		
		boolean gotAllMats = true;
		for (IntIntHolder material : _recipe.getMaterials())
		{
			final int quantity = material.getValue();
			if (quantity > 0)
			{
				final ItemInstance item = inv.getItemByItemId(material.getId());
				if (item == null || item.getCount() < quantity)
				{
					_target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.MISSING_S2_S1_TO_CREATE).addItemName(material.getId()).addItemNumber((item == null) ? quantity : quantity - item.getCount()));
					gotAllMats = false;
				}
			}
		}
		
		if (!gotAllMats)
			return false;
		
		if (remove)
		{
			for (IntIntHolder material : _recipe.getMaterials())
			{
				inv.destroyItemByItemId("Manufacture", material.getId(), material.getValue(), _target, _player);
				
				if (material.getValue() > 1)
					_target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED).addItemName(material.getId()).addItemNumber(material.getValue()));
				else
					_target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED).addItemName(material.getId()));
			}
		}
		return true;
	}
	
	/**
	 * Abort the crafting mode for the {@link Player}.
	 */
	private void abort()
	{
		updateMakeInfo(false);
		_player.setCrafting(false);
	}
	
	/**
	 * Reward a {@link Player} with the result of a craft (retained into a {@link IntIntHolder}).
	 */
	private void rewardPlayer()
	{
		final int itemId = _recipe.getProduct().getId();
		final int itemCount = _recipe.getProduct().getValue();
		
		_target.getInventory().addItem("Manufacture", itemId, itemCount, _target, _player);
		
		// inform customer of earned item
		if (_target != _player)
		{
			// inform manufacturer of earned profit
			if (itemCount == 1)
			{
				_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_CREATED_FOR_S1_FOR_S3_ADENA).addString(_target.getName()).addItemName(itemId).addItemNumber(_price));
				_target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CREATED_S2_FOR_S3_ADENA).addString(_player.getName()).addItemName(itemId).addItemNumber(_price));
			}
			else
			{
				_player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S2_S3_S_CREATED_FOR_S1_FOR_S4_ADENA).addString(_target.getName()).addNumber(itemCount).addItemName(itemId).addItemNumber(_price));
				_target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CREATED_S2_S3_S_FOR_S4_ADENA).addString(_player.getName()).addNumber(itemCount).addItemName(itemId).addItemNumber(_price));
			}
		}
		
		if (itemCount > 1)
			_target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_S2_S1_S).addItemName(itemId).addNumber(itemCount));
		else
			_target.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1).addItemName(itemId));
	}
}
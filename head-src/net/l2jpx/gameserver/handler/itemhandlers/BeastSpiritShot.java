package net.l2jpx.gameserver.handler.itemhandlers;

import net.l2jpx.Config;
import net.l2jpx.gameserver.handler.IItemHandler;
import net.l2jpx.gameserver.model.L2Summon;
import net.l2jpx.gameserver.model.actor.instance.L2BabyPetInstance;
import net.l2jpx.gameserver.model.actor.instance.L2ItemInstance;
import net.l2jpx.gameserver.model.actor.instance.L2PcInstance;
import net.l2jpx.gameserver.model.actor.instance.L2PetInstance;
import net.l2jpx.gameserver.model.actor.instance.L2PlayableInstance;
import net.l2jpx.gameserver.network.SystemMessageId;
import net.l2jpx.gameserver.network.serverpackets.ExAutoSoulShot;
import net.l2jpx.gameserver.network.serverpackets.MagicSkillUser;
import net.l2jpx.gameserver.network.serverpackets.SystemMessage;
import net.l2jpx.gameserver.templates.L2Weapon;
import net.l2jpx.gameserver.util.Broadcast;

/**
 * @author programmos
 * @author l2jfrozen dev
 * @author ReynalDev
 */
public class BeastSpiritShot implements IItemHandler
{
	// All the item IDs that this handler knows.
	private static final int[] ITEM_IDS =
	{
		6646,
		6647
	};
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		if (playable == null)
		{
			return;
		}
		
		L2PcInstance activeOwner = null;
		
		if (playable instanceof L2Summon)
		{
			activeOwner = ((L2Summon) playable).getOwner();
			activeOwner.sendPacket(new SystemMessage(SystemMessageId.THIS_PET_CANNOT_USE_THIS_ITEM));
			return;
		}
		else if (playable instanceof L2PcInstance)
		{
			activeOwner = (L2PcInstance) playable;
		}
		
		if (activeOwner == null)
		{
			return;
		}
		
		L2Summon activePet = activeOwner.getPet();
		if (activePet == null)
		{
			activeOwner.sendPacket(new SystemMessage(SystemMessageId.PETS_AND_SERVITORS_ARE_NOT_AVAILABLE_AT_THIS_TIME));
			return;
		}
		
		if (activePet.isDead())
		{
			activeOwner.sendPacket(new SystemMessage(SystemMessageId.SOULSHOTS_AND_SPIRITSHOTS_ARE_NOT_AVAILABLE_FOR_A_DEAD_PET));
			return;
		}
		
		final int itemId = item.getItemId();
		final boolean isBlessed = itemId == 6647;
		int shotConsumption = 1;
		
		L2ItemInstance weaponInst = null;
		L2Weapon weaponItem = null;
		
		if (activePet instanceof L2PetInstance && !(activePet instanceof L2BabyPetInstance))
		{
			weaponInst = ((L2PetInstance) activePet).getActiveWeaponInstance();
			weaponItem = ((L2PetInstance) activePet).getActiveWeaponItem();
			
			if (weaponInst == null)
			{
				activeOwner.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_USE_SPIRITSHOTS));
				return;
			}
			
			if (weaponInst.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
			{
				// SpiritShots are already active.
				return;
			}
			
			final int shotCount = item.getCount();
			shotConsumption = weaponItem.getSpiritShotCount();
			
			weaponItem = null;
			
			if (shotConsumption == 0)
			{
				activeOwner.sendPacket(new SystemMessage(SystemMessageId.YOU_MAY_NOT_USE_SPIRITSHOTS));
				return;
			}
			
			if (!(shotCount > shotConsumption))
			{
				// Not enough SpiritShots to use.
				activeOwner.sendPacket(new SystemMessage(SystemMessageId.NOT_ENOUGH_SPIRITHOTS_FOR_PET));
				return;
			}
			
			if (isBlessed)
			{
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
			}
			else
			{
				weaponInst.setChargedSpiritshot(L2ItemInstance.CHARGED_SPIRITSHOT);
			}
		}
		else
		{
			if (activePet.getChargedSpiritShot() != L2ItemInstance.CHARGED_NONE)
			{
				return;
			}
			
			if (isBlessed)
			{
				activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_BLESSED_SPIRITSHOT);
			}
			else
			{
				activePet.setChargedSpiritShot(L2ItemInstance.CHARGED_SPIRITSHOT);
			}
		}
		
		// TODO: test ss
		if (!Config.DONT_DESTROY_SS)
		{
			if (!activeOwner.destroyItemWithoutTrace("Consume", item.getObjectId(), shotConsumption, null, false))
			{
				if (activeOwner.getAutoSoulShot().contains(itemId))
				{
					activeOwner.removeAutoSoulShot(itemId);
					activeOwner.sendPacket(new ExAutoSoulShot(itemId, 0));
					SystemMessage sm = new SystemMessage(SystemMessageId.AUTO_USE_OF_S1_CANCELLED);
					sm.addString(item.getItem().getName());
					activeOwner.sendPacket(sm);
					sm = null;
					
					return;
				}
				
				activeOwner.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_SPIRITSHOTS_FOR_THAT));
				return;
			}
		}
		
		// Pet uses the power of spirit.
		activeOwner.sendPacket(new SystemMessage(SystemMessageId.PET_USE_THE_POWER_OF_SPIRIT));
		if (!activeOwner.isSSDisabled())
			Broadcast.toSelfAndKnownPlayersInRadius(activeOwner, new MagicSkillUser(activePet, activePet, isBlessed ? 2009 : 2008, 1, 0, 0), 360000/* 600 */);
		
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}

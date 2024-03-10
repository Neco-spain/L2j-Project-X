package net.l2jpx.gameserver.handler.itemhandlers;

import net.l2jpx.gameserver.handler.IItemHandler;
import net.l2jpx.gameserver.model.actor.instance.L2ItemInstance;
import net.l2jpx.gameserver.model.actor.instance.L2PcInstance;
import net.l2jpx.gameserver.model.actor.instance.L2PlayableInstance;
import net.l2jpx.gameserver.network.SystemMessageId;
import net.l2jpx.gameserver.network.serverpackets.ChooseInventoryItem;
import net.l2jpx.gameserver.network.serverpackets.SystemMessage;

public class EnchantScrolls implements IItemHandler
{
	private static final int[] ITEM_IDS =
	{
		729,
		730,
		731,
		732,
		6569,
		6570, // a grade
		947,
		948,
		949,
		950,
		6571,
		6572, // b grade
		951,
		952,
		953,
		954,
		6573,
		6574, // c grade
		955,
		956,
		957,
		958,
		6575,
		6576, // d grade
		959,
		960,
		961,
		962,
		6577,
		6578
		// s grade
	};
	
	@Override
	public void useItem(final L2PlayableInstance playable, final L2ItemInstance item)
	{
		if (!(playable instanceof L2PcInstance))
		{
			return;
		}
		
		final L2PcInstance activeChar = (L2PcInstance) playable;
		if (activeChar.isCastingNow() || activeChar.isCastingPotionNow())
		{
			return;
		}
		
		activeChar.setActiveEnchantItem(item);
		activeChar.sendPacket(new SystemMessage(SystemMessageId.SELECT_ITEM_TO_ENCHANT));
		activeChar.sendPacket(new ChooseInventoryItem(item.getItemId()));
		return;
	}
	
	@Override
	public int[] getItemIds()
	{
		return ITEM_IDS;
	}
}
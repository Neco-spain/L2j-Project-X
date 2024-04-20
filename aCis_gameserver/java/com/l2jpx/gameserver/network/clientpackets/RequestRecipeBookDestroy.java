package com.l2jpx.gameserver.network.clientpackets;

import com.l2jpx.gameserver.data.xml.RecipeData;
import com.l2jpx.gameserver.enums.actors.OperateType;
import com.l2jpx.gameserver.model.actor.Player;
import com.l2jpx.gameserver.model.item.Recipe;
import com.l2jpx.gameserver.network.SystemMessageId;
import com.l2jpx.gameserver.network.serverpackets.RecipeBookItemList;
import com.l2jpx.gameserver.network.serverpackets.SystemMessage;

public final class RequestRecipeBookDestroy extends L2GameClientPacket
{
	private int _recipeId;
	
	@Override
	protected void readImpl()
	{
		_recipeId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (player.getOperateType() == OperateType.MANUFACTURE)
		{
			player.sendPacket(SystemMessageId.CANT_ALTER_RECIPEBOOK_WHILE_CRAFTING);
			return;
		}
		
		final Recipe recipe = RecipeData.getInstance().getRecipeList(_recipeId);
		if (recipe == null)
			return;
		
		player.getRecipeBook().removeRecipe(_recipeId);
		player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED).addItemName(recipe.getRecipeId()));
		player.sendPacket(new RecipeBookItemList(player, recipe.isDwarven()));
	}
}
package com.px.gameserver.model.craft;

import com.px.gameserver.data.xml.RecipeData;
import com.px.gameserver.model.holder.IntIntHolder;

/**
 * A datatype extending {@link IntIntHolder}. It is part of private workshop system, and is used to hold individual entries.
 */
public class ManufactureItem extends IntIntHolder
{
	private final boolean _isDwarven;
	
	public ManufactureItem(int recipeId, int cost)
	{
		super(recipeId, cost);
		
		_isDwarven = RecipeData.getInstance().getRecipeList(recipeId).isDwarven();
	}
	
	public boolean isDwarven()
	{
		return _isDwarven;
	}
}
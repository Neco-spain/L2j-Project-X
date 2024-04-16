package com.px.gameserver.model.memo;

import java.util.Map;

import com.px.commons.data.MemoSet;

/**
 * An implementation of {@link MemoSet} used for Npc.
 */
public class SpawnMemo extends MemoSet
{
	public static final SpawnMemo DUMMY_SET = new SpawnMemo();
	
	private static final long serialVersionUID = 1L;
	
	public SpawnMemo()
	{
		super();
	}
	
	public SpawnMemo(final int size)
	{
		super(size);
	}
	
	public SpawnMemo(final Map<String, String> m)
	{
		super(m);
	}
	
	@Override
	protected void onSet(String key, String value)
	{
	}
	
	@Override
	protected void onUnset(String key)
	{
	}
}
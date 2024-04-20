package com.l2jpx.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.l2jpx.commons.pool.ConnectionPool;

import com.l2jpx.gameserver.data.sql.PlayerInfoTable;
import com.l2jpx.gameserver.model.World;
import com.l2jpx.gameserver.model.actor.Player;
import com.l2jpx.gameserver.network.SystemMessageId;
import com.l2jpx.gameserver.network.serverpackets.L2Friend;
import com.l2jpx.gameserver.network.serverpackets.SystemMessage;

public final class RequestFriendDel extends L2GameClientPacket
{
	private static final String DELETE_FRIEND = "DELETE FROM character_friends WHERE (char_id = ? AND friend_id = ?) OR (char_id = ? AND friend_id = ?)";
	
	private String _targetName;
	
	@Override
	protected void readImpl()
	{
		_targetName = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final int targetId = PlayerInfoTable.getInstance().getPlayerObjectId(_targetName);
		if (targetId == -1 || !player.getFriendList().contains(targetId))
		{
			player.sendPacket(SystemMessageId.THE_USER_NOT_IN_FRIENDS_LIST);
			return;
		}
		
		player.getFriendList().remove(Integer.valueOf(targetId));
		
		final Player target = World.getInstance().getPlayer(_targetName);
		if (target != null)
		{
			player.sendPacket(new L2Friend(target, 3));
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST).addString(_targetName));
			
			target.getFriendList().remove(Integer.valueOf(player.getObjectId()));
			target.sendPacket(new L2Friend(player, 3));
		}
		else
		{
			player.sendPacket(new L2Friend(_targetName, 3));
			player.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_HAS_BEEN_DELETED_FROM_YOUR_FRIENDS_LIST).addString(_targetName));
		}
		
		try (Connection con = ConnectionPool.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_FRIEND))
		{
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, targetId);
			ps.setInt(3, targetId);
			ps.setInt(4, player.getObjectId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.error("Couldn't delete friendId {} for {}.", e, targetId, player.toString());
		}
	}
}
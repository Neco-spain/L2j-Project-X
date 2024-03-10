package engine.engine.mods;

import java.util.HashMap;
import java.util.Map;

import net.l2jpx.gameserver.model.actor.instance.L2PcInstance;
import net.l2jpx.gameserver.network.clientpackets.Say2;
import net.l2jpx.gameserver.network.serverpackets.PlaySound;

import engine.data.properties.ConfigData;
import engine.engine.AbstractMod;
import engine.holders.objects.CharacterHolder;
import engine.holders.objects.PlayerHolder;
import engine.util.Util;
import engine.util.UtilMessage;

/**
 * Class responsible for managing ads deaths according to consecutive kills amount.
 * @author fissban
 */
public class SpreeKills extends AbstractMod
{
	//
	private static final Map<Integer, Integer> players = new HashMap<>();
	
	public SpreeKills()
	{
		registerMod(ConfigData.ENABLE_SpreeKills);
	}
	
	@Override
	public void onModState()
	{
		//
	}
	
	@Override
	public void onDeath(CharacterHolder player)
	{
		if (players.containsKey(player.getObjectId()))
		{
			players.remove(player.getObjectId());
		}
	}
	
	@Override
	public void onKill(CharacterHolder killer, CharacterHolder victim, boolean isPet)
	{
		if (!Util.areObjectType(L2PcInstance.class, victim) || (killer.getActingPlayer() == null))
		{
			return;
		}
		
		PlayerHolder ph = killer.getActingPlayer();
		
		int count = 1;
		if (players.containsKey(ph.getObjectId()))
		{
			count = players.get(ph.getObjectId());
			count++;
		}
		
		players.put(ph.getObjectId(), count);
		
		// animation Lightning Strike(279)
		// activeChar.broadcastPacket(new MagicSkillUse(activeChar, victim, 279, 1, 500, 500));
		
		int size1 = ConfigData.ANNOUNCEMENTS_KILLS.size();
		int size2 = ConfigData.SOUNDS_KILLS.size();
		
		String msg = count > size1 ? ConfigData.ANNOUNCEMENTS_KILLS.get(size1) : ConfigData.ANNOUNCEMENTS_KILLS.get(count);
		String sound = count > size2 ? ConfigData.SOUNDS_KILLS.get(size2) : ConfigData.SOUNDS_KILLS.get(count);
		
		// Announcement to all characters.
		UtilMessage.toAllOnline(Say2.TELL, msg.replace("%s1", ph.getName()));
		// play music to all characters.
		UtilMessage.toAllOnline(new PlaySound(sound));
	}
}
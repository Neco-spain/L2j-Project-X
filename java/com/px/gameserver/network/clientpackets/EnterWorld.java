package com.px.gameserver.network.clientpackets;

import java.util.Map.Entry;

import com.px.Config;
import com.px.gameserver.communitybbs.manager.MailBBSManager;
import com.px.gameserver.data.SkillTable.FrequentSkill;
import com.px.gameserver.data.manager.CastleManager;
import com.px.gameserver.data.manager.ClanHallManager;
import com.px.gameserver.data.manager.CoupleManager;
import com.px.gameserver.data.manager.PetitionManager;
import com.px.gameserver.data.manager.SevenSignsManager;
import com.px.gameserver.data.xml.AdminData;
import com.px.gameserver.data.xml.AnnouncementData;
import com.px.gameserver.enums.CabalType;
import com.px.gameserver.enums.RestartType;
import com.px.gameserver.enums.SealType;
import com.px.gameserver.enums.SiegeSide;
import com.px.gameserver.enums.ZoneId;
import com.px.gameserver.enums.actors.ClassRace;
import com.px.gameserver.model.World;
import com.px.gameserver.model.actor.Player;
import com.px.gameserver.model.holder.IntIntHolder;
import com.px.gameserver.model.olympiad.Olympiad;
import com.px.gameserver.model.pledge.Clan;
import com.px.gameserver.model.pledge.SubPledge;
import com.px.gameserver.model.residence.castle.Castle;
import com.px.gameserver.model.residence.castle.Siege;
import com.px.gameserver.model.residence.clanhall.ClanHall;
import com.px.gameserver.model.residence.clanhall.SiegableHall;
import com.px.gameserver.network.GameClient.GameClientState;
import com.px.gameserver.network.SystemMessageId;
import com.px.gameserver.network.serverpackets.ActionFailed;
import com.px.gameserver.network.serverpackets.Die;
import com.px.gameserver.network.serverpackets.EtcStatusUpdate;
import com.px.gameserver.network.serverpackets.ExMailArrived;
import com.px.gameserver.network.serverpackets.ExStorageMaxCount;
import com.px.gameserver.network.serverpackets.FriendList;
import com.px.gameserver.network.serverpackets.HennaInfo;
import com.px.gameserver.network.serverpackets.ItemList;
import com.px.gameserver.network.serverpackets.NpcHtmlMessage;
import com.px.gameserver.network.serverpackets.PlaySound;
import com.px.gameserver.network.serverpackets.PledgeShowMemberListAll;
import com.px.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.px.gameserver.network.serverpackets.PledgeSkillList;
import com.px.gameserver.network.serverpackets.QuestList;
import com.px.gameserver.network.serverpackets.ShortCutInit;
import com.px.gameserver.network.serverpackets.SkillCoolTime;
import com.px.gameserver.network.serverpackets.SystemMessage;
import com.px.gameserver.network.serverpackets.UserInfo;
import com.px.gameserver.scripting.Quest;
import com.px.gameserver.scripting.QuestState;
import com.px.gameserver.skills.L2Skill;
import com.px.gameserver.taskmanager.GameTimeTaskManager;

public class EnterWorld extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
		{
			getClient().closeNow();
			return;
		}
		
		getClient().setState(GameClientState.IN_GAME);
		
		final int objectId = player.getObjectId();
		
		if (player.isGM())
		{
			if (Config.GM_STARTUP_INVULNERABLE && AdminData.getInstance().hasAccess("admin_invul", player.getAccessLevel()))
				player.setInvul(true);
			
			if (Config.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_hide", player.getAccessLevel()))
				player.getAppearance().setVisible(false);
			
			if (Config.GM_STARTUP_BLOCK_ALL)
				player.setInBlockingAll(true);
			
			if (Config.GM_STARTUP_AUTO_LIST && AdminData.getInstance().hasAccess("admin_gmlist", player.getAccessLevel()))
				AdminData.getInstance().addGm(player, false);
			else
				AdminData.getInstance().addGm(player, true);
		}
		
		// Set dead status if applies
		if (player.getStatus().getHp() < 0.5 && player.isMortal())
			player.setIsDead(true);
		
		player.getMacroList().sendUpdate();
		player.sendPacket(new ExStorageMaxCount(player));
		player.sendPacket(new HennaInfo(player));
		player.updateEffectIcons();
		player.sendPacket(new EtcStatusUpdate(player));
		
		// Clan checks.
		final Clan clan = player.getClan();
		if (clan != null)
		{
			player.sendPacket(new PledgeSkillList(clan));
			
			// Refresh player instance.
			clan.getClanMember(objectId).setPlayerInstance(player);
			
			final SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.CLAN_MEMBER_S1_LOGGED_IN).addCharName(player);
			final PledgeShowMemberListUpdate psmlu = new PledgeShowMemberListUpdate(player);
			
			// Send packets to others members.
			for (Player member : clan.getOnlineMembers())
			{
				if (member == player)
					continue;
				
				member.sendPacket(sm);
				member.sendPacket(psmlu);
			}
			
			// Send a login notification to sponsor or apprentice, if logged.
			if (player.getSponsor() != 0)
			{
				final Player sponsor = World.getInstance().getPlayer(player.getSponsor());
				if (sponsor != null)
					sponsor.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN).addCharName(player));
			}
			else if (player.getApprentice() != 0)
			{
				final Player apprentice = World.getInstance().getPlayer(player.getApprentice());
				if (apprentice != null)
					apprentice.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOUR_SPONSOR_S1_HAS_LOGGED_IN).addCharName(player));
			}
			
			// Add message at connexion if clanHall not paid.
			final ClanHall ch = ClanHallManager.getInstance().getClanHallByOwner(clan);
			if (ch != null && !ch.getPaid())
				player.sendPacket(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
			
			for (Castle castle : CastleManager.getInstance().getCastles())
			{
				final Siege siege = castle.getSiege();
				if (!siege.isInProgress())
					continue;
				
				final SiegeSide type = siege.getSide(clan);
				if (type == SiegeSide.ATTACKER)
					player.setSiegeState((byte) 1);
				else if (type == SiegeSide.DEFENDER || type == SiegeSide.OWNER)
					player.setSiegeState((byte) 2);
			}
			
			for (SiegableHall hall : ClanHallManager.getInstance().getSiegableHalls())
			{
				if (hall.isInSiege() && hall.isRegistered(clan))
					player.setSiegeState((byte) 1);
			}
			
			player.sendPacket(new PledgeShowMemberListUpdate(player));
			player.sendPacket(new PledgeShowMemberListAll(clan, 0));
			
			for (SubPledge sp : clan.getAllSubPledges())
				player.sendPacket(new PledgeShowMemberListAll(clan, sp.getId()));
			
			player.sendPacket(new UserInfo(player));
		}
		
		// Updating Seal of Strife Buff/Debuff
		if (SevenSignsManager.getInstance().isSealValidationPeriod() && SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE) != CabalType.NORMAL)
		{
			CabalType cabal = SevenSignsManager.getInstance().getPlayerCabal(objectId);
			if (cabal != CabalType.NORMAL)
			{
				if (cabal == SevenSignsManager.getInstance().getSealOwner(SealType.STRIFE))
					player.addSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill(), false);
				else
					player.addSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill(), false);
			}
		}
		else
		{
			player.removeSkill(FrequentSkill.THE_VICTOR_OF_WAR.getSkill().getId(), false);
			player.removeSkill(FrequentSkill.THE_VANQUISHED_OF_WAR.getSkill().getId(), false);
		}
		
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
			player.setSpawnProtection(true);
		
		player.spawnMe();
		
		// Set the location of debug packets.
		player.setEnterWorldLoc(player.getX(), player.getY(), -16000);
		
		// Engage and notify partner.
		for (Entry<Integer, IntIntHolder> coupleEntry : CoupleManager.getInstance().getCouples().entrySet())
		{
			final IntIntHolder couple = coupleEntry.getValue();
			if (couple.getId() == objectId || couple.getValue() == objectId)
			{
				player.setCoupleId(coupleEntry.getKey());
				break;
			}
		}
		
		// Announcements, welcome & Seven signs period messages.
		player.sendPacket(SystemMessageId.WELCOME_TO_LINEAGE);
		player.sendPacket(SevenSignsManager.getInstance().getCurrentPeriod().getMessageId());
		AnnouncementData.getInstance().showAnnouncements(player, false);
		
		// If the Player is a Dark Elf, check for Shadow Sense at night.
		if (player.getRace() == ClassRace.DARK_ELF && player.hasSkill(L2Skill.SKILL_SHADOW_SENSE))
			player.sendPacket(SystemMessage.getSystemMessage((GameTimeTaskManager.getInstance().isNight()) ? SystemMessageId.NIGHT_S1_EFFECT_APPLIES : SystemMessageId.DAY_S1_EFFECT_DISAPPEARS).addSkillName(L2Skill.SKILL_SHADOW_SENSE));
		
		// Notify quest for enterworld event, if quest allows it.
		player.getQuestList().getQuests(Quest::isTriggeredOnEnterWorld).forEach(q -> q.onEnterWorld(player));
		
		player.sendPacket(new QuestList(player));
		player.sendSkillList();
		player.sendPacket(new FriendList(player));
		player.sendPacket(new UserInfo(player));
		player.sendPacket(new ItemList(player, false));
		player.sendPacket(new ShortCutInit(player));
		
		// No broadcast needed since the player will already spawn dead to others.
		if (player.isAlikeDead())
			player.sendPacket(new Die(player));
		
		// Unread mails make a popup appears.
		if (Config.ENABLE_COMMUNITY_BOARD && MailBBSManager.getInstance().checkIfUnreadMail(player))
		{
			player.sendPacket(SystemMessageId.NEW_MAIL);
			player.sendPacket(new PlaySound("systemmsg_e.1233"));
			player.sendPacket(ExMailArrived.STATIC_PACKET);
		}
		
		// Clan notice, if active.
		if (Config.ENABLE_COMMUNITY_BOARD && clan != null && clan.isNoticeEnabled())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/clan_notice.htm");
			html.replace("%clan_name%", clan.getName());
			html.replace("%notice_text%", clan.getNotice().replaceAll("\r\n", "<br>").replaceAll("action", "").replaceAll("bypass", ""));
			sendPacket(html);
		}
		else if (Config.SERVER_NEWS)
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			html.setFile("data/html/servnews.htm");
			sendPacket(html);
		}
		
		PetitionManager.getInstance().checkActivePetition(player);
		
		player.onPlayerEnter();
		
		sendPacket(new SkillCoolTime(player));
		
		// If player logs back in a stadium, port him in nearest town.
		if (Olympiad.getInstance().playerInStadia(player))
			player.teleportTo(RestartType.TOWN);
		
		if (player.getClanJoinExpiryTime() > System.currentTimeMillis())
			player.sendPacket(SystemMessageId.CLAN_MEMBERSHIP_TERMINATED);
		
		// Attacker or spectator logging into a siege zone will be ported at town.
		if (player.isInsideZone(ZoneId.SIEGE) && player.getSiegeState() < 2)
			player.teleportTo(RestartType.TOWN);
		
		// Tutorial
		final QuestState qs = player.getQuestList().getQuestState("Tutorial");
		if (qs != null)
			qs.getQuest().notifyEvent("UC", null, player);
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	protected boolean triggersOnActionRequest()
	{
		return false;
	}
}
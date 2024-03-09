package net.l2jpx.gameserver.handler.skillhandlers;

import org.apache.log4j.Logger;

import net.l2jpx.gameserver.handler.ISkillHandler;
import net.l2jpx.gameserver.model.L2Character;
import net.l2jpx.gameserver.model.L2Object;
import net.l2jpx.gameserver.model.L2Skill;
import net.l2jpx.gameserver.model.L2Skill.SkillType;
import net.l2jpx.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author drunk
 */
public class DrainSoul implements ISkillHandler
{
	private static Logger LOGGER = Logger.getLogger(DrainSoul.class);
	private static final SkillType[] SKILL_IDS =
	{
		SkillType.DRAIN_SOUL
	};
	
	@Override
	public void useSkill(final L2Character activeChar, final L2Skill skill, final L2Object[] targets)
	{
		if (!(activeChar instanceof L2PcInstance))
		{
			return;
		}
		
		L2Object[] targetList = skill.getTargetList(activeChar);
		
		if (targetList == null)
		{
			return;
		}
		
		targetList = null;
		
		LOGGER.debug("Soul Crystal casting succeded.");
		
		// This is just a dummy skill handler for the soul crystal skill,
		// since the Soul Crystal item handler already does everything.
	}
	
	@Override
	public SkillType[] getSkillIds()
	{
		return SKILL_IDS;
	}
}

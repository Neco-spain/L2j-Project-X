package com.px.gameserver.model.actor.container.creature;

import java.util.concurrent.Future;

import com.px.commons.math.MathUtil;
import com.px.commons.pool.ThreadPool;

import com.px.gameserver.data.SkillTable;
import com.px.gameserver.enums.AiEventType;
import com.px.gameserver.geoengine.GeoEngine;
import com.px.gameserver.model.actor.Creature;
import com.px.gameserver.skills.AbstractEffect;
import com.px.gameserver.skills.L2Skill;
import com.px.gameserver.skills.effects.EffectFusion;

public final class FusionSkill
{
	protected Creature _caster;
	protected Creature _target;
	
	protected Future<?> _geoCheckTask;
	
	protected int _skillCastRange;
	protected int _fusionId;
	protected int _fusionLevel;
	
	public FusionSkill(Creature caster, Creature target, L2Skill skill)
	{
		_skillCastRange = skill.getCastRange();
		_caster = caster;
		_target = target;
		_fusionId = skill.getTriggeredId();
		_fusionLevel = skill.getTriggeredLevel();
		
		final AbstractEffect effect = _target.getFirstEffect(_fusionId);
		if (effect != null)
			((EffectFusion) effect).increaseEffect();
		else
		{
			final L2Skill force = SkillTable.getInstance().getInfo(_fusionId, _fusionLevel);
			if (force != null)
				force.getEffects(_caster, _target);
		}
		
		_geoCheckTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			if (!MathUtil.checkIfInRange(_skillCastRange, _caster, _target, true) || !GeoEngine.getInstance().canSeeTarget(_caster, _target))
				_caster.getCast().stop();
		}, 1000, 1000);
	}
	
	public Creature getCaster()
	{
		return _caster;
	}
	
	public Creature getTarget()
	{
		return _target;
	}
	
	public void onCastAbort()
	{
		_caster.setFusionSkill(null);
		
		final AbstractEffect effect = _target.getFirstEffect(_fusionId);
		if (effect != null)
			((EffectFusion) effect).decreaseForce();
		
		_geoCheckTask.cancel(true);
		
		_caster.getAI().notifyEvent(AiEventType.FINISHED_CASTING, null, null);
	}
}
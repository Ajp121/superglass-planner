package com.superglassplanner;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.superglassplanner.SuperglassConstants.*;

/**
 * Tracks Superglass Make session statistics.
 */
@Slf4j
@Singleton
public class SessionTracker
{
	@Inject
	private Client client;

	@Getter
	private int castCount;

	@Getter
	private int craftingXpGained;

	@Getter
	private int magicXpGained;

	@Getter
	private int glassProduced;

	private int lastCraftingXp = -1;
	private int lastMagicXp = -1;

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		if (event.getActor().getAnimation() == SUPERGLASS_MAKE_ANIMATION)
		{
			castCount++;
			log.debug("Superglass Make cast detected! Total casts: {}", castCount);
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.CRAFTING)
		{
			int currentXp = event.getXp();
			if (lastCraftingXp >= 0)
			{
				int gained = currentXp - lastCraftingXp;
				if (gained > 0)
				{
					craftingXpGained += gained;
				}
			}
			lastCraftingXp = currentXp;
		}
		else if (event.getSkill() == Skill.MAGIC)
		{
			int currentXp = event.getXp();
			if (lastMagicXp >= 0)
			{
				int gained = currentXp - lastMagicXp;
				if (gained > 0)
				{
					magicXpGained += gained;
				}
			}
			lastMagicXp = currentXp;
		}
	}

	/**
	 * Estimates glass produced based on cast count.
	 */
	public int estimatedGlassProduced(boolean pickupExtra)
	{
		double glassPerCast = pickupExtra ? AVG_TOTAL_GLASS_PER_CAST : AVG_GLASS_NO_PICKUP;
		return (int) (castCount * glassPerCast);
	}

	public void reset()
	{
		castCount = 0;
		craftingXpGained = 0;
		magicXpGained = 0;
		glassProduced = 0;
		lastCraftingXp = -1;
		lastMagicXp = -1;
	}
}

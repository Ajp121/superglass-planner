package com.superglassplanner;

import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.superglassplanner.SuperglassConstants.*;

/**
 * Calculates materials needed to reach a target Crafting level via Superglass Make.
 */
@Singleton
public class GoalCalculator
{
	@Inject
	private Client client;

	@Inject
	private SuperglassPlannerConfig config;

	@Inject
	private BankScanner bankScanner;

	/**
	 * XP remaining to reach target level.
	 */
	public int xpRemaining()
	{
		int currentXp = client.getSkillExperience(Skill.CRAFTING);
		int targetXp = xpForLevel(config.targetLevel());
		return Math.max(0, targetXp - currentXp);
	}

	/**
	 * Molten glass needed to reach target level (blown into unpowered orbs).
	 */
	public int glassNeeded()
	{
		return (int) Math.ceil(xpRemaining() / CRAFTING_XP_PER_GLASS_BLOW);
	}

	/**
	 * Superglass Make casts needed.
	 */
	public int castsNeeded()
	{
		double glassPerCast = config.pickupExtraGlass() ? AVG_TOTAL_GLASS_PER_CAST : AVG_GLASS_NO_PICKUP;
		return (int) Math.ceil(glassNeeded() / glassPerCast);
	}

	/**
	 * Giant seaweed needed.
	 */
	public int seaweedNeeded()
	{
		return castsNeeded() * GIANT_SEAWEED_PER_CAST;
	}

	/**
	 * Buckets of sand needed.
	 */
	public int sandNeeded()
	{
		return castsNeeded() * SAND_PER_CAST;
	}

	/**
	 * Seaweed still needed after accounting for bank.
	 */
	public int seaweedDeficit()
	{
		return Math.max(0, seaweedNeeded() - bankScanner.getGiantSeaweedCount());
	}

	/**
	 * Sand still needed after accounting for bank.
	 */
	public int sandDeficit()
	{
		return Math.max(0, sandNeeded() - bankScanner.getBucketOfSandCount());
	}

	/**
	 * Progress percentage toward goal (0.0 to 1.0).
	 */
	public double progress()
	{
		int currentXp = client.getSkillExperience(Skill.CRAFTING);
		int currentLevelXp = xpForLevel(client.getRealSkillLevel(Skill.CRAFTING));
		int targetXp = xpForLevel(config.targetLevel());

		if (targetXp <= currentLevelXp)
		{
			return 1.0;
		}

		return (double) (currentXp - currentLevelXp) / (targetXp - currentLevelXp);
	}
}

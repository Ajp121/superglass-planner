package com.superglassplanner;

import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.superglassplanner.SuperglassConstants.*;

/**
 * Calculates materials needed to reach a crafting goal via Superglass Make.
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
	 * XP remaining to reach goal.
	 */
	public int xpRemaining()
	{
		int currentXp = client.getSkillExperience(Skill.CRAFTING);
		int targetXp;

		switch (config.goalType())
		{
			case TARGET_LEVEL:
				targetXp = xpForLevel(config.targetLevel());
				break;
			case TARGET_XP:
				targetXp = config.targetXp();
				break;
			case TARGET_GLASS:
				// XP from blowing the target amount of glass
				targetXp = currentXp + (int) (config.targetGlass() * config.glassItem().getXpPerGlass());
				break;
			default:
				targetXp = xpForLevel(99);
		}

		return Math.max(0, targetXp - currentXp);
	}

	/**
	 * Molten glass needed to reach goal.
	 */
	public int glassNeeded()
	{
		if (config.goalType() == GoalType.TARGET_GLASS)
		{
			return config.targetGlass();
		}
		return (int) Math.ceil(xpRemaining() / config.glassItem().getXpPerGlass());
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

		switch (config.goalType())
		{
			case TARGET_LEVEL:
			{
				int startXp = xpForLevel(client.getRealSkillLevel(Skill.CRAFTING));
				int targetXp = xpForLevel(config.targetLevel());
				if (targetXp <= startXp) return 1.0;
				return (double) (currentXp - startXp) / (targetXp - startXp);
			}
			case TARGET_XP:
			{
				int startXp = xpForLevel(client.getRealSkillLevel(Skill.CRAFTING));
				int targetXp = config.targetXp();
				if (targetXp <= currentXp) return 1.0;
				return (double) (currentXp - startXp) / (targetXp - startXp);
			}
			case TARGET_GLASS:
			default:
				return 0.0; // Can't easily track glass progress without session data
		}
	}

	/**
	 * Human-readable goal description for the panel header.
	 */
	public String goalDescription()
	{
		switch (config.goalType())
		{
			case TARGET_LEVEL:
				return "Level " + config.targetLevel();
			case TARGET_XP:
				return config.targetXp() + " XP";
			case TARGET_GLASS:
				return config.targetGlass() + " glass";
			default:
				return "Goal";
		}
	}
}

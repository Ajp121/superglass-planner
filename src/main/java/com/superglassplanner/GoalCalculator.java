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
	 * Glass per cast based on config: when picking up extra glass, uses the
	 * statistical average (28.8); otherwise, uses the user-configured value
	 * which depends on free inventory slots.
	 */
	public double glassPerCast()
	{
		return config.pickupExtraGlass() ? AVG_TOTAL_GLASS_PER_CAST : Math.max(1, config.glassPerCast());
	}

	/**
	 * The glass item to use for calculations at the player's current level.
	 * Resolves BEST_AVAILABLE to the actual best item.
	 */
	public GlassItem currentGlassItem()
	{
		GlassItem item = config.glassItem();
		if (item == GlassItem.BEST_AVAILABLE)
		{
			return GlassItem.bestItemForLevel(client.getRealSkillLevel(Skill.CRAFTING));
		}
		return item;
	}

	private double makeXpPerGlass()
	{
		return CRAFTING_XP_PER_CAST / glassPerCast();
	}

	/**
	 * Effective crafting XP per glass, including both Superglass Make cast XP
	 * and the XP from blowing the glass item. Uses current level's best item
	 * when BEST_AVAILABLE is selected.
	 */
	public double effectiveXpPerGlass()
	{
		return currentGlassItem().getXpPerGlass() + makeXpPerGlass();
	}

	private double effectiveXpPerGlass(GlassItem item)
	{
		return item.getXpPerGlass() + makeXpPerGlass();
	}

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
				targetXp = currentXp + (int) (config.targetGlass() * effectiveXpPerGlass());
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
			int needed = config.targetGlass();
			if (config.factorExistingGlass())
			{
				needed = Math.max(0, needed - bankScanner.totalMoltenGlass());
			}
			return needed;
		}

		int remaining = xpRemaining();

		if (config.factorExistingGlass())
		{
			int existingGlass = bankScanner.totalMoltenGlass();
			double xpFromExisting = existingGlass * currentGlassItem().getXpPerGlass();
			remaining = Math.max(0, (int) (remaining - xpFromExisting));
		}

		if (config.glassItem() == GlassItem.BEST_AVAILABLE)
		{
			return glassNeededBestAvailable(remaining);
		}

		return (int) Math.ceil(remaining / effectiveXpPerGlass());
	}

	/**
	 * Walks level brackets to calculate glass needed when making the best
	 * available item at each crafting level.
	 */
	private int glassNeededBestAvailable(int xpRemaining)
	{
		if (xpRemaining <= 0) return 0;

		double xpLeft = xpRemaining;
		double currentXp = client.getSkillExperience(Skill.CRAFTING);
		double mxpg = makeXpPerGlass();
		int totalGlass = 0;

		while (xpLeft > 0)
		{
			int level = levelAtXp((int) currentXp);
			GlassItem best = GlassItem.bestItemForLevel(level);
			double effXp = best.getXpPerGlass() + mxpg;

			// Find the XP threshold where the next better item unlocks
			double bracketCeiling = xpLeft;
			for (GlassItem candidate : GlassItem.values())
			{
				if (candidate == GlassItem.BEST_AVAILABLE) continue;
				if (candidate.getLevelRequired() > level)
				{
					double xpToUnlock = xpForLevel(candidate.getLevelRequired()) - currentXp;
					if (xpToUnlock > 0 && xpToUnlock < bracketCeiling)
					{
						bracketCeiling = xpToUnlock;
					}
				}
			}

			int glassInBracket = (int) Math.ceil(bracketCeiling / effXp);
			double xpFromBracket = glassInBracket * effXp;
			totalGlass += glassInBracket;
			xpLeft -= xpFromBracket;
			currentXp += xpFromBracket;
		}

		return totalGlass;
	}

	/**
	 * Returns the crafting level at a given XP amount.
	 */
	private int levelAtXp(int xp)
	{
		for (int level = 99; level >= 1; level--)
		{
			if (xp >= xpForLevel(level))
			{
				return level;
			}
		}
		return 1;
	}

	/**
	 * Superglass Make casts needed.
	 */
	public int castsNeeded()
	{
		return (int) Math.ceil(glassNeeded() / glassPerCast());
	}

	/**
	 * Items to blow to reach goal using only blow XP (for overlay during active blowing).
	 */
	public int itemsToBlowForGoal()
	{
		double xpPerItem = currentGlassItem().getXpPerGlass();
		if (xpPerItem <= 0) return 0;
		return (int) Math.ceil(xpRemaining() / xpPerItem);
	}

	/**
	 * Items to blow to reach the next crafting level using only blow XP.
	 */
	public int itemsToBlowForLevel()
	{
		double xpPerItem = currentGlassItem().getXpPerGlass();
		if (xpPerItem <= 0) return 0;
		int currentLevel = client.getRealSkillLevel(Skill.CRAFTING);
		if (currentLevel >= 99) return 0;
		int currentXp = client.getSkillExperience(Skill.CRAFTING);
		int nextLevelXp = xpForLevel(currentLevel + 1);
		int remaining = Math.max(0, nextLevelXp - currentXp);
		return (int) Math.ceil(remaining / xpPerItem);
	}

	/**
	 * Total items to blow to reach goal, factoring in Superglass Make XP.
	 * When "factor existing glass" is on, includes existing glass that still needs blowing.
	 */
	public int totalItemsToBlow()
	{
		if (config.factorExistingGlass())
		{
			int existingGlass = bankScanner.totalMoltenGlass();
			return glassNeeded() + existingGlass;
		}
		return glassNeeded();
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
		return Math.max(0, seaweedNeeded() - bankScanner.totalGiantSeaweed());
	}

	/**
	 * Sand still needed after accounting for bank.
	 */
	public int sandDeficit()
	{
		return Math.max(0, sandNeeded() - bankScanner.totalBucketOfSand());
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
				return 0.0;
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

package com.superglassplanner;

/**
 * OSRS item IDs and constants used by the Superglass Planner plugin.
 */
public final class SuperglassConstants
{
	private SuperglassConstants() {}

	// Item IDs
	public static final int GIANT_SEAWEED = 21504;
	public static final int BUCKET_OF_SAND = 1783;
	public static final int SODA_ASH = 1781;
	public static final int MOLTEN_GLASS = 1775;
	public static final int SEAWEED = 401;
	public static final int ASTRAL_RUNE = 9075;
	public static final int FIRE_RUNE = 554;
	public static final int AIR_RUNE = 556;

	// Superglass Make produces ~1.3x molten glass (30% bonus yield)
	// With 3 giant seaweed (18 regular equivalent) + 18 sand = 18 base glass
	// Bonus yield: ~10.8 extra on average, for ~28.8 total per cast (pickup method)
	public static final int SAND_PER_CAST = 18;
	public static final int GIANT_SEAWEED_PER_CAST = 3;
	public static final int ASTRAL_RUNES_PER_CAST = 2;
	public static final double BASE_GLASS_PER_CAST = 18.0;
	public static final double AVG_BONUS_GLASS_PER_CAST = 10.8;
	public static final double AVG_TOTAL_GLASS_PER_CAST = BASE_GLASS_PER_CAST + AVG_BONUS_GLASS_PER_CAST;
	public static final double AVG_GLASS_NO_PICKUP = BASE_GLASS_PER_CAST;

	// XP values
	public static final double CRAFTING_XP_PER_GLASS_BLOW = 52.5; // Unpowered orb (most common ironman method)
	public static final double MAGIC_XP_PER_CAST = 78.0;
	public static final double CRAFTING_XP_PER_CAST = 180.0; // From the spell itself

	// Bert daily sand
	public static final int BERT_DAILY_SAND = 84;

	// Seaweed farm cycle (40 minutes in milliseconds)
	public static final long SEAWEED_GROWTH_TIME_MS = 40 * 60 * 1000L;

	// Animation ID for Superglass Make
	public static final int SUPERGLASS_MAKE_ANIMATION = 4413;

	// Crafting XP table (level -> total XP)
	private static final int[] XP_TABLE = new int[100];

	static
	{
		int points = 0;
		for (int lvl = 1; lvl < 100; lvl++)
		{
			points += (int) Math.floor(lvl + 300 * Math.pow(2, lvl / 7.0));
			XP_TABLE[lvl] = points / 4;
		}
	}

	public static int xpForLevel(int level)
	{
		if (level < 1) return 0;
		if (level > 99) level = 99;
		return XP_TABLE[level - 1];
	}
}

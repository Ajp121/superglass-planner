package com.superglassplanner;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Glass items that can be blown from molten glass, with their Crafting XP and level requirements.
 */
@Getter
@RequiredArgsConstructor
public enum GlassItem
{
	BEST_AVAILABLE("Best available", 0, 0),
	BEER_GLASS("Beer glass", 17.5, 1),
	CANDLE_LANTERN("Empty candle lantern", 19.0, 4),
	OIL_LAMP("Empty oil lamp", 25.0, 12),
	VIAL("Vial", 35.0, 33),
	FISHBOWL("Empty fishbowl", 42.5, 42),
	UNPOWERED_ORB("Unpowered orb", 52.5, 46),
	LANTERN_LENS("Lantern lens", 55.0, 49),
	LIGHT_ORB("Light orb", 70.0, 87);

	private final String displayName;
	private final double xpPerGlass;
	private final int levelRequired;

	/**
	 * Returns the highest-XP glass item available at the given crafting level.
	 */
	public static GlassItem bestItemForLevel(int level)
	{
		GlassItem best = BEER_GLASS;
		for (GlassItem item : values())
		{
			if (item == BEST_AVAILABLE) continue;
			if (item.levelRequired <= level && item.xpPerGlass > best.xpPerGlass)
			{
				best = item;
			}
		}
		return best;
	}

	@Override
	public String toString()
	{
		if (this == BEST_AVAILABLE) return displayName;
		return displayName + " (" + xpPerGlass + " xp)";
	}
}

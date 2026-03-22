package com.superglassplanner;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("superglassplanner")
public interface SuperglassPlannerConfig extends Config
{
	@ConfigSection(
		name = "Superglass Make Overlay",
		description = "Overlay display settings",
		position = 0
	)
	String overlaySection = "overlay";

	// Goal settings — controlled from the side panel, hidden from config page

	@ConfigItem(
		keyName = "goalType",
		name = "Goal Type",
		description = "What kind of goal to track",
		hidden = true
	)
	default GoalType goalType()
	{
		return GoalType.TARGET_LEVEL;
	}

	@ConfigItem(
		keyName = "targetLevel",
		name = "Target Crafting Level",
		description = "The Crafting level you are working towards",
		hidden = true
	)
	default int targetLevel()
	{
		return 99;
	}

	@ConfigItem(
		keyName = "targetXp",
		name = "Target XP",
		description = "The total Crafting XP you are working towards",
		hidden = true
	)
	default int targetXp()
	{
		return 13034431;
	}

	@ConfigItem(
		keyName = "targetGlass",
		name = "Target Glass Amount",
		description = "How many molten glass you want to produce",
		hidden = true
	)
	default int targetGlass()
	{
		return 10000;
	}

	@ConfigItem(
		keyName = "glassItem",
		name = "Glass Item to Blow",
		description = "Which item you plan to blow from the molten glass",
		hidden = true
	)
	default GlassItem glassItem()
	{
		return GlassItem.UNPOWERED_ORB;
	}

	@ConfigItem(
		keyName = "pickupExtraGlass",
		name = "Pickup Extra Glass",
		description = "Whether you pick up bonus molten glass from the floor after casting",
		hidden = true
	)
	default boolean pickupExtraGlass()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glassPerCast",
		name = "Inventory Space",
		description = "Free inventory slots for glass after casting Superglass Make",
		hidden = true
	)
	default int glassPerCast()
	{
		return 26;
	}

	@ConfigItem(
		keyName = "factorExistingGlass",
		name = "Factor Existing Glass",
		description = "Subtract molten glass already in bank/looting bag from glass needed calculations",
		hidden = true
	)
	default boolean factorExistingGlass()
	{
		return true;
	}

	// Overlay — visible on config page

	@ConfigItem(
		keyName = "resetOnLogout",
		name = "Reset Session on Logout",
		description = "Clear all session stats when you log out",
		position = -1
	)
	default boolean resetOnLogout()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Display a compact overlay with key stats near the inventory",
		section = overlaySection,
		position = 0
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayCastsToGoal",
		name = "Show Casts to Goal",
		description = "Show casts remaining to reach goal instead of total available casts",
		section = overlaySection,
		position = 1
	)
	default boolean overlayCastsToGoal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "overlayTimeout",
		name = "Overlay Timeout (minutes)",
		description = "Hide overlay after this many minutes of no casts, 0 = never hide",
		section = overlaySection,
		position = 2
	)
	default int overlayTimeout()
	{
		return 2;
	}

	// Glassblowing overlay

	@ConfigSection(
		name = "Glassblowing Overlay",
		description = "Overlay display settings for blowing molten glass",
		position = 1
	)
	String glassblowingSection = "glassblowing";

	@ConfigItem(
		keyName = "showGlassblowingOverlay",
		name = "Show Overlay",
		description = "Display a compact overlay while blowing molten glass",
		section = glassblowingSection,
		position = 0
	)
	default boolean showGlassblowingOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "glassblowingItemsToGoal",
		name = "Show Items to Goal",
		description = "Show items to goal instead of items to next level",
		section = glassblowingSection,
		position = 1
	)
	default boolean glassblowingItemsToGoal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "glassblowingFactorMakeXp",
		name = "Factor Make XP",
		description = "Factor Superglass Make XP into items remaining (only applies in goal mode)",
		section = glassblowingSection,
		position = 2
	)
	default boolean glassblowingFactorMakeXp()
	{
		return false;
	}

	@ConfigItem(
		keyName = "glassblowingTimeout",
		name = "Overlay Timeout (minutes)",
		description = "Hide overlay after this many minutes of no glassblowing, 0 = never hide",
		section = glassblowingSection,
		position = 3
	)
	default int glassblowingTimeout()
	{
		return 2;
	}
}

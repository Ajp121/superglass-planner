package com.superglassplanner;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("superglassplanner")
public interface SuperglassPlannerConfig extends Config
{
	@ConfigSection(
		name = "Goals",
		description = "Crafting goal settings",
		position = 0
	)
	String goalsSection = "goals";

	@ConfigSection(
		name = "Reminders",
		description = "Daily and farming reminders",
		position = 1
	)
	String remindersSection = "reminders";

	@ConfigItem(
		keyName = "goalType",
		name = "Goal Type",
		description = "What kind of goal to track",
		section = goalsSection,
		position = 0
	)
	default GoalType goalType()
	{
		return GoalType.TARGET_LEVEL;
	}

	@ConfigItem(
		keyName = "targetLevel",
		name = "Target Crafting Level",
		description = "The Crafting level you are working towards (used when Goal Type is Target Level)",
		section = goalsSection,
		position = 1
	)
	default int targetLevel()
	{
		return 99;
	}

	@ConfigItem(
		keyName = "targetXp",
		name = "Target XP",
		description = "The total Crafting XP you are working towards (used when Goal Type is Target XP)",
		section = goalsSection,
		position = 2
	)
	default int targetXp()
	{
		return 13034431;
	}

	@ConfigItem(
		keyName = "targetGlass",
		name = "Target Glass Amount",
		description = "How many molten glass you want to produce (used when Goal Type is Target Glass)",
		section = goalsSection,
		position = 3
	)
	default int targetGlass()
	{
		return 10000;
	}

	@ConfigItem(
		keyName = "glassItem",
		name = "Glass Item to Blow",
		description = "Which item you plan to blow from the molten glass (affects XP calculations)",
		section = goalsSection,
		position = 4
	)
	default GlassItem glassItem()
	{
		return GlassItem.UNPOWERED_ORB;
	}

	@ConfigItem(
		keyName = "pickupExtraGlass",
		name = "Pickup Extra Glass",
		description = "Whether you pick up bonus molten glass from the floor after casting",
		section = goalsSection,
		position = 5
	)
	default boolean pickupExtraGlass()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bertReminder",
		name = "Bert Daily Sand Reminder",
		description = "Remind you to collect 84 daily sand from Bert (Hand in the Sand quest)",
		section = remindersSection,
		position = 0
	)
	default boolean bertReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "seaweedFarmReminder",
		name = "Seaweed Farm Reminder",
		description = "Remind you when underwater seaweed patches are ready to harvest",
		section = remindersSection,
		position = 1
	)
	default boolean seaweedFarmReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Display a compact overlay with key stats near the inventory",
		position = 2
	)
	default boolean showOverlay()
	{
		return true;
	}
}

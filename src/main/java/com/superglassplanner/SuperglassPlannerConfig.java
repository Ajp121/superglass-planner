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
		keyName = "targetLevel",
		name = "Target Crafting Level",
		description = "The Crafting level you are working towards",
		section = goalsSection,
		position = 0
	)
	default int targetLevel()
	{
		return 99;
	}

	@ConfigItem(
		keyName = "pickupExtraGlass",
		name = "Pickup Extra Glass",
		description = "Whether you pick up bonus molten glass from the floor after casting",
		section = goalsSection,
		position = 1
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

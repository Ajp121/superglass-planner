package com.superglassplanner;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;

/**
 * Compact overlay showing glassblowing session stats.
 */
public class GlassblowingOverlay extends OverlayPanel
{
	private final SuperglassPlannerConfig config;
	private final GlassblowingTracker tracker;
	private final GoalCalculator goalCalculator;

	private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance();

	@Inject
	public GlassblowingOverlay(
		SuperglassPlannerConfig config,
		GlassblowingTracker tracker,
		GoalCalculator goalCalculator)
	{
		this.config = config;
		this.tracker = tracker;
		this.goalCalculator = goalCalculator;

		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showGlassblowingOverlay() || tracker.getItemsBlown() == 0)
		{
			return null;
		}

		int timeoutMinutes = config.glassblowingTimeout();
		if (timeoutMinutes > 0 && tracker.getLastBlowMillis() > 0)
		{
			long elapsed = System.currentTimeMillis() - tracker.getLastBlowMillis();
			if (elapsed > timeoutMinutes * 60_000L)
			{
				return null;
			}
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Glassblowing")
			.color(new Color(255, 152, 0))
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Items Blown:")
			.right(FORMAT.format(tracker.getItemsBlown()))
			.build());

		if (config.glassblowingItemsToGoal())
		{
			int toGoal = config.glassblowingFactorMakeXp()
				? goalCalculator.totalItemsToBlow()
				: goalCalculator.itemsToBlowForGoal();
			if (toGoal > 0)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Items to Goal:")
					.right(FORMAT.format(toGoal))
					.build());
			}
		}
		else
		{
			int toLevel = goalCalculator.itemsToBlowForLevel();
			if (toLevel > 0)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Items to Level:")
					.right(FORMAT.format(toLevel))
					.build());
			}
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Crafting XP:")
			.right(FORMAT.format(tracker.getCraftingXpGained()))
			.build());

		return super.render(graphics);
	}
}

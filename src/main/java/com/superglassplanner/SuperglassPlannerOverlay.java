package com.superglassplanner;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;

/**
 * Compact overlay showing key Superglass Planner stats.
 */
public class SuperglassPlannerOverlay extends OverlayPanel
{
	private final Client client;
	private final SuperglassPlannerConfig config;
	private final BankScanner bankScanner;
	private final SessionTracker sessionTracker;
	private final GoalCalculator goalCalculator;

	private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance();

	@Inject
	public SuperglassPlannerOverlay(
		Client client,
		SuperglassPlannerConfig config,
		BankScanner bankScanner,
		SessionTracker sessionTracker,
		GoalCalculator goalCalculator)
	{
		this.client = client;
		this.config = config;
		this.bankScanner = bankScanner;
		this.sessionTracker = sessionTracker;
		this.goalCalculator = goalCalculator;

		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay() || sessionTracker.getCastCount() == 0)
		{
			return null;
		}

		int timeoutMinutes = config.overlayTimeout();
		if (timeoutMinutes > 0 && sessionTracker.getLastCastMillis() > 0)
		{
			long elapsed = System.currentTimeMillis() - sessionTracker.getLastCastMillis();
			if (elapsed > timeoutMinutes * 60_000L)
			{
				return null;
			}
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Superglass")
			.color(new Color(255, 152, 0))
			.build());

		if (config.overlayCastsToGoal())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Casts to Goal:")
				.right(FORMAT.format(goalCalculator.castsNeeded()))
				.build());
		}
		else if (bankScanner.isBankLoaded())
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Casts Left:")
				.right(FORMAT.format(bankScanner.possibleCasts()))
				.build());
		}

		if (sessionTracker.getCastCount() > 0)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Session Casts:")
				.right(FORMAT.format(sessionTracker.getCastCount()))
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Glass Made:")
				.right(FORMAT.format(sessionTracker.getGlassProduced()))
				.build());
		}

		return super.render(graphics);
	}
}

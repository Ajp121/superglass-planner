package com.superglassplanner;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Superglass Planner",
	description = "Tracks and optimizes the Superglass Make crafting grind for ironmen",
	tags = {"ironman", "crafting", "superglass", "molten glass", "seaweed", "lunar"}
)
public class SuperglassPlannerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SuperglassPlannerConfig config;

	@Inject
	private EventBus eventBus;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BankScanner bankScanner;

	@Inject
	private SessionTracker sessionTracker;

	@Inject
	private GoalCalculator goalCalculator;

	@Inject
	private SuperglassPlannerOverlay overlay;

	@Inject
	private GlassblowingTracker glassblowingTracker;

	@Inject
	private GlassblowingOverlay glassblowingOverlay;

	@Inject
	private SuperglassPlannerPanel panel;

	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Superglass Planner started!");

		eventBus.register(bankScanner);
		eventBus.register(sessionTracker);
		eventBus.register(glassblowingTracker);

		overlayManager.add(overlay);
		overlayManager.add(glassblowingOverlay);

		navButton = NavigationButton.builder()
			.tooltip("Superglass Planner")
			.icon(getIcon())
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Superglass Planner stopped!");

		eventBus.unregister(bankScanner);
		eventBus.unregister(sessionTracker);
		eventBus.unregister(glassblowingTracker);

		overlayManager.remove(overlay);
		overlayManager.remove(glassblowingOverlay);
		clientToolbar.removeNavigation(navButton);

		bankScanner.reset();
		sessionTracker.reset();
		glassblowingTracker.reset();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			bankScanner.reset();
			sessionTracker.reset();
			glassblowingTracker.reset();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (panel.isActive())
		{
			panel.update();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (panel.isActive())
		{
			panel.update();
		}
	}

	private BufferedImage getIcon()
	{
		try
		{
			return ImageUtil.loadImageResource(getClass(), "/com/superglassplanner/icon.png");
		}
		catch (Exception e)
		{
			// Return a simple default icon if resource not found
			BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = img.createGraphics();
			g.setColor(new java.awt.Color(255, 152, 0));
			g.fillOval(2, 2, 12, 12);
			g.dispose();
			return img;
		}
	}

	@Provides
	SuperglassPlannerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SuperglassPlannerConfig.class);
	}
}

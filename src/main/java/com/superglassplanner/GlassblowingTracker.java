package com.superglassplanner;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.superglassplanner.SuperglassConstants.*;

/**
 * Tracks glassblowing session statistics (blowing molten glass into items).
 */
@Slf4j
@Singleton
public class GlassblowingTracker
{
	@Inject
	private Client client;

	@Getter
	private int itemsBlown;

	@Getter
	private long lastBlowMillis;

	@Getter
	private int craftingXpGained;

	private int lastCraftingXp = -1;
	private int lastInventoryGlassCount = -1;
	private boolean blowing = false;

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		if (event.getActor().getAnimation() == GLASSBLOWING_ANIMATION)
		{
			blowing = true;
			lastBlowMillis = System.currentTimeMillis();
		}
		else
		{
			blowing = false;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INVENTORY.getId())
		{
			return;
		}

		int currentGlass = countItem(event.getItemContainer(), MOLTEN_GLASS);
		if (blowing && lastInventoryGlassCount >= 0)
		{
			int delta = lastInventoryGlassCount - currentGlass;
			if (delta > 0)
			{
				itemsBlown += delta;
				log.debug("Glass blown: +{}, total items: {}", delta, itemsBlown);
			}
		}
		lastInventoryGlassCount = currentGlass;
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() != Skill.CRAFTING)
		{
			return;
		}

		int currentXp = event.getXp();
		if (lastCraftingXp >= 0 && itemsBlown > 0)
		{
			int gained = currentXp - lastCraftingXp;
			if (gained > 0)
			{
				craftingXpGained += gained;
			}
		}
		lastCraftingXp = currentXp;
	}

	private int countItem(ItemContainer container, int itemId)
	{
		int count = 0;
		if (container == null)
		{
			return count;
		}
		for (Item item : container.getItems())
		{
			if (item != null && item.getId() == itemId)
			{
				count += item.getQuantity();
			}
		}
		return count;
	}

	public int getGlassRemaining()
	{
		ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
		return inv != null ? countItem(inv, MOLTEN_GLASS) : 0;
	}

	/**
	 * Resets only transient detection state so tracking resumes
	 * correctly after logout without losing accumulated stats.
	 */
	public void softReset()
	{
		lastCraftingXp = -1;
		lastInventoryGlassCount = -1;
		blowing = false;
	}

	public void reset()
	{
		itemsBlown = 0;
		lastBlowMillis = 0;
		craftingXpGained = 0;
		lastCraftingXp = -1;
		lastInventoryGlassCount = -1;
		blowing = false;
	}
}

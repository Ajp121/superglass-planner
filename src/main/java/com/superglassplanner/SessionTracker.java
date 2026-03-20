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
 * Tracks Superglass Make session statistics.
 */
@Slf4j
@Singleton
public class SessionTracker
{
	@Inject
	private Client client;

	@Getter
	private int castCount;

	@Getter
	private long lastCastMillis;

	@Getter
	private int craftingXpGained;

	@Getter
	private int magicXpGained;

	@Getter
	private int glassProduced;

	private static final int LOOTING_BAG_CONTAINER_ID = 516;

	private int lastCraftingXp = -1;
	private int lastMagicXp = -1;
	private int lastInventoryGlassCount = -1;
	private int lastLootingBagGlassCount = -1;

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		if (event.getActor().getAnimation() == SUPERGLASS_MAKE_ANIMATION)
		{
			castCount++;
			lastCastMillis = System.currentTimeMillis();
			log.debug("Superglass Make cast detected! Total casts: {}", castCount);
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();

		if (containerId == InventoryID.INVENTORY.getId())
		{
			int currentGlass = countItem(event.getItemContainer(), MOLTEN_GLASS);
			if (lastInventoryGlassCount >= 0)
			{
				int delta = currentGlass - lastInventoryGlassCount;
				if (delta > 0)
				{
					glassProduced += delta;
					log.debug("Glass to inventory: +{}, total produced: {}", delta, glassProduced);
				}
			}
			lastInventoryGlassCount = currentGlass;
		}
		else if (containerId == LOOTING_BAG_CONTAINER_ID)
		{
			int currentGlass = countItem(event.getItemContainer(), MOLTEN_GLASS);
			if (lastLootingBagGlassCount >= 0)
			{
				int delta = currentGlass - lastLootingBagGlassCount;
				if (delta > 0)
				{
					glassProduced += delta;
					log.debug("Glass to looting bag: +{}, total produced: {}", delta, glassProduced);
				}
			}
			lastLootingBagGlassCount = currentGlass;
		}
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

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (event.getSkill() == Skill.CRAFTING)
		{
			int currentXp = event.getXp();
			if (lastCraftingXp >= 0)
			{
				int gained = currentXp - lastCraftingXp;
				if (gained > 0)
				{
					craftingXpGained += gained;
				}
			}
			lastCraftingXp = currentXp;
		}
		else if (event.getSkill() == Skill.MAGIC)
		{
			int currentXp = event.getXp();
			if (lastMagicXp >= 0)
			{
				int gained = currentXp - lastMagicXp;
				if (gained > 0)
				{
					magicXpGained += gained;
				}
			}
			lastMagicXp = currentXp;
		}
	}

	public void reset()
	{
		castCount = 0;
		lastCastMillis = 0;
		craftingXpGained = 0;
		magicXpGained = 0;
		glassProduced = 0;
		lastCraftingXp = -1;
		lastMagicXp = -1;
		lastInventoryGlassCount = -1;
		lastLootingBagGlassCount = -1;
	}
}

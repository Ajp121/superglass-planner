package com.superglassplanner;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.superglassplanner.SuperglassConstants.*;

/**
 * Scans the player's bank for Superglass Make materials.
 */
@Slf4j
@Singleton
public class BankScanner
{
	@Inject
	private Client client;

	@Getter
	private int giantSeaweedCount;

	@Getter
	private int bucketOfSandCount;

	@Getter
	private int sodaAshCount;

	@Getter
	private int moltenGlassCount;

	@Getter
	private int astralRuneCount;

	@Getter
	private boolean bankLoaded;

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();

		if (containerId == InventoryID.BANK.getId())
		{
			updateBankCounts(event.getItemContainer());
		}
	}

	private void updateBankCounts(ItemContainer bank)
	{
		if (bank == null)
		{
			return;
		}

		giantSeaweedCount = 0;
		bucketOfSandCount = 0;
		sodaAshCount = 0;
		moltenGlassCount = 0;
		astralRuneCount = 0;

		for (Item item : bank.getItems())
		{
			if (item == null)
			{
				continue;
			}

			switch (item.getId())
			{
				case GIANT_SEAWEED:
					giantSeaweedCount += item.getQuantity();
					break;
				case BUCKET_OF_SAND:
					bucketOfSandCount += item.getQuantity();
					break;
				case SODA_ASH:
					sodaAshCount += item.getQuantity();
					break;
				case MOLTEN_GLASS:
					moltenGlassCount += item.getQuantity();
					break;
				case ASTRAL_RUNE:
					astralRuneCount += item.getQuantity();
					break;
			}
		}

		bankLoaded = true;
		log.debug("Bank scanned - Seaweed: {}, Sand: {}, Soda Ash: {}, Glass: {}, Astrals: {}",
			giantSeaweedCount, bucketOfSandCount, sodaAshCount, moltenGlassCount, astralRuneCount);
	}

	/**
	 * Returns how many Superglass Make casts are possible with current bank materials.
	 */
	public int possibleCasts()
	{
		int castsBySeaweed = giantSeaweedCount / GIANT_SEAWEED_PER_CAST;
		int castsBySand = bucketOfSandCount / SAND_PER_CAST;
		return Math.min(castsBySeaweed, castsBySand);
	}

	/**
	 * Returns estimated molten glass from current materials.
	 */
	public int estimatedGlass(boolean pickupExtra)
	{
		double glassPerCast = pickupExtra ? AVG_TOTAL_GLASS_PER_CAST : AVG_GLASS_NO_PICKUP;
		return (int) (possibleCasts() * glassPerCast);
	}

	public void reset()
	{
		giantSeaweedCount = 0;
		bucketOfSandCount = 0;
		sodaAshCount = 0;
		moltenGlassCount = 0;
		astralRuneCount = 0;
		bankLoaded = false;
	}
}

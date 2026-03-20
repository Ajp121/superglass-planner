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
 * Scans the player's bank and rune pouch for Superglass Make materials.
 */
@Slf4j
@Singleton
public class BankScanner
{
	// Rune pouch varbits (supports up to 6 slots with divine rune pouch)
	private static final int[] RUNE_POUCH_TYPE_VARBITS = {29, 1622, 1623, 4726, 4727, 4728};
	private static final int[] RUNE_POUCH_AMOUNT_VARBITS = {1624, 1625, 1626, 4729, 4730, 4731};
	// Rune pouch type ID for astral rune (from RuneLite's runepouch enum)
	private static final int RUNE_POUCH_ASTRAL_TYPE = 14;

	private static final int LOOTING_BAG_CONTAINER_ID = 516;

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
	private int runePouchAstralCount;

	@Getter
	private int lootingBagGlassCount;

	private int inventoryGlassCount;

	@Getter
	private boolean bankLoaded;

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();

		if (containerId == InventoryID.BANK.getId())
		{
			updateBankCounts(event.getItemContainer());
			updateRunePouchCounts();
		}
		else if (containerId == LOOTING_BAG_CONTAINER_ID)
		{
			updateLootingBagCounts(event.getItemContainer());
		}
		else if (containerId == InventoryID.INVENTORY.getId())
		{
			updateInventoryCounts(event.getItemContainer());
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

	private void updateLootingBagCounts(ItemContainer bag)
	{
		lootingBagGlassCount = 0;
		if (bag == null)
		{
			return;
		}
		for (Item item : bag.getItems())
		{
			if (item != null && item.getId() == MOLTEN_GLASS)
			{
				lootingBagGlassCount += item.getQuantity();
			}
		}
		log.debug("Looting bag glass: {}", lootingBagGlassCount);
	}

	private void updateInventoryCounts(ItemContainer inventory)
	{
		inventoryGlassCount = 0;
		if (inventory == null)
		{
			return;
		}
		for (Item item : inventory.getItems())
		{
			if (item != null && item.getId() == MOLTEN_GLASS)
			{
				inventoryGlassCount += item.getQuantity();
			}
		}
	}

	/**
	 * Total molten glass across bank + looting bag + inventory.
	 */
	public int totalMoltenGlass()
	{
		return moltenGlassCount + lootingBagGlassCount + inventoryGlassCount;
	}

	/**
	 * Reads astral rune count from the rune pouch via varbits.
	 */
	private void updateRunePouchCounts()
	{
		runePouchAstralCount = 0;

		for (int i = 0; i < RUNE_POUCH_TYPE_VARBITS.length; i++)
		{
			int runeType = client.getVarbitValue(RUNE_POUCH_TYPE_VARBITS[i]);
			int amount = client.getVarbitValue(RUNE_POUCH_AMOUNT_VARBITS[i]);

			if (runeType == RUNE_POUCH_ASTRAL_TYPE && amount > 0)
			{
				runePouchAstralCount += amount;
			}
		}

		log.debug("Rune pouch astrals: {}", runePouchAstralCount);
	}

	/**
	 * Total astral runes across bank + rune pouch.
	 */
	public int totalAstralRunes()
	{
		return astralRuneCount + runePouchAstralCount;
	}

	/**
	 * Returns how many Superglass Make casts are possible with current materials.
	 * Factors in giant seaweed, sand, AND astral runes (2 per cast).
	 */
	public int possibleCasts()
	{
		int castsBySeaweed = giantSeaweedCount / GIANT_SEAWEED_PER_CAST;
		int castsBySand = bucketOfSandCount / SAND_PER_CAST;
		int castsByAstrals = totalAstralRunes() / ASTRAL_RUNES_PER_CAST;
		return Math.min(castsBySeaweed, Math.min(castsBySand, castsByAstrals));
	}

	/**
	 * Returns estimated molten glass from current materials.
	 */
	public int estimatedGlass(boolean pickupExtra)
	{
		double glassPerCast = pickupExtra ? AVG_TOTAL_GLASS_PER_CAST : AVG_GLASS_NO_PICKUP;
		return (int) (possibleCasts() * glassPerCast);
	}

	/**
	 * Calculates the max casts if you had unlimited of this resource,
	 * i.e. the max casts based on the OTHER two resources.
	 */
	public int maxCastsIgnoringSeaweed()
	{
		int castsBySand = bucketOfSandCount / SAND_PER_CAST;
		int castsByAstrals = totalAstralRunes() / ASTRAL_RUNES_PER_CAST;
		return Math.min(castsBySand, castsByAstrals);
	}

	public int maxCastsIgnoringSand()
	{
		int castsBySeaweed = giantSeaweedCount / GIANT_SEAWEED_PER_CAST;
		int castsByAstrals = totalAstralRunes() / ASTRAL_RUNES_PER_CAST;
		return Math.min(castsBySeaweed, castsByAstrals);
	}

	public int maxCastsIgnoringAstrals()
	{
		int castsBySeaweed = giantSeaweedCount / GIANT_SEAWEED_PER_CAST;
		int castsBySand = bucketOfSandCount / SAND_PER_CAST;
		return Math.min(castsBySeaweed, castsBySand);
	}

	/**
	 * How much more seaweed you need to fully use your sand + astrals.
	 */
	public int seaweedDeficit()
	{
		int needed = maxCastsIgnoringSeaweed() * GIANT_SEAWEED_PER_CAST;
		return Math.max(0, needed - giantSeaweedCount);
	}

	/**
	 * How much more sand you need to fully use your seaweed + astrals.
	 */
	public int sandDeficit()
	{
		int needed = maxCastsIgnoringSand() * SAND_PER_CAST;
		return Math.max(0, needed - bucketOfSandCount);
	}

	/**
	 * How many more astrals you need to fully use your seaweed + sand.
	 */
	public int astralDeficit()
	{
		int needed = maxCastsIgnoringAstrals() * ASTRAL_RUNES_PER_CAST;
		return Math.max(0, needed - totalAstralRunes());
	}

	// Perspective-based: "to use all of X, how much more Y do I need?"

	public int castsFromSeaweed()
	{
		return giantSeaweedCount / GIANT_SEAWEED_PER_CAST;
	}

	public int castsFromSand()
	{
		return bucketOfSandCount / SAND_PER_CAST;
	}

	public int castsFromAstrals()
	{
		return totalAstralRunes() / ASTRAL_RUNES_PER_CAST;
	}

	public int sandNeededForCasts(int casts)
	{
		return Math.max(0, casts * SAND_PER_CAST - bucketOfSandCount);
	}

	public int seaweedNeededForCasts(int casts)
	{
		return Math.max(0, casts * GIANT_SEAWEED_PER_CAST - giantSeaweedCount);
	}

	public int astralsNeededForCasts(int casts)
	{
		return Math.max(0, casts * ASTRAL_RUNES_PER_CAST - totalAstralRunes());
	}

	public void reset()
	{
		giantSeaweedCount = 0;
		bucketOfSandCount = 0;
		sodaAshCount = 0;
		moltenGlassCount = 0;
		astralRuneCount = 0;
		runePouchAstralCount = 0;
		lootingBagGlassCount = 0;
		inventoryGlassCount = 0;
		bankLoaded = false;
	}
}

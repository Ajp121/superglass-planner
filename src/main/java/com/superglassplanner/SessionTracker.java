package com.superglassplanner;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.events.AnimationChanged;
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

	@Inject
	private BankScanner bankScanner;

	@Inject
	private GlassblowingTracker glassblowingTracker;

	@Getter
	private int castCount;

	@Getter
	private long lastCastMillis;

	@Getter
	private int craftingXpGained;

	@Getter
	private int magicXpGained;

	private int startingGlassTotal = -1;
	private boolean snapshotIncludesBank = false;
	private int frozenGlassProduced = 0;
	private int itemsBlownAtSnapshot = 0;
	private int lastCraftingXp = -1;
	private int lastMagicXp = -1;

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (event.getActor() != client.getLocalPlayer())
		{
			return;
		}

		if (event.getActor().getAnimation() == SUPERGLASS_MAKE_ANIMATION)
		{
			// Snapshot before first cast if we haven't yet
			if (startingGlassTotal < 0)
			{
				snapshotGlass();
			}
			castCount++;
			lastCastMillis = System.currentTimeMillis();
			log.debug("Superglass Make cast detected! Total casts: {}", castCount);
		}
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

	/**
	 * Whether we still need a bank-accurate snapshot.
	 */
	public boolean needsGlassSnapshot()
	{
		return !snapshotIncludesBank;
	}

	/**
	 * Snapshots the current total glass count as the session baseline.
	 * When bank loads after an initial non-bank snapshot, freezes the current
	 * produced value and starts fresh against the full total.
	 */
	public void snapshotGlass()
	{
		if (startingGlassTotal >= 0 && !snapshotIncludesBank && bankScanner.isBankLoaded())
		{
			// Freeze current produced, switch to bank-inclusive tracking
			frozenGlassProduced = getGlassProduced();
			startingGlassTotal = bankScanner.totalMoltenGlass();
			itemsBlownAtSnapshot = glassblowingTracker.getItemsBlown();
			snapshotIncludesBank = true;
			log.debug("Glass snapshot upgraded with bank: start={}, frozen={}", startingGlassTotal, frozenGlassProduced);
		}
		else if (startingGlassTotal < 0)
		{
			if (bankScanner.isBankLoaded())
			{
				startingGlassTotal = bankScanner.totalMoltenGlass();
				snapshotIncludesBank = true;
			}
			else
			{
				startingGlassTotal = bankScanner.nonBankMoltenGlass();
				snapshotIncludesBank = false;
			}
			itemsBlownAtSnapshot = glassblowingTracker.getItemsBlown();
			log.debug("Glass snapshot: {}, includesBank={}", startingGlassTotal, snapshotIncludesBank);
		}
	}

	/**
	 * Glass produced this session, derived from glass count change + glass consumed by blowing.
	 * Compares like-for-like: non-bank vs non-bank before bank opens, total vs total after.
	 */
	public int getGlassProduced()
	{
		if (startingGlassTotal < 0)
		{
			return frozenGlassProduced;
		}
		int currentComparable = snapshotIncludesBank
			? bankScanner.totalMoltenGlass()
			: bankScanner.nonBankMoltenGlass();
		int consumedSinceSnapshot = glassblowingTracker.getItemsBlown() - itemsBlownAtSnapshot;
		return frozenGlassProduced + Math.max(0, currentComparable - startingGlassTotal + consumedSinceSnapshot);
	}

	/**
	 * Resets only transient state on logout, preserving accumulated session stats.
	 */
	public void softReset()
	{
		frozenGlassProduced = getGlassProduced();
		startingGlassTotal = -1;
		snapshotIncludesBank = false;
		itemsBlownAtSnapshot = glassblowingTracker.getItemsBlown();
		lastCraftingXp = -1;
		lastMagicXp = -1;
	}

	public void reset()
	{
		castCount = 0;
		lastCastMillis = 0;
		craftingXpGained = 0;
		magicXpGained = 0;
		startingGlassTotal = -1;
		snapshotIncludesBank = false;
		frozenGlassProduced = 0;
		itemsBlownAtSnapshot = 0;
		lastCraftingXp = -1;
		lastMagicXp = -1;
	}
}

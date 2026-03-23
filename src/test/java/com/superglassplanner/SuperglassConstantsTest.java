package com.superglassplanner;

import org.junit.Test;

import static com.superglassplanner.SuperglassConstants.*;
import static org.junit.Assert.*;

public class SuperglassConstantsTest
{
	@Test
	public void xpForLevel1_isZero()
	{
		assertEquals(0, xpForLevel(1));
	}

	@Test
	public void xpForLevel2_is83()
	{
		assertEquals(83, xpForLevel(2));
	}

	@Test
	public void xpForLevel99_isCorrect()
	{
		assertEquals(13034431, xpForLevel(99));
	}

	@Test
	public void xpForLevel_belowOne_returnsZero()
	{
		assertEquals(0, xpForLevel(0));
		assertEquals(0, xpForLevel(-1));
	}

	@Test
	public void xpForLevel_above99_clamps()
	{
		assertEquals(xpForLevel(99), xpForLevel(100));
		assertEquals(xpForLevel(99), xpForLevel(200));
	}

	@Test
	public void xpForLevel_isMonotonicallyIncreasing()
	{
		for (int level = 2; level <= 99; level++)
		{
			assertTrue("XP should increase from level " + (level - 1) + " to " + level,
				xpForLevel(level) > xpForLevel(level - 1));
		}
	}

	@Test
	public void constants_haveExpectedValues()
	{
		assertEquals(18, SAND_PER_CAST);
		assertEquals(3, GIANT_SEAWEED_PER_CAST);
		assertEquals(2, ASTRAL_RUNES_PER_CAST);
		assertEquals(180.0, CRAFTING_XP_PER_CAST, 0.001);
		assertEquals(78.0, MAGIC_XP_PER_CAST, 0.001);
		assertEquals(28.8, AVG_TOTAL_GLASS_PER_CAST, 0.001);
	}
}

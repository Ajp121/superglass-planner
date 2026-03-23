package com.superglassplanner;

import org.junit.Test;

import static org.junit.Assert.*;

public class GlassItemTest
{
	@Test
	public void bestItemForLevel_level1_returnsBeerGlass()
	{
		assertEquals(GlassItem.BEER_GLASS, GlassItem.bestItemForLevel(1));
	}

	@Test
	public void bestItemForLevel_level3_returnsBeerGlass()
	{
		assertEquals(GlassItem.BEER_GLASS, GlassItem.bestItemForLevel(3));
	}

	@Test
	public void bestItemForLevel_level4_returnsCandleLantern()
	{
		assertEquals(GlassItem.CANDLE_LANTERN, GlassItem.bestItemForLevel(4));
	}

	@Test
	public void bestItemForLevel_level12_returnsOilLamp()
	{
		assertEquals(GlassItem.OIL_LAMP, GlassItem.bestItemForLevel(12));
	}

	@Test
	public void bestItemForLevel_level33_returnsVial()
	{
		assertEquals(GlassItem.VIAL, GlassItem.bestItemForLevel(33));
	}

	@Test
	public void bestItemForLevel_level42_returnsFishbowl()
	{
		assertEquals(GlassItem.FISHBOWL, GlassItem.bestItemForLevel(42));
	}

	@Test
	public void bestItemForLevel_level46_returnsUnpoweredOrb()
	{
		assertEquals(GlassItem.UNPOWERED_ORB, GlassItem.bestItemForLevel(46));
	}

	@Test
	public void bestItemForLevel_level49_returnsLanternLens()
	{
		assertEquals(GlassItem.LANTERN_LENS, GlassItem.bestItemForLevel(49));
	}

	@Test
	public void bestItemForLevel_level87_returnsLightOrb()
	{
		assertEquals(GlassItem.LIGHT_ORB, GlassItem.bestItemForLevel(87));
	}

	@Test
	public void bestItemForLevel_level99_returnsLightOrb()
	{
		assertEquals(GlassItem.LIGHT_ORB, GlassItem.bestItemForLevel(99));
	}

	@Test
	public void bestAvailable_hasZeroXp()
	{
		assertEquals(0, GlassItem.BEST_AVAILABLE.getXpPerGlass(), 0.001);
	}

	@Test
	public void bestAvailable_toString_noXpSuffix()
	{
		assertEquals("Best available", GlassItem.BEST_AVAILABLE.toString());
	}

	@Test
	public void regularItem_toString_includesXp()
	{
		assertTrue(GlassItem.UNPOWERED_ORB.toString().contains("52.5"));
	}
}

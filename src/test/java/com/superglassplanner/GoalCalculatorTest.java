package com.superglassplanner;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static com.superglassplanner.SuperglassConstants.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GoalCalculatorTest
{
	private GoalCalculator calc;
	private Client client;
	private SuperglassPlannerConfig config;
	private BankScanner bankScanner;

	@Before
	public void setUp() throws Exception
	{
		calc = new GoalCalculator();
		client = mock(Client.class);
		config = mock(SuperglassPlannerConfig.class);
		bankScanner = mock(BankScanner.class);

		setField(calc, "client", client);
		setField(calc, "config", config);
		setField(calc, "bankScanner", bankScanner);

		// Sensible defaults
		when(config.pickupExtraGlass()).thenReturn(true);
		when(config.glassItem()).thenReturn(GlassItem.UNPOWERED_ORB);
		when(config.goalType()).thenReturn(GoalType.TARGET_LEVEL);
		when(config.targetLevel()).thenReturn(99);
		when(config.factorExistingGlass()).thenReturn(false);
		when(config.glassPerCast()).thenReturn(26);

		when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(70);
		when(client.getSkillExperience(Skill.CRAFTING)).thenReturn(737627);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception
	{
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	// ---- glassPerCast ----

	@Test
	public void glassPerCast_pickupEnabled_returnsAverage()
	{
		when(config.pickupExtraGlass()).thenReturn(true);
		assertEquals(AVG_TOTAL_GLASS_PER_CAST, calc.glassPerCast(), 0.001);
	}

	@Test
	public void glassPerCast_pickupDisabled_returnsConfigValue()
	{
		when(config.pickupExtraGlass()).thenReturn(false);
		when(config.glassPerCast()).thenReturn(26);
		assertEquals(26.0, calc.glassPerCast(), 0.001);
	}

	@Test
	public void glassPerCast_pickupDisabled_guardsAgainstZero()
	{
		when(config.pickupExtraGlass()).thenReturn(false);
		when(config.glassPerCast()).thenReturn(0);
		assertEquals(1.0, calc.glassPerCast(), 0.001);
	}

	// ---- currentGlassItem ----

	@Test
	public void currentGlassItem_fixedItem_returnsConfigItem()
	{
		when(config.glassItem()).thenReturn(GlassItem.VIAL);
		assertEquals(GlassItem.VIAL, calc.currentGlassItem());
	}

	@Test
	public void currentGlassItem_bestAvailable_resolvesToBestForLevel()
	{
		when(config.glassItem()).thenReturn(GlassItem.BEST_AVAILABLE);
		when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(46);
		assertEquals(GlassItem.UNPOWERED_ORB, calc.currentGlassItem());
	}

	@Test
	public void currentGlassItem_bestAvailable_level87_returnsLightOrb()
	{
		when(config.glassItem()).thenReturn(GlassItem.BEST_AVAILABLE);
		when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(87);
		assertEquals(GlassItem.LIGHT_ORB, calc.currentGlassItem());
	}

	// ---- effectiveXpPerGlass ----

	@Test
	public void effectiveXpPerGlass_includesMakeAndBlowXp()
	{
		when(config.glassItem()).thenReturn(GlassItem.UNPOWERED_ORB);
		double makeXpPerGlass = CRAFTING_XP_PER_CAST / AVG_TOTAL_GLASS_PER_CAST;
		double expected = 52.5 + makeXpPerGlass;
		assertEquals(expected, calc.effectiveXpPerGlass(), 0.001);
	}

	// ---- xpRemaining ----

	@Test
	public void xpRemaining_targetLevel99()
	{
		when(config.goalType()).thenReturn(GoalType.TARGET_LEVEL);
		when(config.targetLevel()).thenReturn(99);
		when(client.getSkillExperience(Skill.CRAFTING)).thenReturn(737627);

		int expected = xpForLevel(99) - 737627;
		assertEquals(expected, calc.xpRemaining());
	}

	@Test
	public void xpRemaining_targetXp()
	{
		when(config.goalType()).thenReturn(GoalType.TARGET_XP);
		when(config.targetXp()).thenReturn(1000000);
		when(client.getSkillExperience(Skill.CRAFTING)).thenReturn(500000);

		assertEquals(500000, calc.xpRemaining());
	}

	@Test
	public void xpRemaining_alreadyPastGoal_returnsZero()
	{
		when(config.goalType()).thenReturn(GoalType.TARGET_XP);
		when(config.targetXp()).thenReturn(500000);
		when(client.getSkillExperience(Skill.CRAFTING)).thenReturn(600000);

		assertEquals(0, calc.xpRemaining());
	}

	// ---- glassNeeded ----

	@Test
	public void glassNeeded_targetGlass_returnsTarget()
	{
		when(config.goalType()).thenReturn(GoalType.TARGET_GLASS);
		when(config.targetGlass()).thenReturn(5000);
		when(config.factorExistingGlass()).thenReturn(false);

		assertEquals(5000, calc.glassNeeded());
	}

	@Test
	public void glassNeeded_targetGlass_factorsExisting()
	{
		when(config.goalType()).thenReturn(GoalType.TARGET_GLASS);
		when(config.targetGlass()).thenReturn(5000);
		when(config.factorExistingGlass()).thenReturn(true);
		when(bankScanner.totalMoltenGlass()).thenReturn(2000);

		assertEquals(3000, calc.glassNeeded());
	}

	@Test
	public void glassNeeded_fixedItem_positive()
	{
		when(config.goalType()).thenReturn(GoalType.TARGET_LEVEL);
		when(config.targetLevel()).thenReturn(99);
		when(client.getSkillExperience(Skill.CRAFTING)).thenReturn(737627);

		int glass = calc.glassNeeded();
		assertTrue("Should need positive glass", glass > 0);
		assertTrue("Should be reasonable amount", glass < 500000);
	}

	@Test
	public void glassNeeded_bestAvailable_returnsPositive()
	{
		when(config.glassItem()).thenReturn(GlassItem.BEST_AVAILABLE);
		when(config.goalType()).thenReturn(GoalType.TARGET_LEVEL);
		when(config.targetLevel()).thenReturn(99);
		when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(70);
		when(client.getSkillExperience(Skill.CRAFTING)).thenReturn(737627);

		int glass = calc.glassNeeded();
		assertTrue("Should need positive glass", glass > 0);
	}

	@Test
	public void glassNeeded_bestAvailable_lessOrEqualToFixedLowItem()
	{
		// Best available should need <= glass than using a fixed low-XP item
		// because it upgrades to better items as you level
		when(config.goalType()).thenReturn(GoalType.TARGET_LEVEL);
		when(config.targetLevel()).thenReturn(99);
		when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(33);
		when(client.getSkillExperience(Skill.CRAFTING)).thenReturn(xpForLevel(33));

		when(config.glassItem()).thenReturn(GlassItem.VIAL);
		int fixedGlass = calc.glassNeeded();

		when(config.glassItem()).thenReturn(GlassItem.BEST_AVAILABLE);
		int bestGlass = calc.glassNeeded();

		assertTrue("Best available should need <= glass than fixed vial, got best=" + bestGlass + " fixed=" + fixedGlass,
			bestGlass <= fixedGlass);
	}

	// ---- castsNeeded ----

	@Test
	public void castsNeeded_derivesFromGlassNeeded()
	{
		when(config.goalType()).thenReturn(GoalType.TARGET_GLASS);
		when(config.targetGlass()).thenReturn(100);
		when(config.factorExistingGlass()).thenReturn(false);

		int casts = calc.castsNeeded();
		assertEquals((int) Math.ceil(100.0 / AVG_TOTAL_GLASS_PER_CAST), casts);
	}

	// ---- progress ----

	@Test
	public void progress_atGoal_returnsOne()
	{
		when(config.goalType()).thenReturn(GoalType.TARGET_LEVEL);
		when(config.targetLevel()).thenReturn(70);
		when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(70);
		when(client.getSkillExperience(Skill.CRAFTING)).thenReturn(xpForLevel(70));

		assertEquals(1.0, calc.progress(), 0.001);
	}

	@Test
	public void progress_targetXp_pastGoal_returnsOne()
	{
		when(config.goalType()).thenReturn(GoalType.TARGET_XP);
		when(config.targetXp()).thenReturn(500000);
		when(client.getSkillExperience(Skill.CRAFTING)).thenReturn(600000);
		when(client.getRealSkillLevel(Skill.CRAFTING)).thenReturn(70);

		assertEquals(1.0, calc.progress(), 0.001);
	}
}

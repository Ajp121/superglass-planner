package com.superglassplanner;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;

/**
 * Side panel displaying Superglass Planner information.
 */
public class SuperglassPlannerPanel extends PluginPanel
{
	private final SuperglassPlannerConfig config;
	private final BankScanner bankScanner;
	private final GoalCalculator goalCalculator;
	private final SessionTracker sessionTracker;

	// Bank section labels
	private final JLabel giantSeaweedLabel = new JLabel();
	private final JLabel bucketOfSandLabel = new JLabel();
	private final JLabel moltenGlassLabel = new JLabel();
	private final JLabel astralRuneLabel = new JLabel();
	private final JLabel possibleCastsLabel = new JLabel();
	private final JLabel estimatedGlassLabel = new JLabel();
	private final JLabel bankSeaweedNeededLabel = new JLabel();
	private final JLabel bankSandNeededLabel = new JLabel();
	private final JLabel bankAstralsNeededLabel = new JLabel();

	// Goal section labels
	private final JLabel xpRemainingLabel = new JLabel();
	private final JLabel glassNeededLabel = new JLabel();
	private final JLabel castsNeededLabel = new JLabel();
	private final JLabel seaweedDeficitLabel = new JLabel();
	private final JLabel sandDeficitLabel = new JLabel();
	private final JLabel progressLabel = new JLabel();

	// Session section labels
	private final JLabel sessionCastsLabel = new JLabel();
	private final JLabel sessionGlassLabel = new JLabel();
	private final JLabel sessionCraftXpLabel = new JLabel();
	private final JLabel sessionMagicXpLabel = new JLabel();

	private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance();

	@Inject
	public SuperglassPlannerPanel(
		SuperglassPlannerConfig config,
		BankScanner bankScanner,
		GoalCalculator goalCalculator,
		SessionTracker sessionTracker)
	{
		this.config = config;
		this.bankScanner = bankScanner;
		this.goalCalculator = goalCalculator;
		this.sessionTracker = sessionTracker;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(createTitle());
		add(Box.createVerticalStrut(10));
		add(createBankSection());
		add(Box.createVerticalStrut(10));
		add(createGoalSection());
		add(Box.createVerticalStrut(10));
		add(createSessionSection());
	}

	private JPanel createTitle()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Superglass Planner");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		panel.add(title, BorderLayout.WEST);

		return panel;
	}

	private JPanel createBankSection()
	{
		JPanel section = createSection("Bank Materials");

		section.add(createRow("Giant Seaweed:", giantSeaweedLabel));
		section.add(createRow("Bucket of Sand:", bucketOfSandLabel));
		section.add(createRow("Molten Glass:", moltenGlassLabel));
		section.add(createRow("Astral Runes:", astralRuneLabel));
		section.add(Box.createVerticalStrut(5));
		section.add(createRow("Possible Casts:", possibleCastsLabel));
		section.add(createRow("Est. Glass Output:", estimatedGlassLabel));
		section.add(Box.createVerticalStrut(5));
		section.add(createRow("Seaweed Needed:", bankSeaweedNeededLabel));
		section.add(createRow("Sand Needed:", bankSandNeededLabel));
		section.add(createRow("Astrals Needed:", bankAstralsNeededLabel));

		return section;
	}

	private JPanel createGoalSection()
	{
		JPanel section = createSection("Goal");

		section.add(createRow("XP Remaining:", xpRemainingLabel));
		section.add(createRow("Glass Needed:", glassNeededLabel));
		section.add(createRow("Casts Needed:", castsNeededLabel));
		section.add(createRow("Seaweed Deficit:", seaweedDeficitLabel));
		section.add(createRow("Sand Deficit:", sandDeficitLabel));
		section.add(createRow("Progress:", progressLabel));

		return section;
	}

	private JPanel createSessionSection()
	{
		JPanel section = createSection("Session Stats");

		section.add(createRow("Casts:", sessionCastsLabel));
		section.add(createRow("Glass Made:", sessionGlassLabel));
		section.add(createRow("Crafting XP:", sessionCraftXpLabel));
		section.add(createRow("Magic XP:", sessionMagicXpLabel));

		JButton resetButton = new JButton("Reset Session");
		resetButton.addActionListener(e -> sessionTracker.reset());
		section.add(Box.createVerticalStrut(5));
		section.add(resetButton);

		return section;
	}

	private JPanel createSection(String title)
	{
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		section.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
			new EmptyBorder(8, 8, 8, 8)
		));

		JLabel header = new JLabel(title);
		header.setFont(FontManager.getRunescapeSmallFont());
		header.setForeground(Color.WHITE);
		header.setAlignmentX(LEFT_ALIGNMENT);
		section.add(header);
		section.add(Box.createVerticalStrut(5));

		return section;
	}

	private JPanel createRow(String label, JLabel valueLabel)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
		row.setAlignmentX(LEFT_ALIGNMENT);

		JLabel nameLabel = new JLabel(label);
		nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());

		valueLabel.setForeground(Color.WHITE);
		valueLabel.setFont(FontManager.getRunescapeSmallFont());
		valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

		row.add(nameLabel, BorderLayout.WEST);
		row.add(valueLabel, BorderLayout.EAST);

		return row;
	}

	/**
	 * Called periodically to refresh displayed values.
	 */
	public void update()
	{
		// Bank
		if (bankScanner.isBankLoaded())
		{
			giantSeaweedLabel.setText(FORMAT.format(bankScanner.getGiantSeaweedCount()));
			bucketOfSandLabel.setText(FORMAT.format(bankScanner.getBucketOfSandCount()));
			moltenGlassLabel.setText(FORMAT.format(bankScanner.getMoltenGlassCount()));

			int totalAstrals = bankScanner.totalAstralRunes();
			String astralText = FORMAT.format(totalAstrals);
			if (bankScanner.getRunePouchAstralCount() > 0)
			{
				astralText += " (" + FORMAT.format(bankScanner.getRunePouchAstralCount()) + " in pouch)";
			}
			astralRuneLabel.setText(astralText);

			possibleCastsLabel.setText(FORMAT.format(bankScanner.possibleCasts()));
			estimatedGlassLabel.setText(FORMAT.format(bankScanner.estimatedGlass(config.pickupExtraGlass())));

			int seaweedDef = bankScanner.seaweedDeficit();
			int sandDef = bankScanner.sandDeficit();
			int astralDef = bankScanner.astralDeficit();
			bankSeaweedNeededLabel.setText(seaweedDef > 0 ? "+" + FORMAT.format(seaweedDef) : "Balanced");
			bankSandNeededLabel.setText(sandDef > 0 ? "+" + FORMAT.format(sandDef) : "Balanced");
			bankAstralsNeededLabel.setText(astralDef > 0 ? "+" + FORMAT.format(astralDef) : "Balanced");
			bankSeaweedNeededLabel.setForeground(seaweedDef > 0 ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_COMPLETE_COLOR);
			bankSandNeededLabel.setForeground(sandDef > 0 ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_COMPLETE_COLOR);
			bankAstralsNeededLabel.setForeground(astralDef > 0 ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_COMPLETE_COLOR);
		}
		else
		{
			giantSeaweedLabel.setText("Open bank");
			bucketOfSandLabel.setText("-");
			moltenGlassLabel.setText("-");
			astralRuneLabel.setText("-");
			possibleCastsLabel.setText("-");
			estimatedGlassLabel.setText("-");
			bankSeaweedNeededLabel.setText("-");
			bankSandNeededLabel.setText("-");
			bankAstralsNeededLabel.setText("-");
		}

		// Goal
		xpRemainingLabel.setText(FORMAT.format(goalCalculator.xpRemaining()));
		glassNeededLabel.setText(FORMAT.format(goalCalculator.glassNeeded()));
		castsNeededLabel.setText(FORMAT.format(goalCalculator.castsNeeded()));

		int seaweedDef = goalCalculator.seaweedDeficit();
		int sandDef = goalCalculator.sandDeficit();
		seaweedDeficitLabel.setText(seaweedDef > 0 ? FORMAT.format(seaweedDef) : "Enough!");
		sandDeficitLabel.setText(sandDef > 0 ? FORMAT.format(sandDef) : "Enough!");
		seaweedDeficitLabel.setForeground(seaweedDef > 0 ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_COMPLETE_COLOR);
		sandDeficitLabel.setForeground(sandDef > 0 ? ColorScheme.PROGRESS_ERROR_COLOR : ColorScheme.PROGRESS_COMPLETE_COLOR);

		double prog = goalCalculator.progress();
		progressLabel.setText(String.format("%.1f%%", prog * 100));

		// Session
		sessionCastsLabel.setText(FORMAT.format(sessionTracker.getCastCount()));
		sessionGlassLabel.setText(FORMAT.format(sessionTracker.getGlassProduced()));
		sessionCraftXpLabel.setText(FORMAT.format(sessionTracker.getCraftingXpGained()));
		sessionMagicXpLabel.setText(FORMAT.format(sessionTracker.getMagicXpGained()));
	}
}

package com.superglassplanner;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.text.NumberFormat;

/**
 * Side panel styled to match RuneLite config panels.
 * Uses DynamicGridLayout with section headers, no colored section backgrounds.
 */
public class SuperglassPlannerPanel extends PluginPanel
{
	private static final String CONFIG_GROUP = "superglassplanner";
	private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance();

	private static final Color VALUE_HIGHLIGHT = new Color(190, 210, 255);
	private static final Color DIMMED_VALUE = new Color(120, 120, 120);

	private final SuperglassPlannerConfig config;
	private final ConfigManager configManager;
	private final BankScanner bankScanner;
	private final GoalCalculator goalCalculator;
	private final SessionTracker sessionTracker;

	// Bank labels
	private final JLabel giantSeaweedLabel = valueLabel();
	private final JLabel bucketOfSandLabel = valueLabel();
	private final JLabel moltenGlassLabel = valueLabel();
	private final JLabel astralRuneLabel = valueLabel();
	private final JLabel possibleCastsLabel = valueLabel();
	private final JLabel estimatedGlassLabel = valueLabel();
	private final JLabel bankSeaweedNeededLabel = valueLabel();
	private final JLabel bankSandNeededLabel = valueLabel();
	private final JLabel bankAstralsNeededLabel = valueLabel();

	// Goal labels
	private final JLabel goalHeaderLabel = new JLabel();
	private final JLabel xpRemainingLabel = valueLabel();
	private final JLabel glassNeededLabel = valueLabel();
	private final JLabel castsNeededLabel = valueLabel();
	private final JLabel seaweedDeficitLabel = valueLabel();
	private final JLabel sandDeficitLabel = valueLabel();
	private final ProgressBar goalProgressBar = new ProgressBar();

	// Goal controls
	private final JComboBox<GoalType> goalTypeCombo = new JComboBox<>(GoalType.values());
	private final JComboBox<GlassItem> glassItemCombo = new JComboBox<>(GlassItem.values());
	private final JSpinner targetSpinner = new JSpinner(new SpinnerNumberModel(99, 1, 200_000_000, 1));
	private final JLabel targetLabel = new JLabel("Target Level");
	private final JCheckBox pickupCheckbox = new JCheckBox();
	private final JCheckBox existingGlassCheckbox = new JCheckBox();

	// Session labels
	private final JLabel sessionCastsLabel = valueLabel();
	private final JLabel sessionGlassLabel = valueLabel();
	private final JLabel sessionCraftXpLabel = valueLabel();
	private final JLabel sessionMagicXpLabel = valueLabel();

	private JPanel bankDataPanel;
	private JPanel bankNotLoadedPanel;
	private JPanel bankWrapper;
	private boolean updatingControls = false;
	private boolean active = false;

	@Inject
	public SuperglassPlannerPanel(
	SuperglassPlannerConfig config,
	ConfigManager configManager,
	BankScanner bankScanner,
	GoalCalculator goalCalculator,
	SessionTracker sessionTracker)
	{
		super(false);

		this.config = config;
		this.configManager = configManager;
		this.bankScanner = bankScanner;
		this.goalCalculator = goalCalculator;
		this.sessionTracker = sessionTracker;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
		mainPanel.setBorder(new EmptyBorder(8, 10, 10, 10));
		mainPanel.setAlignmentX(LEFT_ALIGNMENT);

		mainPanel.add(createTitle());
		buildBankSection(mainPanel);
		buildGoalSection(mainPanel);
		buildSessionSection(mainPanel);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.add(mainPanel, BorderLayout.NORTH);
		add(wrapper, BorderLayout.CENTER);

		initGoalControls();

		goalTypeCombo.setFocusable(false);
		glassItemCombo.setFocusable(false);
	}

	@Override
	public void onActivate()
	{
		active = true;
		SwingUtilities.updateComponentTreeUI(this);
		update();
	}

	@Override
	public void onDeactivate()
	{
		active = false;
	}

	public boolean isActive()
	{
		return active;
	}

	private static JLabel valueLabel()
	{
		JLabel l = new JLabel();
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(Color.WHITE);
		l.setHorizontalAlignment(SwingConstants.RIGHT);
		return l;
	}

	// ---- Control wiring ----

	private void initGoalControls()
	{
		updatingControls = true;
		goalTypeCombo.setSelectedItem(config.goalType());
		glassItemCombo.setSelectedItem(config.glassItem());
		pickupCheckbox.setSelected(config.pickupExtraGlass());
		existingGlassCheckbox.setSelected(config.factorExistingGlass());
		syncTargetSpinner();
		updatingControls = false;

		goalTypeCombo.addActionListener(e ->
		{
			if (updatingControls) return;
			configManager.setConfiguration(CONFIG_GROUP, "goalType", goalTypeCombo.getSelectedItem());
			updatingControls = true;
			syncTargetSpinner();
			updatingControls = false;
			update();
		});

		targetSpinner.addChangeListener(e ->
		{
			if (updatingControls) return;
			int value = (Integer) targetSpinner.getValue();
			GoalType type = (GoalType) goalTypeCombo.getSelectedItem();
			switch (type)
			{
				case TARGET_LEVEL:
				configManager.setConfiguration(CONFIG_GROUP, "targetLevel", value);
				break;
				case TARGET_XP:
				configManager.setConfiguration(CONFIG_GROUP, "targetXp", value);
				break;
				case TARGET_GLASS:
				configManager.setConfiguration(CONFIG_GROUP, "targetGlass", value);
				break;
			}
			update();
		});

		glassItemCombo.addActionListener(e ->
		{
			if (updatingControls) return;
			configManager.setConfiguration(CONFIG_GROUP, "glassItem", glassItemCombo.getSelectedItem());
			update();
		});

		pickupCheckbox.addActionListener(e ->
		{
			if (updatingControls) return;
			configManager.setConfiguration(CONFIG_GROUP, "pickupExtraGlass", pickupCheckbox.isSelected());
			update();
		});

		existingGlassCheckbox.addActionListener(e ->
		{
			if (updatingControls) return;
			configManager.setConfiguration(CONFIG_GROUP, "factorExistingGlass", existingGlassCheckbox.isSelected());
			update();
		});
	}

	private void syncTargetSpinner()
	{
		GoalType type = (GoalType) goalTypeCombo.getSelectedItem();
		switch (type)
		{
			case TARGET_LEVEL:
			targetLabel.setText("Target Level");
			targetSpinner.setModel(new SpinnerNumberModel(config.targetLevel(), 1, 99, 1));
			break;
			case TARGET_XP:
			targetLabel.setText("Target XP");
			targetSpinner.setModel(new SpinnerNumberModel(config.targetXp(), 0, 200_000_000, 1000));
			break;
			case TARGET_GLASS:
			targetLabel.setText("Target Glass");
			targetSpinner.setModel(new SpinnerNumberModel(config.targetGlass(), 0, 10_000_000, 100));
			break;
		}
		JSpinner.NumberEditor editor = new JSpinner.NumberEditor(targetSpinner, "#");
		targetSpinner.setEditor(editor);
	}

	// ---- Section builders ----

	private JPanel createTitle()
	{
		JPanel p = new JPanel(new BorderLayout());
		p.setAlignmentX(LEFT_ALIGNMENT);

		JLabel title = new JLabel("Superglass Planner");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		p.add(title, BorderLayout.WEST);
		return p;
	}

	private JPanel sectionHeader(String text)
	{
		JLabel l = new JLabel(text);
		return sectionHeader(l);
	}

	private JPanel sectionHeader(JLabel label)
	{
		JPanel h = new JPanel(new BorderLayout());
		h.setBorder(new CompoundBorder(
		new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
		new EmptyBorder(8, 0, 3, 0)
	));

	label.setFont(FontManager.getRunescapeBoldFont().deriveFont(Font.BOLD, 16f));
	label.setForeground(ColorScheme.BRAND_ORANGE);
	h.add(label, BorderLayout.CENTER);
	return h;
}

private void buildBankSection(JPanel parent)
{
	parent.add(sectionHeader("Bank Materials"));

	// Wrapper so not-loaded and loaded share one grid slot
	bankWrapper = new JPanel(new CardLayout());

	// Not loaded
	bankNotLoadedPanel = new JPanel(new BorderLayout());
	bankNotLoadedPanel.setBorder(new EmptyBorder(4, 0, 4, 0));
	JLabel msg = new JLabel("Open your bank to scan materials");
	msg.setFont(FontManager.getRunescapeSmallFont());
	msg.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
	bankNotLoadedPanel.add(msg, BorderLayout.CENTER);
	bankWrapper.add(bankNotLoadedPanel, "notLoaded");

	// Loaded
	bankDataPanel = new JPanel();
	bankDataPanel.setLayout(new DynamicGridLayout(0, 1, 0, 3));
	bankDataPanel.add(row("Giant Seaweed", giantSeaweedLabel));
	bankDataPanel.add(row("Bucket of Sand", bucketOfSandLabel));
	bankDataPanel.add(row("Molten Glass", moltenGlassLabel));
	bankDataPanel.add(row("Astral Runes", astralRuneLabel));
	bankDataPanel.add(row("Possible Casts", possibleCastsLabel));
	bankDataPanel.add(row("Est. Glass", estimatedGlassLabel));
	bankDataPanel.add(subHeader("Material Balance"));
	bankDataPanel.add(row("Seaweed", bankSeaweedNeededLabel));
	bankDataPanel.add(row("Sand", bankSandNeededLabel));
	bankDataPanel.add(row("Astrals", bankAstralsNeededLabel));
	bankWrapper.add(bankDataPanel, "loaded");

	parent.add(bankWrapper);
}

private void buildGoalSection(JPanel parent)
{
	goalHeaderLabel.setFont(FontManager.getRunescapeSmallFont());
	goalHeaderLabel.setForeground(Color.WHITE);
	parent.add(sectionHeader(goalHeaderLabel));

	parent.add(labeledControl("Goal Type", goalTypeCombo));
	parent.add(labeledControl(targetLabel, targetSpinner));
	parent.add(stackedControl("Glass Item", glassItemCombo));
	parent.add(row("Pickup extra glass", pickupCheckbox));
	parent.add(row("Factor existing glass", existingGlassCheckbox));
	parent.add(row("XP Remaining", xpRemainingLabel));
	parent.add(row("Glass Needed", glassNeededLabel));
	parent.add(row("Casts Needed", castsNeededLabel));
	parent.add(row("Seaweed Deficit", seaweedDeficitLabel));
	parent.add(row("Sand Deficit", sandDeficitLabel));

	goalProgressBar.setMaximumValue(100);
	goalProgressBar.setValue(0);
	goalProgressBar.setCenterLabel("0.0%");
	goalProgressBar.setForeground(new Color(60, 180, 70));
	goalProgressBar.setBackground(new Color(60, 60, 60));
	goalProgressBar.setPreferredSize(new Dimension(0, 16));
	parent.add(goalProgressBar);
}

private void buildSessionSection(JPanel parent)
{
	parent.add(sectionHeader("Session Stats"));
	parent.add(row("Casts", sessionCastsLabel));
	parent.add(row("Glass Made", sessionGlassLabel));
	parent.add(row("Crafting XP", sessionCraftXpLabel));
	parent.add(row("Magic XP", sessionMagicXpLabel));

	JButton resetButton = new JButton("Reset Session");
	resetButton.setFocusPainted(false);
	resetButton.addActionListener(e -> sessionTracker.reset());
	parent.add(resetButton);
}

// ---- Reusable layout helpers ----

private JPanel row(String label, JComponent value)
{
	JLabel name = new JLabel(label);
	name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
	name.setFont(FontManager.getRunescapeSmallFont());
	return row(name, value);
}

private JPanel row(JLabel name, JComponent value)
{
	JPanel r = new JPanel(new BorderLayout());
	r.add(name, BorderLayout.CENTER);
	r.add(value, BorderLayout.EAST);
	return r;
}

private JPanel subHeader(String text)
{
	JPanel p = new JPanel(new BorderLayout());
	p.setBorder(new EmptyBorder(2, 0, 0, 0));
	JLabel l = new JLabel(text);
	l.setFont(FontManager.getRunescapeBoldFont());
	l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
	p.add(l, BorderLayout.WEST);
	return p;
}

private JPanel labeledControl(String labelText, JComponent control)
{
	return labeledControl(new JLabel(labelText), control);
}

private JPanel labeledControl(JLabel label, JComponent control)
{
	JPanel r = new JPanel(new BorderLayout());

	label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
	label.setFont(FontManager.getRunescapeSmallFont());
	r.add(label, BorderLayout.CENTER);
	r.add(control, BorderLayout.EAST);
	return r;
}

private JPanel stackedControl(String labelText, JComponent control)
{
	JPanel p = new JPanel(new BorderLayout());
	JLabel label = new JLabel(labelText);
	label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
	label.setFont(FontManager.getRunescapeSmallFont());
	p.add(label, BorderLayout.NORTH);
	p.add(control, BorderLayout.CENTER);
	return p;
}

// ---- Update ----

public void update()
{
	goalHeaderLabel.setText("Goal: " + goalCalculator.goalDescription());

	if (bankScanner.isBankLoaded())
	{
		((CardLayout) bankWrapper.getLayout()).show(bankWrapper, "loaded");

		giantSeaweedLabel.setText(FORMAT.format(bankScanner.getGiantSeaweedCount()));
		bucketOfSandLabel.setText(FORMAT.format(bankScanner.getBucketOfSandCount()));

		moltenGlassLabel.setText(FORMAT.format(bankScanner.totalMoltenGlass()));

		astralRuneLabel.setText(FORMAT.format(bankScanner.totalAstralRunes()));

		possibleCastsLabel.setText(FORMAT.format(bankScanner.possibleCasts()));
		possibleCastsLabel.setForeground(VALUE_HIGHLIGHT);
		estimatedGlassLabel.setText(FORMAT.format(bankScanner.estimatedGlass(config.pickupExtraGlass())));
		estimatedGlassLabel.setForeground(VALUE_HIGHLIGHT);

		setDeficit(bankSeaweedNeededLabel, bankScanner.seaweedDeficit(), "Balanced");
		setDeficit(bankSandNeededLabel, bankScanner.sandDeficit(), "Balanced");
		setDeficit(bankAstralsNeededLabel, bankScanner.astralDeficit(), "Balanced");
	}
	else
	{
		((CardLayout) bankWrapper.getLayout()).show(bankWrapper, "notLoaded");
	}

	xpRemainingLabel.setText(FORMAT.format(goalCalculator.xpRemaining()));
	glassNeededLabel.setText(FORMAT.format(goalCalculator.glassNeeded()));
	castsNeededLabel.setText(FORMAT.format(goalCalculator.castsNeeded()));
	setDeficit(seaweedDeficitLabel, goalCalculator.seaweedDeficit(), "Enough!");
	setDeficit(sandDeficitLabel, goalCalculator.sandDeficit(), "Enough!");

	double prog = goalCalculator.progress();
	goalProgressBar.setValue((int) (prog * 100));
	goalProgressBar.setCenterLabel(String.format("%.1f%%", prog * 100));
	goalProgressBar.setForeground(prog >= 1.0
	? ColorScheme.PROGRESS_COMPLETE_COLOR
	: prog >= 0.5 ? new Color(60, 180, 70) : ColorScheme.PROGRESS_INPROGRESS_COLOR);

	updateSessionLabel(sessionCastsLabel, sessionTracker.getCastCount());
	updateSessionLabel(sessionGlassLabel, sessionTracker.getGlassProduced());
	updateSessionLabel(sessionCraftXpLabel, sessionTracker.getCraftingXpGained());
	updateSessionLabel(sessionMagicXpLabel, sessionTracker.getMagicXpGained());
}

private void updateSessionLabel(JLabel label, int value)
{
	label.setText(FORMAT.format(value));
	label.setForeground(value == 0 ? DIMMED_VALUE : Color.WHITE);
}

private void setDeficit(JLabel label, int deficit, String goodText)
{
	if (deficit > 0)
	{
		label.setText("-" + FORMAT.format(deficit));
		label.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
	}
	else
	{
		label.setText(goodText);
		label.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
	}
}
}

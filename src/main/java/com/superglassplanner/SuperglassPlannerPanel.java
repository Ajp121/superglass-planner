package com.superglassplanner;

import net.runelite.api.Client;
import net.runelite.api.GameState;
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

	private final Client client;
	private final SuperglassPlannerConfig config;
	private final ConfigManager configManager;
	private final BankScanner bankScanner;
	private final GoalCalculator goalCalculator;
	private final SessionTracker sessionTracker;
	private final GlassblowingTracker glassblowingTracker;

	// Bank labels
	private final JLabel giantSeaweedLabel = valueLabel();
	private final JLabel bucketOfSandLabel = valueLabel();
	private final JLabel moltenGlassLabel = valueLabel();
	private final JLabel astralRuneLabel = valueLabel();
	private final JLabel possibleCastsLabel = valueLabel();
	private final JLabel estimatedGlassLabel = valueLabel();
	private final JLabel bankBalanceLabel1 = valueLabel();
	private final JLabel bankBalanceLabel2 = valueLabel();
	private final JLabel bankBalanceName1 = new JLabel();
	private final JLabel bankBalanceName2 = new JLabel();
	private final JLabel balanceHeaderLabel = new JLabel("To Use All Seaweed");
	private int balanceMode = 0; // 0=seaweed, 1=sand, 2=astrals

	// Goal labels
	private final JLabel goalHeaderLabel = new JLabel();
	private final JLabel xpRemainingLabel = valueLabel();
	private final JLabel glassNeededLabel = valueLabel();
	private final JLabel itemsToBlowLabel = valueLabel();
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
	private final JSpinner glassPerCastSpinner = new JSpinner(new SpinnerNumberModel(26, 1, 50, 1))
	{{
		setToolTipText("Free inventory slots after casting (glass produced per cast)");
	}};
	private JPanel glassPerCastWrapper;
	private final JCheckBox existingGlassCheckbox = new JCheckBox();
	private final JLabel bankWarningLabel = new JLabel("Open bank to update glass count");
	private JPanel bankWarningWrapper;

	// Session labels
	private final JLabel sessionCastsLabel = valueLabel();
	private final JLabel sessionGlassLabel = valueLabel();
	private final JLabel sessionItemsBlownLabel = valueLabel();
	private final JLabel sessionCraftXpLabel = valueLabel();
	private final JLabel sessionMagicXpLabel = valueLabel();

	private JPanel bankDataPanel;
	private JPanel bankNotLoadedPanel;
	private JPanel bankWrapper;
	private JPanel goalWrapper;
	private JPanel xpRemainingRow;
	private JPanel itemsToBlowRow;
	private boolean updatingControls = false;
	private volatile boolean active = false;

	@Inject
	public SuperglassPlannerPanel(
	Client client,
	SuperglassPlannerConfig config,
	ConfigManager configManager,
	BankScanner bankScanner,
	GoalCalculator goalCalculator,
	SessionTracker sessionTracker,
	GlassblowingTracker glassblowingTracker)
	{
		super(false);

		this.client = client;
		this.config = config;
		this.configManager = configManager;
		this.bankScanner = bankScanner;
		this.goalCalculator = goalCalculator;
		this.sessionTracker = sessionTracker;
		this.glassblowingTracker = glassblowingTracker;

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
		glassPerCastSpinner.setValue(config.glassPerCast());
		syncGlassPerCastRow(!config.pickupExtraGlass());
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
			syncGlassPerCastRow(!pickupCheckbox.isSelected());
			update();
		});

		glassPerCastSpinner.addChangeListener(e ->
		{
			if (updatingControls) return;
			configManager.setConfiguration(CONFIG_GROUP, "glassPerCast", (Integer) glassPerCastSpinner.getValue());
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

	private void syncGlassPerCastRow(boolean show)
	{
		if (show && glassPerCastWrapper.getComponentCount() == 0)
		{
			glassPerCastWrapper.add(labeledControl("Inventory space", glassPerCastSpinner), BorderLayout.CENTER);
			glassPerCastWrapper.revalidate();
		}
		else if (!show && glassPerCastWrapper.getComponentCount() > 0)
		{
			glassPerCastWrapper.removeAll();
			glassPerCastWrapper.revalidate();
		}
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

	balanceHeaderLabel.setFont(FontManager.getRunescapeBoldFont());
	balanceHeaderLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
	balanceHeaderLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	JPanel balanceHeader = new JPanel(new BorderLayout());
	balanceHeader.setBorder(new EmptyBorder(2, 0, 0, 0));
	balanceHeader.add(balanceHeaderLabel, BorderLayout.WEST);
	balanceHeader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	balanceHeader.addMouseListener(new java.awt.event.MouseAdapter()
	{
		@Override
		public void mouseClicked(java.awt.event.MouseEvent e)
		{
			balanceMode = (balanceMode + 1) % 3;
			update();
		}
	});
	bankDataPanel.add(balanceHeader);

	bankBalanceName1.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
	bankBalanceName1.setFont(FontManager.getRunescapeSmallFont());
	bankBalanceName2.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
	bankBalanceName2.setFont(FontManager.getRunescapeSmallFont());
	bankDataPanel.add(row(bankBalanceName1, bankBalanceLabel1));
	bankDataPanel.add(row(bankBalanceName2, bankBalanceLabel2));
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
	pickupCheckbox.setToolTipText("Pick up bonus glass that drops on the floor after casting");
	parent.add(row("Pickup extra glass", pickupCheckbox));
	glassPerCastWrapper = new JPanel(new BorderLayout()) {
		@Override
		public Dimension getPreferredSize()
		{
			if (getComponentCount() == 0) return new Dimension(0, 0);
			return super.getPreferredSize();
		}
	};
	parent.add(glassPerCastWrapper);
	existingGlassCheckbox.setToolTipText("Subtract glass already in your bank from goal calculations");
	parent.add(row("Factor existing glass", existingGlassCheckbox));
	bankWarningLabel.setFont(FontManager.getRunescapeFont());
	bankWarningLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
	bankWarningWrapper = new JPanel(new BorderLayout()) {
		@Override
		public Dimension getPreferredSize()
		{
			if (getComponentCount() == 0) return new Dimension(0, 0);
			return super.getPreferredSize();
		}
	};
	parent.add(bankWarningWrapper);

	goalWrapper = new JPanel(new CardLayout());

	// Not logged in
	JPanel goalNotLoggedIn = new JPanel(new BorderLayout());
	goalNotLoggedIn.setBorder(new EmptyBorder(4, 0, 4, 0));
	JLabel loginMsg = new JLabel("Log in to view goal progress");
	loginMsg.setFont(FontManager.getRunescapeSmallFont());
	loginMsg.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
	goalNotLoggedIn.add(loginMsg, BorderLayout.CENTER);
	goalWrapper.add(goalNotLoggedIn, "notLoggedIn");

	// Logged in
	JPanel goalDataPanel = new JPanel();
	goalDataPanel.setLayout(new DynamicGridLayout(0, 1, 0, 3));
	xpRemainingRow = row("XP Remaining", xpRemainingLabel);
	goalDataPanel.add(xpRemainingRow);
	goalDataPanel.add(row("Glass to Make", glassNeededLabel));
	itemsToBlowRow = row("Items to Blow", itemsToBlowLabel);
	goalDataPanel.add(itemsToBlowRow);
	goalDataPanel.add(row("Casts Needed", castsNeededLabel));
	goalDataPanel.add(row("Seaweed Deficit", seaweedDeficitLabel));
	goalDataPanel.add(row("Sand Deficit", sandDeficitLabel));

	goalProgressBar.setMaximumValue(100);
	goalProgressBar.setValue(0);
	goalProgressBar.setCenterLabel("0.0%");
	goalProgressBar.setForeground(new Color(60, 180, 70));
	goalProgressBar.setBackground(new Color(60, 60, 60));
	goalProgressBar.setPreferredSize(new Dimension(0, 16));
	goalDataPanel.add(goalProgressBar);

	goalWrapper.add(goalDataPanel, "loggedIn");
	parent.add(goalWrapper);
}

private void buildSessionSection(JPanel parent)
{
	parent.add(sectionHeader("Session Stats"));
	parent.add(row("Casts", sessionCastsLabel));
	parent.add(row("Glass Made", sessionGlassLabel));
	parent.add(row("Items Blown", sessionItemsBlownLabel));
	parent.add(row("Crafting XP", sessionCraftXpLabel));
	parent.add(row("Magic XP", sessionMagicXpLabel));

	JButton resetButton = new JButton("Reset Session");
	resetButton.setFocusPainted(false);
	resetButton.addActionListener(e ->
	{
		sessionTracker.reset();
		glassblowingTracker.reset();
		update();
	});
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
		estimatedGlassLabel.setText(FORMAT.format(bankScanner.estimatedGlass(goalCalculator.glassPerCast())));
		estimatedGlassLabel.setForeground(VALUE_HIGHLIGHT);

		updateBalanceSection();
	}
	else
	{
		((CardLayout) bankWrapper.getLayout()).show(bankWrapper, "notLoaded");
	}

	if (config.factorExistingGlass() && !bankScanner.isBankLoaded())
	{
		if (bankWarningWrapper.getComponentCount() == 0)
		{
			bankWarningWrapper.add(bankWarningLabel, BorderLayout.CENTER);
			bankWarningWrapper.revalidate();
		}
	}
	else
	{
		if (bankWarningWrapper.getComponentCount() > 0)
		{
			bankWarningWrapper.removeAll();
			bankWarningWrapper.revalidate();
		}
	}

	boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;

	if (loggedIn)
	{
		((CardLayout) goalWrapper.getLayout()).show(goalWrapper, "loggedIn");

		boolean isTargetGlass = config.goalType() == GoalType.TARGET_GLASS;
		xpRemainingRow.setVisible(!isTargetGlass);
		itemsToBlowRow.setVisible(!isTargetGlass);
		goalProgressBar.setVisible(!isTargetGlass || config.factorExistingGlass());

		if (!isTargetGlass)
		{
			xpRemainingLabel.setText(FORMAT.format(goalCalculator.xpRemaining()));
			itemsToBlowLabel.setText(FORMAT.format(goalCalculator.totalItemsToBlow()));
		}

		glassNeededLabel.setText(FORMAT.format(goalCalculator.glassNeeded()));
		castsNeededLabel.setText(FORMAT.format(goalCalculator.castsNeeded()));
		setDeficit(seaweedDeficitLabel, goalCalculator.seaweedDeficit(), "Enough!");
		setDeficit(sandDeficitLabel, goalCalculator.sandDeficit(), "Enough!");

		double prog;
		if (isTargetGlass && config.factorExistingGlass())
		{
			int target = config.targetGlass();
			prog = target > 0 ? Math.min(1.0, (double) bankScanner.totalMoltenGlass() / target) : 1.0;
		}
		else
		{
			prog = Math.min(1.0, Math.max(0.0, goalCalculator.progress()));
		}
		goalProgressBar.setValue((int) (prog * 100));
		goalProgressBar.setCenterLabel(String.format("%.1f%%", prog * 100));
		goalProgressBar.setForeground(prog >= 1.0
		? ColorScheme.PROGRESS_COMPLETE_COLOR
		: prog >= 0.5 ? new Color(60, 180, 70) : ColorScheme.PROGRESS_INPROGRESS_COLOR);
	}
	else
	{
		((CardLayout) goalWrapper.getLayout()).show(goalWrapper, "notLoggedIn");
	}

	updateSessionLabel(sessionCastsLabel, sessionTracker.getCastCount());
	updateSessionLabel(sessionGlassLabel, sessionTracker.getGlassProduced());
	updateSessionLabel(sessionItemsBlownLabel, glassblowingTracker.getItemsBlown());
	updateSessionLabel(sessionCraftXpLabel, sessionTracker.getCraftingXpGained());
	updateSessionLabel(sessionMagicXpLabel, sessionTracker.getMagicXpGained());
}

private void updateSessionLabel(JLabel label, int value)
{
	label.setText(FORMAT.format(value));
	label.setForeground(value == 0 ? DIMMED_VALUE : Color.WHITE);
}

private void updateBalanceSection()
{
	int casts;
	String name1, name2;
	int need1, need2;

	switch (balanceMode)
	{
		case 0: // Use all seaweed
			balanceHeaderLabel.setText("To Use All Seaweed \u25B6");
			casts = bankScanner.castsFromSeaweed();
			name1 = "Sand";
			name2 = "Astrals";
			need1 = bankScanner.sandNeededForCasts(casts);
			need2 = bankScanner.astralsNeededForCasts(casts);
			break;
		case 1: // Use all sand
			balanceHeaderLabel.setText("To Use All Sand \u25B6");
			casts = bankScanner.castsFromSand();
			name1 = "Seaweed";
			name2 = "Astrals";
			need1 = bankScanner.seaweedNeededForCasts(casts);
			need2 = bankScanner.astralsNeededForCasts(casts);
			break;
		default: // Use all astrals
			balanceHeaderLabel.setText("To Use All Astrals \u25B6");
			casts = bankScanner.castsFromAstrals();
			name1 = "Seaweed";
			name2 = "Sand";
			need1 = bankScanner.seaweedNeededForCasts(casts);
			need2 = bankScanner.sandNeededForCasts(casts);
			break;
	}

	bankBalanceName1.setText(name1);
	bankBalanceName2.setText(name2);
	setDeficit(bankBalanceLabel1, need1, "OK");
	setDeficit(bankBalanceLabel2, need2, "OK");
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

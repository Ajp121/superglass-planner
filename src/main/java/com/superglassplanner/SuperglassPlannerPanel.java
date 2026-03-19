package com.superglassplanner;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.ProgressBar;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;

/**
 * Side panel styled to match native RuneLite panels.
 * Follows Quest Helper's approach: plain Swing + FlatLaf theme.
 */
public class SuperglassPlannerPanel extends PluginPanel
{
	private static final String CONFIG_GROUP = "superglassplanner";
	private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance();
	private static final Color HEADER_COLOR = new Color(200, 180, 130);
	private static final Color VALUE_HIGHLIGHT = new Color(190, 210, 255);

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
	private final JLabel targetLabel = new JLabel("Target Level:");
	private final JCheckBox pickupCheckbox = new JCheckBox("Pickup extra glass");
	private final JCheckBox existingGlassCheckbox = new JCheckBox("Factor existing glass");

	// Session labels
	private final JLabel sessionCastsLabel = valueLabel();
	private final JLabel sessionGlassLabel = valueLabel();
	private final JLabel sessionCraftXpLabel = valueLabel();
	private final JLabel sessionMagicXpLabel = valueLabel();

	private JPanel bankDataPanel;
	private JPanel bankNotLoadedPanel;
	private boolean updatingControls = false;

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

		// Use default PluginPanel (with scroll pane)
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		// All content in a vertical box, placed at NORTH so it doesn't stretch
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		mainPanel.add(createTitle());
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(createBankSection());
		mainPanel.add(createGoalSection());
		mainPanel.add(createSessionSection());

		add(mainPanel, BorderLayout.NORTH);

		initGoalControls();

		// Force components to adopt FlatLaf theme (they may have been
		// created before RuneLite's LAF was fully initialized)
		SwingUtilities.updateComponentTreeUI(this);

		// Re-apply dark styling AFTER updateComponentTreeUI
		styleCombo(goalTypeCombo);
		styleCombo(glassItemCombo);
	}

	private static JLabel valueLabel()
	{
		JLabel l = new JLabel();
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(Color.WHITE);
		l.setHorizontalAlignment(SwingConstants.RIGHT);
		return l;
	}

	/**
	 * Quest Helper style: setFocusable(false), setForeground(WHITE),
	 * dark background, and a simple dark renderer.
	 */
	private static <T> void styleCombo(JComboBox<T> combo)
	{
		combo.setFocusable(false);
		combo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		combo.setForeground(Color.WHITE);
		combo.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(
				JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setBackground(isSelected ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
				setForeground(isSelected ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
				setBorder(new EmptyBorder(2, 4, 2, 4));
				return this;
			}
		});
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
		});

		glassItemCombo.addActionListener(e ->
		{
			if (updatingControls) return;
			configManager.setConfiguration(CONFIG_GROUP, "glassItem", glassItemCombo.getSelectedItem());
		});

		pickupCheckbox.addActionListener(e ->
		{
			if (updatingControls) return;
			configManager.setConfiguration(CONFIG_GROUP, "pickupExtraGlass", pickupCheckbox.isSelected());
		});

		existingGlassCheckbox.addActionListener(e ->
		{
			if (updatingControls) return;
			configManager.setConfiguration(CONFIG_GROUP, "factorExistingGlass", existingGlassCheckbox.isSelected());
		});
	}

	private void syncTargetSpinner()
	{
		GoalType type = (GoalType) goalTypeCombo.getSelectedItem();
		switch (type)
		{
			case TARGET_LEVEL:
				targetLabel.setText("Target Level:");
				targetSpinner.setModel(new SpinnerNumberModel(config.targetLevel(), 1, 99, 1));
				break;
			case TARGET_XP:
				targetLabel.setText("Target XP:");
				targetSpinner.setModel(new SpinnerNumberModel(config.targetXp(), 0, 200_000_000, 1000));
				break;
			case TARGET_GLASS:
				targetLabel.setText("Target Glass:");
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
		p.setBackground(ColorScheme.DARK_GRAY_COLOR);
		p.setAlignmentX(LEFT_ALIGNMENT);
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

		JLabel title = new JLabel("Superglass Planner");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(ColorScheme.BRAND_ORANGE);
		p.add(title, BorderLayout.WEST);
		return p;
	}

	private JPanel createBankSection()
	{
		JPanel wrapper = verticalBox();

		wrapper.add(sectionHeader("Bank Materials"));

		// Not loaded — compact message
		bankNotLoadedPanel = new JPanel();
		bankNotLoadedPanel.setLayout(new BoxLayout(bankNotLoadedPanel, BoxLayout.Y_AXIS));
		bankNotLoadedPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		bankNotLoadedPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
		bankNotLoadedPanel.setAlignmentX(LEFT_ALIGNMENT);
		JLabel msg = new JLabel("Open your bank to scan materials");
		msg.setFont(FontManager.getRunescapeSmallFont());
		msg.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		msg.setAlignmentX(LEFT_ALIGNMENT);
		bankNotLoadedPanel.add(msg);
		wrapper.add(bankNotLoadedPanel);

		// Loaded
		bankDataPanel = contentPanel();
		bankDataPanel.setVisible(false);
		bankDataPanel.add(row("Giant Seaweed", giantSeaweedLabel));
		bankDataPanel.add(row("Bucket of Sand", bucketOfSandLabel));
		bankDataPanel.add(row("Molten Glass", moltenGlassLabel));
		bankDataPanel.add(row("Astral Runes", astralRuneLabel));
		bankDataPanel.add(divider());
		bankDataPanel.add(row("Possible Casts", possibleCastsLabel));
		bankDataPanel.add(row("Est. Glass", estimatedGlassLabel));
		bankDataPanel.add(divider());

		JLabel bal = new JLabel("Material Balance");
		bal.setFont(FontManager.getRunescapeSmallFont());
		bal.setForeground(HEADER_COLOR);
		bal.setAlignmentX(LEFT_ALIGNMENT);
		bankDataPanel.add(bal);
		bankDataPanel.add(Box.createVerticalStrut(3));
		bankDataPanel.add(row("Seaweed", bankSeaweedNeededLabel));
		bankDataPanel.add(row("Sand", bankSandNeededLabel));
		bankDataPanel.add(row("Astrals", bankAstralsNeededLabel));

		wrapper.add(bankDataPanel);
		return wrapper;
	}

	private JPanel createGoalSection()
	{
		JPanel wrapper = verticalBox();

		goalHeaderLabel.setFont(FontManager.getRunescapeSmallFont());
		goalHeaderLabel.setForeground(Color.WHITE);
		wrapper.add(sectionHeader(goalHeaderLabel));

		JPanel c = contentPanel();

		// Quest Helper style: label on left, combo on right via BorderLayout
		c.add(dropdownRow("Goal Type:", goalTypeCombo));
		c.add(Box.createVerticalStrut(4));
		c.add(dropdownRow(targetLabel, targetSpinner));
		c.add(Box.createVerticalStrut(4));
		c.add(dropdownRow("Glass Item:", glassItemCombo));
		c.add(Box.createVerticalStrut(6));
		c.add(pickupCheckbox);
		c.add(Box.createVerticalStrut(2));
		c.add(existingGlassCheckbox);
		c.add(divider());

		c.add(row("XP Remaining", xpRemainingLabel));
		c.add(row("Glass Needed", glassNeededLabel));
		c.add(row("Casts Needed", castsNeededLabel));
		c.add(row("Seaweed Deficit", seaweedDeficitLabel));
		c.add(row("Sand Deficit", sandDeficitLabel));
		c.add(Box.createVerticalStrut(6));

		goalProgressBar.setMaximumValue(100);
		goalProgressBar.setValue(0);
		goalProgressBar.setCenterLabel("0.0%");
		goalProgressBar.setForeground(new Color(60, 180, 70));
		goalProgressBar.setBackground(new Color(60, 60, 60));
		goalProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
		goalProgressBar.setPreferredSize(new Dimension(0, 16));
		goalProgressBar.setAlignmentX(LEFT_ALIGNMENT);
		c.add(goalProgressBar);

		wrapper.add(c);
		return wrapper;
	}

	private JPanel createSessionSection()
	{
		JPanel wrapper = verticalBox();
		wrapper.add(sectionHeader("Session Stats"));

		JPanel c = contentPanel();
		c.add(row("Casts", sessionCastsLabel));
		c.add(row("Glass Made", sessionGlassLabel));
		c.add(row("Crafting XP", sessionCraftXpLabel));
		c.add(row("Magic XP", sessionMagicXpLabel));
		c.add(Box.createVerticalStrut(6));

		JButton resetButton = new JButton("Reset Session");
		resetButton.setFocusPainted(false);
		resetButton.setAlignmentX(LEFT_ALIGNMENT);
		resetButton.addActionListener(e -> sessionTracker.reset());
		c.add(resetButton);

		wrapper.add(c);
		return wrapper;
	}

	// ---- Reusable layout helpers ----

	private JPanel verticalBox()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(ColorScheme.DARK_GRAY_COLOR);
		p.setAlignmentX(LEFT_ALIGNMENT);
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
		h.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		h.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(8, 0, 0, 0, ColorScheme.DARK_GRAY_COLOR),
			new EmptyBorder(5, 10, 5, 10)
		));
		h.setAlignmentX(LEFT_ALIGNMENT);
		h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

		label.setFont(FontManager.getRunescapeBoldFont());
		label.setForeground(HEADER_COLOR);
		h.add(label, BorderLayout.WEST);
		return h;
	}

	private JPanel contentPanel()
	{
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		p.setBorder(new EmptyBorder(6, 10, 8, 10));
		p.setAlignmentX(LEFT_ALIGNMENT);
		return p;
	}

	private JPanel row(String label, JLabel value)
	{
		JPanel r = new JPanel(new BorderLayout());
		r.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
		r.setAlignmentX(LEFT_ALIGNMENT);

		JLabel name = new JLabel(label);
		name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		name.setFont(FontManager.getRunescapeSmallFont());
		r.add(name, BorderLayout.WEST);
		r.add(value, BorderLayout.EAST);
		return r;
	}

	/**
	 * Quest Helper style: label CENTER, control EAST (fixed width).
	 */
	private JPanel dropdownRow(String label, JComponent control)
	{
		return dropdownRow(new JLabel(label), control);
	}

	private JPanel dropdownRow(JLabel label, JComponent control)
	{
		JPanel r = new JPanel(new BorderLayout());
		r.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		r.setBorder(new EmptyBorder(0, 0, 0, 0));
		r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
		r.setAlignmentX(LEFT_ALIGNMENT);

		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(FontManager.getRunescapeSmallFont());

		r.add(label, BorderLayout.CENTER);
		r.add(control, BorderLayout.EAST);
		return r;
	}

	private Component divider()
	{
		JPanel d = new JPanel();
		d.setLayout(new BoxLayout(d, BoxLayout.Y_AXIS));
		d.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		d.setAlignmentX(LEFT_ALIGNMENT);
		d.add(Box.createVerticalStrut(4));
		JSeparator sep = new JSeparator();
		sep.setForeground(new Color(60, 60, 60));
		sep.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
		d.add(sep);
		d.add(Box.createVerticalStrut(4));
		return d;
	}

	// ---- Update ----

	public void update()
	{
		goalHeaderLabel.setText("Goal: " + goalCalculator.goalDescription());

		if (bankScanner.isBankLoaded())
		{
			bankNotLoadedPanel.setVisible(false);
			bankDataPanel.setVisible(true);

			giantSeaweedLabel.setText(FORMAT.format(bankScanner.getGiantSeaweedCount()));
			bucketOfSandLabel.setText(FORMAT.format(bankScanner.getBucketOfSandCount()));

			int totalGlass = bankScanner.totalMoltenGlass();
			String glassText = FORMAT.format(totalGlass);
			if (bankScanner.getLootingBagGlassCount() > 0)
			{
				glassText += " (" + bankScanner.getLootingBagGlassCount() + " bag)";
			}
			moltenGlassLabel.setText(glassText);

			int totalAstrals = bankScanner.totalAstralRunes();
			String astralText = FORMAT.format(totalAstrals);
			if (bankScanner.getRunePouchAstralCount() > 0)
			{
				astralText += " (" + bankScanner.getRunePouchAstralCount() + " pouch)";
			}
			astralRuneLabel.setText(astralText);

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
			bankNotLoadedPanel.setVisible(true);
			bankDataPanel.setVisible(false);
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

		sessionCastsLabel.setText(FORMAT.format(sessionTracker.getCastCount()));
		sessionGlassLabel.setText(FORMAT.format(sessionTracker.getGlassProduced()));
		sessionCraftXpLabel.setText(FORMAT.format(sessionTracker.getCraftingXpGained()));
		sessionMagicXpLabel.setText(FORMAT.format(sessionTracker.getMagicXpGained()));
	}

	private void setDeficit(JLabel label, int deficit, String goodText)
	{
		if (deficit > 0)
		{
			label.setText("+" + FORMAT.format(deficit));
			label.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		}
		else
		{
			label.setText(goodText);
			label.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		}
	}
}

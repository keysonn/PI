package com.trafficsimulation.gui;

import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.simulation.DistributionLaw;
import com.trafficsimulation.simulation.SimulationParameters;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class ModelingSettingsDialog extends JDialog {

    private SimulationParameters params;
    private boolean settingsSaved = false;

    private JButton saveButton;

    // --- Компоненты для СКОРОСТИ ---
    private JToggleButton speedDeterministicRadio, speedRandomRadio;
    private ButtonGroup speedFlowTypeGroup;
    private JPanel speedSettingsPanel;

    private JLabel speedFlowTypeLabel;

    private JLabel speedDetLabel;
    private JSpinner speedDetValueSpinner;
    private JLabel speedDetUnitLabel;

    private JLabel speedLawLabel;
    private JToggleButton speedLawUniform, speedLawNormal, speedLawExponential;
    private ButtonGroup speedLawGroup;

    private JLabel speedParam1DisplayLabel, speedParam2DisplayLabel;
    private JSpinner speedParam1Spinner, speedParam2Spinner;
    private JLabel speedUnit1DisplayLabel, speedUnit2DisplayLabel;

    // --- Компоненты для ВРЕМЕНИ ---
    private JToggleButton timeDeterministicRadio, timeRandomRadio;
    private ButtonGroup timeFlowTypeGroup;
    private JPanel timeSettingsPanel;

    private JLabel timeFlowTypeLabel;

    private JLabel timeDetLabel;
    private JSpinner timeDetValueSpinner;
    private JLabel timeDetUnitLabel;

    private JLabel timeLawLabel;
    private JToggleButton timeLawUniform, timeLawNormal, timeLawExponential;
    private ButtonGroup timeLawGroup;

    private JLabel timeParam1DisplayLabel, timeParam2DisplayLabel;
    private JSpinner timeRandomParam1Spinner, timeRandomParam2Spinner;
    private JLabel timeRandomUnit1DisplayLabel, timeRandomUnit2DisplayLabel;

    private static final boolean DETAILED_LOGGING_MSD = false;
    private final Dimension spinnerPreferredSize = new Dimension(70, 25);
    private final Dimension dialogBaseMinimumSize = new Dimension(630, 280);


    public ModelingSettingsDialog(Frame owner, SimulationParameters currentParams) {
        super(owner, "Настройки параметров моделирования", true);
        this.params = currentParams;

        initComponents();
        layoutComponents();
        addListeners();

        setupModelsAndLoadParameters();

        updateSpeedParameterFields();
        updateTimeParameterFields();

        pack();
        Dimension packed = getSize();
        setMinimumSize(new Dimension(Math.max(packed.width, dialogBaseMinimumSize.width),
                Math.max(packed.height, dialogBaseMinimumSize.height)));
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        speedFlowTypeLabel = new JLabel("Выберите тип потока:");
        speedDeterministicRadio = new JToggleButton("Детерминированный");
        speedRandomRadio = new JToggleButton("Случайный");
        speedFlowTypeGroup = new ButtonGroup();
        speedFlowTypeGroup.add(speedDeterministicRadio); speedFlowTypeGroup.add(speedRandomRadio);

        speedDetLabel = new JLabel("Задайте скорость:");
        speedDetValueSpinner = new JSpinner(new SpinnerNumberModel(60.0, 0.0, 200.0, 1.0));
        speedDetValueSpinner.setPreferredSize(spinnerPreferredSize);
        speedDetUnitLabel = new JLabel("км/ч");

        speedLawLabel = new JLabel("Выберите закон распределения:");
        speedLawUniform = new JToggleButton("Равномерный");
        speedLawNormal = new JToggleButton("Нормальный");
        speedLawExponential = new JToggleButton("Показательный");
        speedLawGroup = new ButtonGroup();
        speedLawGroup.add(speedLawUniform); speedLawGroup.add(speedLawNormal); speedLawGroup.add(speedLawExponential);

        this.speedParam1DisplayLabel = new JLabel("Параметр 1:");
        this.speedParam1Spinner = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1));
        this.speedParam1Spinner.setPreferredSize(spinnerPreferredSize);
        this.speedUnit1DisplayLabel = new JLabel("ед.");
        this.speedParam2DisplayLabel = new JLabel("Параметр 2:");
        this.speedParam2Spinner = new JSpinner(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.1));
        this.speedParam2Spinner.setPreferredSize(spinnerPreferredSize);
        this.speedUnit2DisplayLabel = new JLabel("ед.");

        timeFlowTypeLabel = new JLabel("Выберите тип потока:");
        timeDeterministicRadio = new JToggleButton("Детерминированный");
        timeRandomRadio = new JToggleButton("Случайный");
        timeFlowTypeGroup = new ButtonGroup();
        timeFlowTypeGroup.add(timeDeterministicRadio); timeFlowTypeGroup.add(timeRandomRadio);

        this.timeDetLabel = new JLabel("Задайте интервал появления автомобилей:");
        this.timeDetValueSpinner = new JSpinner(new SpinnerNumberModel(3.0, 1.0, 8.0, 0.5));
        this.timeDetValueSpinner.setPreferredSize(spinnerPreferredSize);
        this.timeDetUnitLabel = new JLabel("сек");

        timeLawLabel = new JLabel("Выберите закон распределения:");
        timeLawUniform = new JToggleButton("Равномерный");
        timeLawNormal = new JToggleButton("Нормальный");
        timeLawExponential = new JToggleButton("Показательный");
        timeLawGroup = new ButtonGroup();
        timeLawGroup.add(timeLawUniform); timeLawGroup.add(timeLawNormal); timeLawGroup.add(timeLawExponential);

        this.timeParam1DisplayLabel = new JLabel("Параметр 1:");
        this.timeRandomParam1Spinner = new JSpinner(new SpinnerNumberModel(0.0,-1000.0,1000.0,0.1));
        this.timeRandomParam1Spinner.setPreferredSize(spinnerPreferredSize);
        this.timeRandomUnit1DisplayLabel = new JLabel("ед.");
        this.timeParam2DisplayLabel = new JLabel("Параметр 2:");
        this.timeRandomParam2Spinner = new JSpinner(new SpinnerNumberModel(0.0,-1000.0,1000.0,0.1));
        this.timeRandomParam2Spinner.setPreferredSize(spinnerPreferredSize);
        this.timeRandomUnit2DisplayLabel = new JLabel("ед.");

        saveButton = new JButton("Сохранить настройки");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Панель настроек скорости с GridBagLayout ---
        speedSettingsPanel = new JPanel(new GridBagLayout());
        speedSettingsPanel.setBorder(BorderFactory.createTitledBorder("Настройка скоростного режима"));
        GridBagConstraints gbcSpeed = new GridBagConstraints();
        gbcSpeed.insets = new Insets(3, 5, 3, 5);
        gbcSpeed.anchor = GridBagConstraints.WEST;

        // Строка 0: Выберите тип потока
        gbcSpeed.gridx = 0; gbcSpeed.gridy = 0; gbcSpeed.fill = GridBagConstraints.NONE; gbcSpeed.weightx = 0.1;
        speedSettingsPanel.add(speedFlowTypeLabel, gbcSpeed);

        JPanel speedFlowTypeButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        speedFlowTypeButtonsPanel.add(speedDeterministicRadio); speedFlowTypeButtonsPanel.add(speedRandomRadio);
        gbcSpeed.gridx = 1; gbcSpeed.gridwidth = 2;
        gbcSpeed.anchor = GridBagConstraints.EAST; gbcSpeed.fill = GridBagConstraints.NONE; gbcSpeed.weightx = 0.9;
        speedSettingsPanel.add(speedFlowTypeButtonsPanel, gbcSpeed);
        gbcSpeed.gridwidth = 1; gbcSpeed.weightx = 0;

        // Строка 1: Детерминированная скорость
        gbcSpeed.gridy = 1;
        gbcSpeed.gridx = 0; gbcSpeed.anchor = GridBagConstraints.WEST; gbcSpeed.fill = GridBagConstraints.HORIZONTAL; gbcSpeed.weightx = 0.3;
        speedSettingsPanel.add(speedDetLabel, gbcSpeed);
        gbcSpeed.gridx = 1; gbcSpeed.anchor = GridBagConstraints.EAST; gbcSpeed.fill = GridBagConstraints.NONE; gbcSpeed.weightx = 0.0;
        speedSettingsPanel.add(speedDetValueSpinner, gbcSpeed);
        gbcSpeed.gridx = 2; gbcSpeed.anchor = GridBagConstraints.WEST; gbcSpeed.fill = GridBagConstraints.NONE; gbcSpeed.weightx = 0.0;
        speedSettingsPanel.add(speedDetUnitLabel, gbcSpeed);

        // Строка 2: Выберите закон распределения (для случайного)
        gbcSpeed.gridy = 2;
        gbcSpeed.gridx = 0; gbcSpeed.anchor = GridBagConstraints.WEST; gbcSpeed.fill = GridBagConstraints.HORIZONTAL; gbcSpeed.weightx = 0.1;
        speedSettingsPanel.add(speedLawLabel, gbcSpeed);
        JPanel speedLawButtonsInnerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        speedLawButtonsInnerPanel.add(speedLawUniform); speedLawButtonsInnerPanel.add(speedLawNormal); speedLawButtonsInnerPanel.add(speedLawExponential);
        gbcSpeed.gridx = 1; gbcSpeed.gridwidth = 2; gbcSpeed.anchor = GridBagConstraints.EAST; gbcSpeed.fill = GridBagConstraints.NONE; gbcSpeed.weightx = 0.9;
        speedSettingsPanel.add(speedLawButtonsInnerPanel, gbcSpeed);
        gbcSpeed.gridwidth = 1; gbcSpeed.weightx = 0;

        // Строка 3: Параметр 1 для случайной скорости
        gbcSpeed.gridy = 3;
        gbcSpeed.gridx = 0; gbcSpeed.anchor = GridBagConstraints.WEST; gbcSpeed.fill = GridBagConstraints.HORIZONTAL; gbcSpeed.weightx = 0.3;
        speedSettingsPanel.add(this.speedParam1DisplayLabel, gbcSpeed);
        gbcSpeed.gridx = 1; gbcSpeed.anchor = GridBagConstraints.EAST; gbcSpeed.fill = GridBagConstraints.NONE; gbcSpeed.weightx = 0.0;
        speedSettingsPanel.add(this.speedParam1Spinner, gbcSpeed);
        gbcSpeed.gridx = 2; gbcSpeed.anchor = GridBagConstraints.WEST; gbcSpeed.fill = GridBagConstraints.NONE; gbcSpeed.weightx = 0.0;
        speedSettingsPanel.add(this.speedUnit1DisplayLabel, gbcSpeed);

        // Строка 4: Параметр 2 для случайной скорости
        gbcSpeed.gridy = 4;
        gbcSpeed.gridx = 0; gbcSpeed.anchor = GridBagConstraints.WEST; gbcSpeed.fill = GridBagConstraints.HORIZONTAL; gbcSpeed.weightx = 0.3;
        speedSettingsPanel.add(this.speedParam2DisplayLabel, gbcSpeed);
        gbcSpeed.gridx = 1; gbcSpeed.anchor = GridBagConstraints.EAST; gbcSpeed.fill = GridBagConstraints.NONE; gbcSpeed.weightx = 0.0;
        speedSettingsPanel.add(this.speedParam2Spinner, gbcSpeed);
        gbcSpeed.gridx = 2; gbcSpeed.anchor = GridBagConstraints.WEST; gbcSpeed.fill = GridBagConstraints.NONE; gbcSpeed.weightx = 0.0;
        speedSettingsPanel.add(this.speedUnit2DisplayLabel, gbcSpeed);

        gbcSpeed.gridy = 5; gbcSpeed.gridx = 0; gbcSpeed.weighty = 1.0; gbcSpeed.fill = GridBagConstraints.VERTICAL;
        speedSettingsPanel.add(Box.createVerticalGlue(), gbcSpeed);
        mainPanel.add(speedSettingsPanel);

        // --- Панель настроек времени (Аналогично) ---
        timeSettingsPanel = new JPanel(new GridBagLayout());
        timeSettingsPanel.setBorder(BorderFactory.createTitledBorder("Настройка времени появления"));
        GridBagConstraints gbcTime = new GridBagConstraints();
        gbcTime.insets = new Insets(3, 5, 3, 5);
        gbcTime.anchor = GridBagConstraints.WEST;

        gbcTime.gridx = 0; gbcTime.gridy = 0; gbcTime.fill = GridBagConstraints.NONE; gbcTime.weightx = 0.1;
        timeSettingsPanel.add(timeFlowTypeLabel, gbcTime);
        JPanel timeFlowTypeButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        timeFlowTypeButtonsPanel.add(timeDeterministicRadio); timeFlowTypeButtonsPanel.add(timeRandomRadio);
        gbcTime.gridx = 1; gbcTime.gridwidth = 2; gbcTime.anchor = GridBagConstraints.EAST; gbcTime.fill = GridBagConstraints.NONE; gbcTime.weightx = 0.9;
        timeSettingsPanel.add(timeFlowTypeButtonsPanel, gbcTime);
        gbcTime.gridwidth = 1; gbcTime.weightx = 0;

        gbcTime.gridy = 1;
        gbcTime.gridx = 0; gbcTime.anchor = GridBagConstraints.WEST; gbcTime.fill = GridBagConstraints.HORIZONTAL; gbcTime.weightx = 0.3;
        timeSettingsPanel.add(this.timeDetLabel, gbcTime);
        gbcTime.gridx = 1; gbcTime.anchor = GridBagConstraints.EAST; gbcTime.fill = GridBagConstraints.NONE; gbcTime.weightx = 0.0;
        timeSettingsPanel.add(this.timeDetValueSpinner, gbcTime);
        gbcTime.gridx = 2; gbcTime.anchor = GridBagConstraints.WEST; gbcTime.fill = GridBagConstraints.NONE; gbcTime.weightx = 0.0;
        timeSettingsPanel.add(this.timeDetUnitLabel, gbcTime);

        gbcTime.gridy = 2;
        gbcTime.gridx = 0; gbcTime.anchor = GridBagConstraints.WEST; gbcTime.fill = GridBagConstraints.HORIZONTAL; gbcTime.weightx = 0.1;
        timeSettingsPanel.add(timeLawLabel, gbcTime);
        JPanel timeLawButtonsInnerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        timeLawButtonsInnerPanel.add(timeLawUniform); timeLawButtonsInnerPanel.add(timeLawNormal); timeLawButtonsInnerPanel.add(timeLawExponential);
        gbcTime.gridx = 1; gbcTime.gridwidth = 2; gbcTime.anchor = GridBagConstraints.EAST; gbcTime.fill = GridBagConstraints.NONE; gbcTime.weightx = 0.9;
        timeSettingsPanel.add(timeLawButtonsInnerPanel, gbcTime);
        gbcTime.gridwidth = 1; gbcTime.weightx = 0;

        gbcTime.gridy = 3;
        gbcTime.gridx = 0; gbcTime.anchor = GridBagConstraints.WEST; gbcTime.fill = GridBagConstraints.HORIZONTAL; gbcTime.weightx = 0.3;
        timeSettingsPanel.add(this.timeParam1DisplayLabel, gbcTime);
        gbcTime.gridx = 1; gbcTime.anchor = GridBagConstraints.EAST; gbcTime.fill = GridBagConstraints.NONE; gbcTime.weightx = 0.0;
        timeSettingsPanel.add(this.timeRandomParam1Spinner, gbcTime);
        gbcTime.gridx = 2; gbcTime.anchor = GridBagConstraints.WEST; gbcTime.fill = GridBagConstraints.NONE; gbcTime.weightx = 0.0;
        timeSettingsPanel.add(this.timeRandomUnit1DisplayLabel, gbcTime);

        gbcTime.gridy = 4;
        gbcTime.gridx = 0; gbcTime.anchor = GridBagConstraints.WEST; gbcTime.fill = GridBagConstraints.HORIZONTAL; gbcTime.weightx = 0.3;
        timeSettingsPanel.add(this.timeParam2DisplayLabel, gbcTime);
        gbcTime.gridx = 1; gbcTime.anchor = GridBagConstraints.EAST; gbcTime.fill = GridBagConstraints.NONE; gbcTime.weightx = 0.0;
        timeSettingsPanel.add(this.timeRandomParam2Spinner, gbcTime);
        gbcTime.gridx = 2; gbcTime.anchor = GridBagConstraints.WEST; gbcTime.fill = GridBagConstraints.NONE; gbcTime.weightx = 0.0;
        timeSettingsPanel.add(this.timeRandomUnit2DisplayLabel, gbcTime);

        gbcTime.gridy = 5; gbcTime.gridx = 0; gbcTime.weighty = 1.0; gbcTime.fill = GridBagConstraints.VERTICAL;
        timeSettingsPanel.add(Box.createVerticalGlue(), gbcTime);

        mainPanel.add(timeSettingsPanel);
        add(mainPanel, BorderLayout.CENTER);

        JPanel buttonPanelSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
        saveButton.addActionListener(e -> saveAndClose());
        buttonPanelSouth.add(saveButton);
        add(buttonPanelSouth, BorderLayout.SOUTH);
    }

    private void addListeners() {
        ActionListener updateAllFieldsListener = e -> {
            updateSpeedParameterFields();
            updateTimeParameterFields();
        };
        speedDeterministicRadio.addActionListener(updateAllFieldsListener);
        speedRandomRadio.addActionListener(updateAllFieldsListener);
        speedLawUniform.addActionListener(updateAllFieldsListener);
        speedLawNormal.addActionListener(updateAllFieldsListener);
        speedLawExponential.addActionListener(updateAllFieldsListener);

        timeDeterministicRadio.addActionListener(updateAllFieldsListener);
        timeRandomRadio.addActionListener(updateAllFieldsListener);
        timeLawUniform.addActionListener(updateAllFieldsListener);
        timeLawNormal.addActionListener(updateAllFieldsListener);
        timeLawExponential.addActionListener(updateAllFieldsListener);
    }

    private void setupModelsAndLoadParameters() {
        RoadType currentRoadType = params.getRoadType();
        if (DETAILED_LOGGING_MSD) System.out.println("[DEBUG] MSD.setupModelsAndLoad: RoadType from params = " + currentRoadType);
        if (currentRoadType == null) {
            currentRoadType = RoadType.CITY_ROAD;
            if (DETAILED_LOGGING_MSD) System.out.println("[DEBUG] MSD.setupModelsAndLoad: RoadType was null, defaulting to CITY_ROAD.");
        }
        double minSpeed = currentRoadType.getMinSpeedLimitKmh();
        double maxSpeed = currentRoadType.getMaxSpeedLimitKmh();

        double detSpeedVal = params.getDeterministicSpeedKmh();
        detSpeedVal = clamp(detSpeedVal, minSpeed, maxSpeed);
        speedDetValueSpinner.setModel(new SpinnerNumberModel(detSpeedVal, minSpeed, maxSpeed, 1.0));

        speedDeterministicRadio.setSelected(!params.isRandomSpeedFlow());
        speedRandomRadio.setSelected(params.isRandomSpeedFlow());

        DistributionLaw speedLaw = params.getSpeedDistributionLaw();
        if (params.isRandomSpeedFlow() && speedLaw == null) speedLaw = DistributionLaw.NORMAL;
        speedLawUniform.setSelected(speedLaw == DistributionLaw.UNIFORM);
        speedLawNormal.setSelected(speedLaw == DistributionLaw.NORMAL);
        speedLawExponential.setSelected(speedLaw == DistributionLaw.EXPONENTIAL);
        if (params.isRandomSpeedFlow() && !speedLawUniform.isSelected() && !speedLawNormal.isSelected() && !speedLawExponential.isSelected()) {
            speedLawNormal.setSelected(true);
        }

        timeDeterministicRadio.setSelected(!params.isRandomTimeFlow());
        timeRandomRadio.setSelected(params.isRandomTimeFlow());
        timeDetValueSpinner.setValue(clamp(params.getDeterministicIntervalSeconds(),1.0, 8.0));

        DistributionLaw timeLaw = params.getTimeDistributionLaw();
        if (params.isRandomTimeFlow() && timeLaw == null) timeLaw = DistributionLaw.NORMAL;
        timeLawUniform.setSelected(timeLaw == DistributionLaw.UNIFORM);
        timeLawNormal.setSelected(timeLaw == DistributionLaw.NORMAL);
        timeLawExponential.setSelected(timeLaw == DistributionLaw.EXPONENTIAL);
        if (params.isRandomTimeFlow() && !timeLawUniform.isSelected() && !timeLawNormal.isSelected() && !timeLawExponential.isSelected()) {
            timeLawNormal.setSelected(true);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private void updateSpeedParameterFields() {
        boolean isDeterministic = speedDeterministicRadio.isSelected();

        speedDetLabel.setVisible(isDeterministic);
        speedDetValueSpinner.setVisible(isDeterministic);
        speedDetUnitLabel.setVisible(isDeterministic);

        speedLawLabel.setVisible(!isDeterministic);
        speedLawUniform.setVisible(!isDeterministic);
        speedLawNormal.setVisible(!isDeterministic);
        speedLawExponential.setVisible(!isDeterministic);

        setSpeedRandomInnerFieldsVisible(false, false);

        if (!isDeterministic) {
            RoadType currentRoadType = params.getRoadType();
            if (currentRoadType == null) currentRoadType = RoadType.CITY_ROAD;
            double minRTypeSpeed = currentRoadType.getMinSpeedLimitKmh();
            double maxRTypeSpeed = currentRoadType.getMaxSpeedLimitKmh();

            if (speedLawUniform.isSelected()) {
                this.speedParam1DisplayLabel.setText("Укажите нижнюю границу:");
                double val1 = clamp(params.getSpeedUniformMinKmh(), minRTypeSpeed, maxRTypeSpeed);
                this.speedParam1Spinner.setModel(new SpinnerNumberModel(val1, minRTypeSpeed, maxRTypeSpeed, 1.0));
                this.speedUnit1DisplayLabel.setText("км/ч");
                this.speedParam2DisplayLabel.setText("Укажите верхнюю границу:");
                double val2 = clamp(params.getSpeedUniformMaxKmh(), minRTypeSpeed, maxRTypeSpeed);
                if (val1 > val2) val2 = val1;
                val2 = Math.max(val1, val2);
                this.speedParam2Spinner.setModel(new SpinnerNumberModel(val2, minRTypeSpeed, maxRTypeSpeed, 1.0));
                this.speedUnit2DisplayLabel.setText("км/ч");
                setSpeedRandomInnerFieldsVisible(true, true);
            } else if (speedLawNormal.isSelected()) {
                this.speedParam1DisplayLabel.setText("Укажите математическое ожидание:");
                double val1 = clamp(params.getSpeedNormalMeanKmh(), minRTypeSpeed, maxRTypeSpeed);
                this.speedParam1Spinner.setModel(new SpinnerNumberModel(val1, minRTypeSpeed, maxRTypeSpeed, 1.0));
                this.speedUnit1DisplayLabel.setText("км/ч");
                this.speedParam2DisplayLabel.setText("Укажите дисперсию:");
                double varianceVal = clamp(params.getSpeedNormalVarianceKmh(), 0.0, 40.0);
                this.speedParam2Spinner.setModel(new SpinnerNumberModel(varianceVal, 0.0, 40.0, 1.0));
                this.speedUnit2DisplayLabel.setText("(км/ч)²");
                setSpeedRandomInnerFieldsVisible(true, true);
            } else if (speedLawExponential.isSelected()) {
                this.speedParam1DisplayLabel.setText("Задайте интенсивность λ (скорость):");
                double intensityVal = clamp(params.getSpeedExponentialIntensityPerKmh(), 0.007, 0.1);
                this.speedParam1Spinner.setModel(new SpinnerNumberModel(intensityVal, 0.007, 0.1, 0.001));
                ((JSpinner.NumberEditor)this.speedParam1Spinner.getEditor()).getFormat().setMinimumFractionDigits(3);
                this.speedUnit1DisplayLabel.setText("1/(км/ч)");
                setSpeedRandomInnerFieldsVisible(true, false);
            }
        }
        packDialog();
    }

    private void setSpeedRandomInnerFieldsVisible(boolean param1Visible, boolean param2Visible) {
        speedParam1DisplayLabel.setVisible(param1Visible);
        speedParam1Spinner.setVisible(param1Visible);
        speedUnit1DisplayLabel.setVisible(param1Visible);
        speedParam2DisplayLabel.setVisible(param2Visible);
        speedParam2Spinner.setVisible(param2Visible);
        speedUnit2DisplayLabel.setVisible(param2Visible);
    }

    private void updateTimeParameterFields() {
        boolean isDeterministic = timeDeterministicRadio.isSelected();
        timeDetLabel.setVisible(isDeterministic);
        timeDetValueSpinner.setVisible(isDeterministic);
        timeDetUnitLabel.setVisible(isDeterministic);

        timeLawLabel.setVisible(!isDeterministic);
        timeLawUniform.setVisible(!isDeterministic);
        timeLawNormal.setVisible(!isDeterministic);
        timeLawExponential.setVisible(!isDeterministic);

        setTimeRandomInnerFieldsVisible(false, false);

        if (!isDeterministic) {
            if (timeLawUniform.isSelected()) {
                this.timeParam1DisplayLabel.setText("Укажите нижнюю границу:");
                double val1 = clamp(params.getTimeUniformMinSec(), 0.8, 7.0);
                this.timeRandomParam1Spinner.setModel(new SpinnerNumberModel(val1, 0.8, 7.0, 0.1));
                this.timeRandomUnit1DisplayLabel.setText("сек");
                this.timeParam2DisplayLabel.setText("Укажите верхнюю границу:");
                double val2 = clamp(params.getTimeUniformMaxSec(), 1.0, 8.0);
                if(val1 > val2) val2 = val1;
                val2 = Math.max(val1, val2);
                this.timeRandomParam2Spinner.setModel(new SpinnerNumberModel(val2, 1.0, 8.0, 0.1));
                this.timeRandomUnit2DisplayLabel.setText("сек");
                setTimeRandomInnerFieldsVisible(true, true);
            } else if (timeLawNormal.isSelected()) {
                this.timeParam1DisplayLabel.setText("Укажите математическое ожидание:");
                double val1 = clamp(params.getTimeNormalMeanSec(), 1.5, 10.0);
                this.timeRandomParam1Spinner.setModel(new SpinnerNumberModel(val1, 1.5, 10.0, 0.1));
                this.timeRandomUnit1DisplayLabel.setText("секунд");
                this.timeParam2DisplayLabel.setText("Укажите дисперсию:");
                double val2 = clamp(params.getTimeNormalVarianceSec(), 0.0, 5.0);
                this.timeRandomParam2Spinner.setModel(new SpinnerNumberModel(val2, 0.0, 5.0, 0.1));
                this.timeRandomUnit2DisplayLabel.setText("секунд²");
                setTimeRandomInnerFieldsVisible(true, true);
            } else if (timeLawExponential.isSelected()) {
                this.timeParam1DisplayLabel.setText("Задайте интенсивность λ:");
                double val1 = clamp(params.getTimeExponentialIntensityPerSec(), 0.2, 3.0);
                this.timeRandomParam1Spinner.setModel(new SpinnerNumberModel(val1, 0.2, 3.0, 0.1));
                this.timeRandomUnit1DisplayLabel.setText("авто/с");
                setTimeRandomInnerFieldsVisible(true, false);
            }
        }
        packDialog();
    }

    private void setTimeRandomInnerFieldsVisible(boolean param1Visible, boolean param2Visible) {
        timeParam1DisplayLabel.setVisible(param1Visible);
        timeRandomParam1Spinner.setVisible(param1Visible);
        timeRandomUnit1DisplayLabel.setVisible(param1Visible);
        timeParam2DisplayLabel.setVisible(param2Visible);
        timeRandomParam2Spinner.setVisible(param2Visible);
        timeRandomUnit2DisplayLabel.setVisible(param2Visible);
    }

    private void packDialog() {
        SwingUtilities.invokeLater(() -> {
            if(isVisible()) {
                pack();
                Dimension packedSize = getSize();
                Dimension currentMinSize = dialogBaseMinimumSize;
                int finalWidth = Math.max(packedSize.width, currentMinSize.width);
                int finalHeight = Math.max(packedSize.height, currentMinSize.height);
                if (getWidth() != finalWidth || getHeight() != finalHeight) {
                    setSize(finalWidth, finalHeight);
                }
            }
        });
    }

    private void saveAndClose() {
        if (DETAILED_LOGGING_MSD) System.out.println("[DEBUG] MSD.saveAndClose: Validating with RoadType from params = " + params.getRoadType());

        if (speedDeterministicRadio.isSelected()) {
            double detSpeedValue = ((Number) speedDetValueSpinner.getValue()).doubleValue();
            SpinnerNumberModel detSpeedModel = (SpinnerNumberModel) speedDetValueSpinner.getModel();
            if (detSpeedValue < ((Number)detSpeedModel.getMinimum()).doubleValue() || detSpeedValue > ((Number)detSpeedModel.getMaximum()).doubleValue()){
                JOptionPane.showMessageDialog(this, "Детерминированная скорость (" + String.format("%.0f", detSpeedValue) + " км/ч) выходит за допустимые пределы (" + String.format("%.0f", ((Number)detSpeedModel.getMinimum()).doubleValue()) + " - " + String.format("%.0f", ((Number)detSpeedModel.getMaximum()).doubleValue()) + " км/ч) для текущего типа дороги: " + params.getRoadType().getDisplayName() + "!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else if (speedRandomRadio.isSelected()) {
            if (!speedLawUniform.isSelected() && !speedLawNormal.isSelected() && !speedLawExponential.isSelected()){
                JOptionPane.showMessageDialog(this, "Для случайного потока скорости не выбран закон распределения!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE); return;
            }
            if (speedLawUniform.isSelected()) {
                double minSpeed = ((Number) this.speedParam1Spinner.getValue()).doubleValue();
                double maxSpeed = ((Number) this.speedParam2Spinner.getValue()).doubleValue();
                SpinnerNumberModel model1 = (SpinnerNumberModel) this.speedParam1Spinner.getModel();
                SpinnerNumberModel model2 = (SpinnerNumberModel) this.speedParam2Spinner.getModel();
                if (minSpeed < ((Number)model1.getMinimum()).doubleValue() || maxSpeed > ((Number)model2.getMaximum()).doubleValue() || minSpeed > maxSpeed) {
                    JOptionPane.showMessageDialog(this, "Границы равномерного распределения скорости некорректны или выходят за пределы ("+ String.format("%.0f", ((Number)model1.getMinimum()).doubleValue()) +"-"+ String.format("%.0f", ((Number)model2.getMaximum()).doubleValue()) +") для типа дороги: " + params.getRoadType().getDisplayName() + "!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE); return;
                }
            }
            if (speedLawNormal.isSelected()) {
                double meanSpeed = ((Number) this.speedParam1Spinner.getValue()).doubleValue();
                SpinnerNumberModel model1 = (SpinnerNumberModel) this.speedParam1Spinner.getModel();
                if (meanSpeed < ((Number)model1.getMinimum()).doubleValue() || meanSpeed > ((Number)model1.getMaximum()).doubleValue()) {
                    JOptionPane.showMessageDialog(this, "Мат. ожидание скорости ("+ String.format("%.0f", meanSpeed) +") выходит за допустимые пределы ("+ String.format("%.0f", ((Number)model1.getMinimum()).doubleValue()) +"-"+ String.format("%.0f", ((Number)model1.getMaximum()).doubleValue()) +") для типа дороги: " + params.getRoadType().getDisplayName() + "!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE); return;
                }
            }
            if (speedLawExponential.isSelected()){
                double intensity = ((Number) this.speedParam1Spinner.getValue()).doubleValue();
                if (intensity < ((Number)((SpinnerNumberModel)this.speedParam1Spinner.getModel()).getMinimum()).doubleValue() || intensity > ((Number)((SpinnerNumberModel)this.speedParam1Spinner.getModel()).getMaximum()).doubleValue()){
                    JOptionPane.showMessageDialog(this, "Значение интенсивности скорости некорректно!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE); return;
                }
            }
        }
        if (timeRandomRadio.isSelected()) {
            if (!timeLawUniform.isSelected() && !timeLawNormal.isSelected() && !timeLawExponential.isSelected()){
                JOptionPane.showMessageDialog(this, "Для случайного потока времени не выбран закон распределения!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE); return;
            }
            if (timeLawUniform.isSelected()) {
                double minTime = ((Number) this.timeRandomParam1Spinner.getValue()).doubleValue();
                double maxTime = ((Number) this.timeRandomParam2Spinner.getValue()).doubleValue();
                if (minTime > maxTime) {
                    JOptionPane.showMessageDialog(this, "Минимальное время не может быть больше максимального!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE); return;
                }
            }
        }

        params.setRandomSpeedFlow(speedRandomRadio.isSelected());
        if (params.isRandomSpeedFlow()){
            if(speedLawUniform.isSelected()) {
                params.setSpeedDistributionLaw(DistributionLaw.UNIFORM);
                params.setSpeedUniformMinKmh(((Number) this.speedParam1Spinner.getValue()).doubleValue());
                params.setSpeedUniformMaxKmh(((Number) this.speedParam2Spinner.getValue()).doubleValue());
            } else if(speedLawNormal.isSelected()) {
                params.setSpeedDistributionLaw(DistributionLaw.NORMAL);
                params.setSpeedNormalMeanKmh(((Number) this.speedParam1Spinner.getValue()).doubleValue());
                params.setSpeedNormalVarianceKmh(((Number) this.speedParam2Spinner.getValue()).doubleValue());
            } else if(speedLawExponential.isSelected()) {
                params.setSpeedDistributionLaw(DistributionLaw.EXPONENTIAL);
                params.setSpeedExponentialIntensityPerKmh(((Number) this.speedParam1Spinner.getValue()).doubleValue());
            }
        } else {
            params.setDeterministicSpeedKmh(((Number) speedDetValueSpinner.getValue()).doubleValue());
        }
        params.setRandomTimeFlow(timeRandomRadio.isSelected());
        if (params.isRandomTimeFlow()){
            if(timeLawUniform.isSelected()) {
                params.setTimeDistributionLaw(DistributionLaw.UNIFORM);
                params.setTimeUniformMinSec(((Number) this.timeRandomParam1Spinner.getValue()).doubleValue());
                params.setTimeUniformMaxSec(((Number) this.timeRandomParam2Spinner.getValue()).doubleValue());
            } else if(timeLawNormal.isSelected()) {
                params.setTimeDistributionLaw(DistributionLaw.NORMAL);
                params.setTimeNormalMeanSec(((Number) this.timeRandomParam1Spinner.getValue()).doubleValue());
                params.setTimeNormalVarianceSec(((Number) this.timeRandomParam2Spinner.getValue()).doubleValue());
            } else if(timeLawExponential.isSelected()) {
                params.setTimeDistributionLaw(DistributionLaw.EXPONENTIAL);
                params.setTimeExponentialIntensityPerSec(((Number) this.timeRandomParam1Spinner.getValue()).doubleValue());
            }
        } else {
            params.setDeterministicIntervalSeconds(((Number) timeDetValueSpinner.getValue()).doubleValue());
        }
        settingsSaved = true;
        dispose();
    }

    public boolean showDialog() {
        setupModelsAndLoadParameters();
        updateSpeedParameterFields();
        updateTimeParameterFields();
        setVisible(true);
        return settingsSaved;
    }
}
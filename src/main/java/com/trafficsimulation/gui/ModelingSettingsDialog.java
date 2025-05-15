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

    // Скоростной режим
    private JToggleButton speedDeterministicRadio, speedRandomRadio;
    private ButtonGroup speedFlowTypeGroup;
    private JPanel speedDeterministicPanel, speedRandomPanelContainer;
    private JSpinner speedDetValueSpinner;
    // private JLabel speedUnitLabel; // Будет создаваться локально в createSingleParameterPanel
    // private JLabel speedRangeLabel; // УДАЛЕНО

    private JToggleButton speedLawUniform, speedLawNormal, speedLawExponential;
    private ButtonGroup speedLawGroup;
    private JPanel speedParamsUniformPanel, speedParamsNormalPanel, speedParamsExponentialPanel;
    private JSpinner speedUniformMinSpinner, speedUniformMaxSpinner;
    private JSpinner speedNormalMeanSpinner, speedNormalVarianceSpinner;
    private JSpinner speedExponentialIntensitySpinner;

    // Настройка времени
    private JToggleButton timeDeterministicRadio, timeRandomRadio;
    private ButtonGroup timeFlowTypeGroup;
    private JPanel timeDeterministicPanel, timeRandomPanelContainer;
    private JSpinner timeDetValueSpinner;
    private JToggleButton timeLawUniform, timeLawNormal, timeLawExponential;
    private ButtonGroup timeLawGroup;
    private JPanel timeParamsUniformPanel, timeParamsNormalPanel, timeParamsExponentialPanel;
    private JSpinner timeUniformMinSpinner, timeUniformMaxSpinner;
    private JSpinner timeNormalMeanSpinner, timeNormalVarianceSpinner;
    private JSpinner timeExponentialIntensitySpinner;


    public ModelingSettingsDialog(Frame owner, SimulationParameters currentParams) {
        super(owner, "Настройки параметров моделирования", true);
        this.params = currentParams;

        initComponents();
        layoutComponents();
        addListeners();

        pack();
        setMinimumSize(new Dimension(Math.max(650, getPreferredSize().width), getPreferredSize().height)); // Уменьшил ширину немного
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        speedDeterministicRadio = new JToggleButton("Детерминированный");
        speedRandomRadio = new JToggleButton("Случайный");
        speedFlowTypeGroup = new ButtonGroup();
        speedFlowTypeGroup.add(speedDeterministicRadio); speedFlowTypeGroup.add(speedRandomRadio);

        speedDetValueSpinner = new JSpinner(new SpinnerNumberModel(60.0, 20.0, 130.0, 1.0));
        // speedUnitLabel = new JLabel(" км/ч"); // Не нужно как поле класса
        // speedRangeLabel = new JLabel(); // УДАЛЕНО

        speedLawUniform = new JToggleButton("Равномерный");
        speedLawNormal = new JToggleButton("Нормальный");
        speedLawExponential = new JToggleButton("Показательный");
        speedLawGroup = new ButtonGroup();
        speedLawGroup.add(speedLawUniform); speedLawGroup.add(speedLawNormal); speedLawGroup.add(speedLawExponential);

        speedUniformMinSpinner = new JSpinner(new SpinnerNumberModel(40.0, 20.0, 130.0, 1.0));
        speedUniformMaxSpinner = new JSpinner(new SpinnerNumberModel(80.0, 20.0, 130.0, 1.0));
        speedNormalMeanSpinner = new JSpinner(new SpinnerNumberModel(60.0, 20.0, 130.0, 1.0));
        speedNormalVarianceSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 40.0, 1.0));
        speedExponentialIntensitySpinner = new JSpinner(new SpinnerNumberModel(0.02, 0.01, 0.05, 0.001));

        timeDeterministicRadio = new JToggleButton("Детерминированный");
        timeRandomRadio = new JToggleButton("Случайный");
        timeFlowTypeGroup = new ButtonGroup();
        timeFlowTypeGroup.add(timeDeterministicRadio); timeFlowTypeGroup.add(timeRandomRadio);
        timeDetValueSpinner = new JSpinner(new SpinnerNumberModel(12.0, 10.0, 15.0, 0.1));

        timeLawUniform = new JToggleButton("Равномерный");
        timeLawNormal = new JToggleButton("Нормальный");
        timeLawExponential = new JToggleButton("Показательный");
        timeLawGroup = new ButtonGroup();
        timeLawGroup.add(timeLawUniform); timeLawGroup.add(timeLawNormal); timeLawGroup.add(timeLawExponential);

        timeUniformMinSpinner = new JSpinner(new SpinnerNumberModel(10.0, 10.0, 15.0, 0.1));
        timeUniformMaxSpinner = new JSpinner(new SpinnerNumberModel(15.0, 10.0, 15.0, 0.1));
        timeNormalMeanSpinner = new JSpinner(new SpinnerNumberModel(30.0, 20.0, 120.0, 1.0));
        timeNormalVarianceSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 20.0, 0.1));
        timeExponentialIntensitySpinner = new JSpinner(new SpinnerNumberModel(1.0, 1.0, 20.0, 0.1));

        speedParamsUniformPanel = createSingleParameterPanel(new String[]{"Нижняя граница:", "Верхняя граница:"}, new String[]{"км/ч", "км/ч"}, speedUniformMinSpinner, speedUniformMaxSpinner);
        speedParamsNormalPanel = createSingleParameterPanel(new String[]{"Мат. ожидание:", "Дисперсия:"}, new String[]{"км/ч", "(км/ч)²"}, speedNormalMeanSpinner, speedNormalVarianceSpinner);
        speedParamsExponentialPanel = createSingleParameterPanel(new String[]{"Интенсивность λ:"}, new String[]{"1/(км/ч)"}, speedExponentialIntensitySpinner);

        timeParamsUniformPanel = createSingleParameterPanel(new String[]{"Нижняя граница:", "Верхняя граница:"}, new String[]{"сек", "сек"}, timeUniformMinSpinner, timeUniformMaxSpinner);
        timeParamsNormalPanel = createSingleParameterPanel(new String[]{"Мат. ожидание:", "Дисперсия:"}, new String[]{"сек", "сек²"}, timeNormalMeanSpinner, timeNormalVarianceSpinner);
        timeParamsExponentialPanel = createSingleParameterPanel(new String[]{"Интенсивность λ:"}, new String[]{"авто/сек"}, timeExponentialIntensitySpinner);
    }

    private void updateSpeedSpinnersForRoadType() {
        RoadType currentRoadType = params.getRoadType();
        if (currentRoadType == null) {
            currentRoadType = RoadType.CITY_ROAD;
        }

        double minLimitKmh = currentRoadType.getMinSpeedLimitKmh();
        double maxLimitKmh = currentRoadType.getMaxSpeedLimitKmh();

        double currentDetSpeed = ((Number)speedDetValueSpinner.getValue()).doubleValue();
        currentDetSpeed = Math.max(minLimitKmh, Math.min(currentDetSpeed, maxLimitKmh));
        speedDetValueSpinner.setModel(new SpinnerNumberModel(currentDetSpeed, minLimitKmh, maxLimitKmh, 1.0));
        // speedRangeLabel.setText(String.format("(%.0f - %.0f)", minLimitKmh, maxLimitKmh)); // УДАЛЕНО ОТОБРАЖЕНИЕ

        double currentUniformMin = ((Number)speedUniformMinSpinner.getValue()).doubleValue();
        double currentUniformMax = ((Number)speedUniformMaxSpinner.getValue()).doubleValue();
        currentUniformMin = Math.max(minLimitKmh, Math.min(currentUniformMin, maxLimitKmh));
        currentUniformMax = Math.max(minLimitKmh, Math.min(currentUniformMax, maxLimitKmh));
        if (currentUniformMin > currentUniformMax) currentUniformMin = currentUniformMax;
        speedUniformMinSpinner.setModel(new SpinnerNumberModel(currentUniformMin, minLimitKmh, maxLimitKmh, 1.0));
        speedUniformMaxSpinner.setModel(new SpinnerNumberModel(currentUniformMax, minLimitKmh, maxLimitKmh, 1.0));

        double currentNormalMean = ((Number)speedNormalMeanSpinner.getValue()).doubleValue();
        currentNormalMean = Math.max(minLimitKmh, Math.min(currentNormalMean, maxLimitKmh));
        speedNormalMeanSpinner.setModel(new SpinnerNumberModel(currentNormalMean, minLimitKmh, maxLimitKmh, 1.0));
    }

    private JPanel createSingleParameterPanel(String[] labels, String[] units, JComponent... components) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5); gbc.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; panel.add(new JLabel(labels[i]), gbc);
            JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            valuePanel.add(components[i]);

            // if (components[i] == speedDetValueSpinner) { // УДАЛЕНО ОТОБРАЖЕНИЕ ДИАПАЗОНА
            //     // valuePanel.add(speedRangeLabel); // Не добавляем
            //     // valuePanel.add(speedUnitLabel); // Юниты добавляются ниже общим способом
            // }
            // Всегда добавляем юниты, если они есть
            if (units != null && units.length > i && units[i] != null && !units[i].isEmpty()) {
                valuePanel.add(new JLabel(" " + units[i])); // Добавляем пробел перед юнитами
            }
            gbc.gridx = 1; gbc.gridy = i; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; panel.add(valuePanel, gbc);
        }
        return panel;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10,10));
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        mainPanel.add(createSpeedSettingsMainPanel());
        mainPanel.add(createTimeSettingsMainPanel());
        add(mainPanel, BorderLayout.CENTER);

        JButton saveButton = new JButton("Сохранить настройки");
        saveButton.addActionListener(e -> saveAndClose());
        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> {
            settingsSaved = false;
            dispose();
        });
        JPanel buttonPanelSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanelSouth.add(saveButton);
        buttonPanelSouth.add(cancelButton);
        add(buttonPanelSouth, BorderLayout.SOUTH);
    }

    private JPanel createSpeedSettingsMainPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Настройка скоростного режима"));
        JPanel typeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typeSelectionPanel.add(new JLabel("Тип потока:"));
        typeSelectionPanel.add(speedDeterministicRadio); typeSelectionPanel.add(speedRandomRadio);
        mainPanel.add(typeSelectionPanel);

        speedDeterministicPanel = createSingleParameterPanel(new String[]{"Скорость:"}, new String[]{"км/ч"}, speedDetValueSpinner);
        mainPanel.add(speedDeterministicPanel);

        speedRandomPanelContainer = new JPanel();
        speedRandomPanelContainer.setLayout(new BoxLayout(speedRandomPanelContainer, BoxLayout.Y_AXIS));
        JPanel lawSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lawSelectionPanel.add(new JLabel("Закон распределения:"));
        lawSelectionPanel.add(speedLawUniform); lawSelectionPanel.add(speedLawNormal); lawSelectionPanel.add(speedLawExponential);
        speedRandomPanelContainer.add(lawSelectionPanel);
        speedRandomPanelContainer.add(speedParamsUniformPanel);
        speedRandomPanelContainer.add(speedParamsNormalPanel);
        speedRandomPanelContainer.add(speedParamsExponentialPanel);
        mainPanel.add(speedRandomPanelContainer);
        return mainPanel;
    }

    private JPanel createTimeSettingsMainPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Настройка времени появления"));
        JPanel typeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typeSelectionPanel.add(new JLabel("Тип потока:"));
        typeSelectionPanel.add(timeDeterministicRadio); typeSelectionPanel.add(timeRandomRadio);
        mainPanel.add(typeSelectionPanel);
        timeDeterministicPanel = createSingleParameterPanel(new String[]{"Интервал:"}, new String[]{"сек"}, timeDetValueSpinner);
        mainPanel.add(timeDeterministicPanel);
        timeRandomPanelContainer = new JPanel();
        timeRandomPanelContainer.setLayout(new BoxLayout(timeRandomPanelContainer, BoxLayout.Y_AXIS));
        JPanel lawSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lawSelectionPanel.add(new JLabel("Закон распределения:"));
        lawSelectionPanel.add(timeLawUniform); lawSelectionPanel.add(timeLawNormal); lawSelectionPanel.add(timeLawExponential);
        timeRandomPanelContainer.add(lawSelectionPanel);
        timeRandomPanelContainer.add(timeParamsUniformPanel);
        timeRandomPanelContainer.add(timeParamsNormalPanel);
        timeRandomPanelContainer.add(timeParamsExponentialPanel);
        mainPanel.add(timeRandomPanelContainer);
        return mainPanel;
    }

    private void addListeners() {
        ActionListener speedFlowTypeListener = e -> updateSpeedSettingsVisibility();
        speedDeterministicRadio.addActionListener(speedFlowTypeListener);
        speedRandomRadio.addActionListener(speedFlowTypeListener);
        ActionListener speedLawListener = e -> updateSpeedLawPanelsVisibility();
        speedLawUniform.addActionListener(speedLawListener);
        speedLawNormal.addActionListener(speedLawListener);
        speedLawExponential.addActionListener(speedLawListener);

        ActionListener timeFlowTypeListener = e -> updateTimeSettingsVisibility();
        timeDeterministicRadio.addActionListener(timeFlowTypeListener);
        timeRandomRadio.addActionListener(timeFlowTypeListener);
        ActionListener timeLawListener = e -> updateTimeLawPanelsVisibility();
        timeLawUniform.addActionListener(timeLawListener);
        timeLawNormal.addActionListener(timeLawListener);
        timeLawExponential.addActionListener(timeLawListener);
    }

    private void updateSpeedSettingsVisibility() {
        boolean isRandom = speedRandomRadio.isSelected();
        speedDeterministicPanel.setVisible(!isRandom);
        speedRandomPanelContainer.setVisible(isRandom);
        if (isRandom) {
            updateSpeedLawPanelsVisibility();
        } else {
            speedParamsUniformPanel.setVisible(false);
            speedParamsNormalPanel.setVisible(false);
            speedParamsExponentialPanel.setVisible(false);
        }
        SwingUtilities.invokeLater(this::packIfVisible);
    }

    private void updateSpeedLawPanelsVisibility() {
        if (!speedRandomRadio.isSelected()) return;
        speedParamsUniformPanel.setVisible(speedLawUniform.isSelected());
        speedParamsNormalPanel.setVisible(speedLawNormal.isSelected());
        speedParamsExponentialPanel.setVisible(speedLawExponential.isSelected());
        SwingUtilities.invokeLater(this::packIfVisible);
    }

    private void updateTimeSettingsVisibility() {
        boolean isRandom = timeRandomRadio.isSelected();
        timeDeterministicPanel.setVisible(!isRandom);
        timeRandomPanelContainer.setVisible(isRandom);
        if (isRandom) {
            updateTimeLawPanelsVisibility();
        } else {
            timeParamsUniformPanel.setVisible(false);
            timeParamsNormalPanel.setVisible(false);
            timeParamsExponentialPanel.setVisible(false);
        }
        SwingUtilities.invokeLater(this::packIfVisible);
    }

    private void updateTimeLawPanelsVisibility() {
        if (!timeRandomRadio.isSelected()) return;
        timeParamsUniformPanel.setVisible(timeLawUniform.isSelected());
        timeParamsNormalPanel.setVisible(timeLawNormal.isSelected());
        timeParamsExponentialPanel.setVisible(timeLawExponential.isSelected());
        SwingUtilities.invokeLater(this::packIfVisible);
    }

    private void packIfVisible() {
        if (isVisible()) {
            pack();
        }
    }

    private void loadParameters() {
        updateSpeedSpinnersForRoadType();

        speedDeterministicRadio.setSelected(!params.isRandomSpeedFlow());
        speedRandomRadio.setSelected(params.isRandomSpeedFlow());
        double detSpeedToLoad = params.getDeterministicSpeedKmh();
        SpinnerNumberModel detModel = (SpinnerNumberModel) speedDetValueSpinner.getModel();
        detSpeedToLoad = Math.max((Double)detModel.getMinimum(), Math.min(detSpeedToLoad, (Double)detModel.getMaximum()));
        speedDetValueSpinner.setValue(detSpeedToLoad);

        DistributionLaw speedLaw = params.getSpeedDistributionLaw();
        if (speedLaw == null) speedLaw = DistributionLaw.NORMAL;
        switch(speedLaw) {
            case UNIFORM: speedLawUniform.setSelected(true); break;
            case NORMAL: speedLawNormal.setSelected(true); break;
            case EXPONENTIAL: speedLawExponential.setSelected(true); break;
            default: speedLawNormal.setSelected(true);
        }

        double uniformMinToLoad = params.getSpeedUniformMinKmh();
        SpinnerNumberModel uniformMinModel = (SpinnerNumberModel) speedUniformMinSpinner.getModel();
        uniformMinToLoad = Math.max((Double)uniformMinModel.getMinimum(), Math.min(uniformMinToLoad, (Double)uniformMinModel.getMaximum()));
        speedUniformMinSpinner.setValue(uniformMinToLoad);

        double uniformMaxToLoad = params.getSpeedUniformMaxKmh();
        SpinnerNumberModel uniformMaxModel = (SpinnerNumberModel) speedUniformMaxSpinner.getModel();
        uniformMaxToLoad = Math.max((Double)uniformMaxModel.getMinimum(), Math.min(uniformMaxToLoad, (Double)uniformMaxModel.getMaximum()));
        if (uniformMinToLoad > uniformMaxToLoad) uniformMaxToLoad = uniformMinToLoad;
        speedUniformMaxSpinner.setValue(uniformMaxToLoad);

        double normalMeanToLoad = params.getSpeedNormalMeanKmh();
        SpinnerNumberModel normalMeanModel = (SpinnerNumberModel) speedNormalMeanSpinner.getModel();
        normalMeanToLoad = Math.max((Double)normalMeanModel.getMinimum(), Math.min(normalMeanToLoad, (Double)normalMeanModel.getMaximum()));
        speedNormalMeanSpinner.setValue(normalMeanToLoad);

        speedNormalVarianceSpinner.setValue(params.getSpeedNormalVarianceKmh());
        speedExponentialIntensitySpinner.setValue(params.getSpeedExponentialIntensityPerKmh());

        timeDeterministicRadio.setSelected(!params.isRandomTimeFlow());
        timeRandomRadio.setSelected(params.isRandomTimeFlow());
        timeDetValueSpinner.setValue(params.getDeterministicIntervalSeconds());

        DistributionLaw timeLaw = params.getTimeDistributionLaw();
        if (timeLaw == null) timeLaw = DistributionLaw.NORMAL;
        switch(timeLaw) {
            case UNIFORM: timeLawUniform.setSelected(true); break;
            case NORMAL: timeLawNormal.setSelected(true); break;
            case EXPONENTIAL: timeLawExponential.setSelected(true); break;
            default: timeLawNormal.setSelected(true);
        }
        timeUniformMinSpinner.setValue(params.getTimeUniformMinSec());
        timeUniformMaxSpinner.setValue(params.getTimeUniformMaxSec());
        timeNormalMeanSpinner.setValue(params.getTimeNormalMeanSec());
        timeNormalVarianceSpinner.setValue(params.getTimeNormalVarianceSec());
        timeExponentialIntensitySpinner.setValue(params.getTimeExponentialIntensityPerSec());

        updateSpeedSettingsVisibility();
        updateTimeSettingsVisibility();
    }

    private void saveAndClose() {
        double detSpeedValue = ((Number) speedDetValueSpinner.getValue()).doubleValue();
        SpinnerNumberModel detSpeedModel = (SpinnerNumberModel) speedDetValueSpinner.getModel();
        if (detSpeedValue < (Double)detSpeedModel.getMinimum() || detSpeedValue > (Double)detSpeedModel.getMaximum()){
            JOptionPane.showMessageDialog(this,
                    "Детерминированная скорость (" + String.format("%.0f", detSpeedValue) + " км/ч) выходит за допустимые пределы (" +
                            String.format("%.0f", (Double)detSpeedModel.getMinimum()) + " - " + String.format("%.0f", (Double)detSpeedModel.getMaximum()) + " км/ч) для текущего типа дороги!",
                    "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (speedRandomRadio.isSelected()) {
            if (speedLawUniform.isSelected()) {
                double minSpeed = ((Number) speedUniformMinSpinner.getValue()).doubleValue();
                double maxSpeed = ((Number) speedUniformMaxSpinner.getValue()).doubleValue();
                SpinnerNumberModel uniformMinModel = (SpinnerNumberModel) speedUniformMinSpinner.getModel();
                if (minSpeed < (Double)uniformMinModel.getMinimum() || maxSpeed > (Double)uniformMinModel.getMaximum() || minSpeed > maxSpeed) {
                    JOptionPane.showMessageDialog(this, "Границы равномерного распределения скорости некорректны или выходят за пределы для типа дороги!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            if (speedLawNormal.isSelected()) {
                double meanSpeed = ((Number) speedNormalMeanSpinner.getValue()).doubleValue();
                SpinnerNumberModel normalMeanModel = (SpinnerNumberModel) speedNormalMeanSpinner.getModel();
                if (meanSpeed < (Double)normalMeanModel.getMinimum() || meanSpeed > (Double)normalMeanModel.getMaximum()) {
                    JOptionPane.showMessageDialog(this, "Мат. ожидание скорости выходит за допустимые пределы для типа дороги!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }
        if (timeRandomRadio.isSelected() && timeLawUniform.isSelected()) {
            double minTime = ((Number) timeUniformMinSpinner.getValue()).doubleValue();
            double maxTime = ((Number) timeUniformMaxSpinner.getValue()).doubleValue();
            if (minTime > maxTime) {
                JOptionPane.showMessageDialog(this, "Минимальное время не может быть больше максимального!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        params.setRandomSpeedFlow(speedRandomRadio.isSelected());
        if (params.isRandomSpeedFlow()){
            if(speedLawUniform.isSelected()) params.setSpeedDistributionLaw(DistributionLaw.UNIFORM);
            else if(speedLawNormal.isSelected()) params.setSpeedDistributionLaw(DistributionLaw.NORMAL);
            else if(speedLawExponential.isSelected()) params.setSpeedDistributionLaw(DistributionLaw.EXPONENTIAL);
            else params.setSpeedDistributionLaw(DistributionLaw.NORMAL);
            params.setSpeedUniformMinKmh(((Number) speedUniformMinSpinner.getValue()).doubleValue());
            params.setSpeedUniformMaxKmh(((Number) speedUniformMaxSpinner.getValue()).doubleValue());
            params.setSpeedNormalMeanKmh(((Number) speedNormalMeanSpinner.getValue()).doubleValue());
            params.setSpeedNormalVarianceKmh(((Number) speedNormalVarianceSpinner.getValue()).doubleValue());
            params.setSpeedExponentialIntensityPerKmh(((Number) speedExponentialIntensitySpinner.getValue()).doubleValue());
        } else {
            params.setDeterministicSpeedKmh(((Number) speedDetValueSpinner.getValue()).doubleValue());
        }

        params.setRandomTimeFlow(timeRandomRadio.isSelected());
        if (params.isRandomTimeFlow()){
            if(timeLawUniform.isSelected()) params.setTimeDistributionLaw(DistributionLaw.UNIFORM);
            else if(timeLawNormal.isSelected()) params.setTimeDistributionLaw(DistributionLaw.NORMAL);
            else if(timeLawExponential.isSelected()) params.setTimeDistributionLaw(DistributionLaw.EXPONENTIAL);
            else params.setTimeDistributionLaw(DistributionLaw.NORMAL);
            params.setTimeUniformMinSec(((Number) timeUniformMinSpinner.getValue()).doubleValue());
            params.setTimeUniformMaxSec(((Number) timeUniformMaxSpinner.getValue()).doubleValue());
            params.setTimeNormalMeanSec(((Number) timeNormalMeanSpinner.getValue()).doubleValue());
            params.setTimeNormalVarianceSec(((Number) timeNormalVarianceSpinner.getValue()).doubleValue());
            params.setTimeExponentialIntensityPerSec(((Number) timeExponentialIntensitySpinner.getValue()).doubleValue());
        } else {
            params.setDeterministicIntervalSeconds(((Number) timeDetValueSpinner.getValue()).doubleValue());
        }

        settingsSaved = true;
        dispose();
    }

    public boolean showDialog() {
        updateSpeedSpinnersForRoadType();
        loadParameters();
        setVisible(true);
        return settingsSaved;
    }
}
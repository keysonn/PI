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

    // Элементы для настройки скорости
    private JToggleButton speedDeterministicRadio, speedRandomRadio;
    private ButtonGroup speedFlowTypeGroup;
    private JPanel speedDeterministicPanel, speedRandomPanelContainer;
    private JSpinner speedDetValueSpinner;

    private JToggleButton speedLawUniform, speedLawNormal, speedLawExponential;
    private ButtonGroup speedLawGroup;
    private JPanel speedParamsUniformPanel, speedParamsNormalPanel, speedParamsExponentialPanel;
    private JSpinner speedUniformMinSpinner, speedUniformMaxSpinner;
    private JSpinner speedNormalMeanSpinner, speedNormalVarianceSpinner;
    private JSpinner speedExponentialIntensitySpinner;

    // Элементы для настройки времени появления
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

        initComponents(); // Инициализация компонентов с базовыми моделями
        layoutComponents();
        addListeners();
        pack();
        setMinimumSize(new Dimension(Math.max(650, getPreferredSize().width), getPreferredSize().height));
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // --- Скорость ---
        speedDeterministicRadio = new JToggleButton("Детерминированный");
        speedRandomRadio = new JToggleButton("Случайный");
        speedFlowTypeGroup = new ButtonGroup();
        speedFlowTypeGroup.add(speedDeterministicRadio); speedFlowTypeGroup.add(speedRandomRadio);
        speedDetValueSpinner = new JSpinner(); // Модель будет установлена в updateSpeedSpinners

        speedLawUniform = new JToggleButton("Равномерный");
        speedLawNormal = new JToggleButton("Нормальный");
        speedLawExponential = new JToggleButton("Показательный");
        speedLawGroup = new ButtonGroup();
        speedLawGroup.add(speedLawUniform); speedLawGroup.add(speedLawNormal); speedLawGroup.add(speedLawExponential);

        speedUniformMinSpinner = new JSpinner();
        speedUniformMaxSpinner = new JSpinner();
        speedNormalMeanSpinner = new JSpinner();
        speedNormalVarianceSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 40.0, 1.0)); // Дисперсия не зависит от типа дороги
        speedExponentialIntensitySpinner = new JSpinner(new SpinnerNumberModel(0.02, 1.0/130.0, 1.0/20.0, 0.001)); // Интенсивность скорости
        ((JSpinner.NumberEditor)speedExponentialIntensitySpinner.getEditor()).getFormat().setMinimumFractionDigits(3);


        // --- Время появления ---
        timeDeterministicRadio = new JToggleButton("Детерминированный");
        timeRandomRadio = new JToggleButton("Случайный");
        timeFlowTypeGroup = new ButtonGroup();
        timeFlowTypeGroup.add(timeDeterministicRadio); timeFlowTypeGroup.add(timeRandomRadio);
        timeDetValueSpinner = new JSpinner(new SpinnerNumberModel(8.0, 6.5, 15.0, 0.1));

        timeLawUniform = new JToggleButton("Равномерный");
        timeLawNormal = new JToggleButton("Нормальный");
        timeLawExponential = new JToggleButton("Показательный");
        timeLawGroup = new ButtonGroup();
        timeLawGroup.add(timeLawUniform); timeLawGroup.add(timeLawNormal); timeLawGroup.add(timeLawExponential);

        timeUniformMinSpinner = new JSpinner(new SpinnerNumberModel(7.0, 6.5, 15.0, 0.1));
        timeUniformMaxSpinner = new JSpinner(new SpinnerNumberModel(12.0, 6.5, 15.0, 0.1));
        timeNormalMeanSpinner = new JSpinner(new SpinnerNumberModel(15.0, 13.5, 120.0, 0.1));
        timeNormalVarianceSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 20.0, 0.1));
        timeExponentialIntensitySpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.3, 20.0, 0.1));

        speedParamsUniformPanel = createSingleParameterPanel(new String[]{"Нижняя граница:", "Верхняя граница:"}, new String[]{"км/ч", "км/ч"}, speedUniformMinSpinner, speedUniformMaxSpinner);
        speedParamsNormalPanel = createSingleParameterPanel(new String[]{"Мат. ожидание:", "Дисперсия:"}, new String[]{"км/ч", "(км/ч)²"}, speedNormalMeanSpinner, speedNormalVarianceSpinner);
        speedParamsExponentialPanel = createSingleParameterPanel(new String[]{"Интенсивность λ:"}, new String[]{"1/(км/ч)"}, speedExponentialIntensitySpinner);
        timeParamsUniformPanel = createSingleParameterPanel(new String[]{"Нижняя граница:", "Верхняя граница:"}, new String[]{"сек", "сек"}, timeUniformMinSpinner, timeUniformMaxSpinner);
        timeParamsNormalPanel = createSingleParameterPanel(new String[]{"Мат. ожидание:", "Дисперсия:"}, new String[]{"сек", "сек²"}, timeNormalMeanSpinner, timeNormalVarianceSpinner);
        timeParamsExponentialPanel = createSingleParameterPanel(new String[]{"Интенсивность λ:"}, new String[]{"авто/сек"}, timeExponentialIntensitySpinner);
    }

    private void updateSpeedSpinnersModelsAndValues() {
        RoadType currentRoadType = params.getRoadType();
        System.out.println("[DEBUG] ModelingSettingsDialog.updateSpeedSpinnersModelsAndValues: Current RoadType from params = " + currentRoadType);

        if (currentRoadType == null) {
            System.out.println("[DEBUG] ModelingSettingsDialog.updateSpeedSpinnersModelsAndValues: RoadType was null, defaulting to CITY_ROAD.");
            currentRoadType = RoadType.CITY_ROAD;
        }

        double minSpeedLimit = currentRoadType.getMinSpeedLimitKmh();
        double maxSpeedLimit = currentRoadType.getMaxSpeedLimitKmh();
        double defaultSpeed = currentRoadType.getDefaultSpeedLimitKmh();

        // Детерминированная скорость
        double detSpeedVal = params.getDeterministicSpeedKmh();
        detSpeedVal = Math.max(minSpeedLimit, Math.min(detSpeedVal, maxSpeedLimit)); // Приводим к границам
        speedDetValueSpinner.setModel(new SpinnerNumberModel(detSpeedVal, minSpeedLimit, maxSpeedLimit, 1.0));

        // Равномерное распределение скорости
        double uniformMinVal = params.getSpeedUniformMinKmh();
        uniformMinVal = Math.max(minSpeedLimit, Math.min(uniformMinVal, maxSpeedLimit));
        double uniformMaxVal = params.getSpeedUniformMaxKmh();
        uniformMaxVal = Math.max(minSpeedLimit, Math.min(uniformMaxVal, maxSpeedLimit));
        if (uniformMinVal > uniformMaxVal) uniformMinVal = uniformMaxVal; // Гарантируем min <= max
        speedUniformMinSpinner.setModel(new SpinnerNumberModel(uniformMinVal, minSpeedLimit, maxSpeedLimit, 1.0));
        if (uniformMinVal > uniformMaxVal) uniformMaxVal = uniformMinVal; // Перепроверка после установки min
        uniformMaxVal = Math.max(uniformMinVal, uniformMaxVal); // Гарантируем, что max не меньше min
        speedUniformMaxSpinner.setModel(new SpinnerNumberModel(uniformMaxVal, minSpeedLimit, maxSpeedLimit, 1.0));


        // Нормальное распределение скорости (мат. ожидание)
        double normalMeanVal = params.getSpeedNormalMeanKmh();
        normalMeanVal = Math.max(minSpeedLimit, Math.min(normalMeanVal, maxSpeedLimit));
        speedNormalMeanSpinner.setModel(new SpinnerNumberModel(normalMeanVal, minSpeedLimit, maxSpeedLimit, 1.0));

        // Дисперсия и интенсивность скорости - их модели не меняются в зависимости от min/max скорости дороги
        // но мы можем перезагрузить их значения из params, если они там есть
        speedNormalVarianceSpinner.setValue(params.getSpeedNormalVarianceKmh());
        speedExponentialIntensitySpinner.setValue(params.getSpeedExponentialIntensityPerKmh());
    }


    private JPanel createSingleParameterPanel(String[] labels, String[] units, JComponent... components) {
        // ... (без изменений) ...
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            panel.add(new JLabel(labels[i]), gbc);
            JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            valuePanel.add(components[i]);
            if (units != null && units.length > i && units[i] != null && !units[i].isEmpty()) {
                valuePanel.add(new JLabel(" " + units[i]));
            }
            gbc.gridx = 1; gbc.gridy = i;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            panel.add(valuePanel, gbc);
        }
        return panel;
    }

    private void layoutComponents() {
        // ... (без изменений) ...
        setLayout(new BorderLayout(10,10));
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        mainPanel.add(createSpeedSettingsMainPanel());
        mainPanel.add(createTimeSettingsMainPanel());
        add(mainPanel, BorderLayout.CENTER);
        JButton saveButton = new JButton("Сохранить настройки");
        saveButton.addActionListener(e -> saveAndClose());
        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> { settingsSaved = false; dispose(); });
        JPanel buttonPanelSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanelSouth.add(saveButton);
        buttonPanelSouth.add(cancelButton);
        add(buttonPanelSouth, BorderLayout.SOUTH);
    }

    private JPanel createSpeedSettingsMainPanel() {
        // ... (без изменений) ...
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
        speedRandomPanelContainer.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
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
        // ... (без изменений) ...
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
        timeRandomPanelContainer.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
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
        // ... (без изменений) ...
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
        // ... (без изменений) ...
        boolean isRandom = speedRandomRadio.isSelected();
        speedDeterministicPanel.setVisible(!isRandom);
        speedRandomPanelContainer.setVisible(isRandom);
        if (isRandom) { updateSpeedLawPanelsVisibility(); }
        else {
            speedParamsUniformPanel.setVisible(false);
            speedParamsNormalPanel.setVisible(false);
            speedParamsExponentialPanel.setVisible(false);
        }
        SwingUtilities.invokeLater(this::packIfVisible);
    }
    private void updateSpeedLawPanelsVisibility() {
        // ... (без изменений) ...
        if (!speedRandomRadio.isSelected()) return;
        speedParamsUniformPanel.setVisible(speedLawUniform.isSelected());
        speedParamsNormalPanel.setVisible(speedLawNormal.isSelected());
        speedParamsExponentialPanel.setVisible(speedLawExponential.isSelected());
        SwingUtilities.invokeLater(this::packIfVisible);
    }
    private void updateTimeSettingsVisibility() {
        // ... (без изменений) ...
        boolean isRandom = timeRandomRadio.isSelected();
        timeDeterministicPanel.setVisible(!isRandom);
        timeRandomPanelContainer.setVisible(isRandom);
        if (isRandom) { updateTimeLawPanelsVisibility(); }
        else {
            timeParamsUniformPanel.setVisible(false);
            timeParamsNormalPanel.setVisible(false);
            timeParamsExponentialPanel.setVisible(false);
        }
        SwingUtilities.invokeLater(this::packIfVisible);
    }
    private void updateTimeLawPanelsVisibility() {
        // ... (без изменений) ...
        if (!timeRandomRadio.isSelected()) return;
        timeParamsUniformPanel.setVisible(timeLawUniform.isSelected());
        timeParamsNormalPanel.setVisible(timeLawNormal.isSelected());
        timeParamsExponentialPanel.setVisible(timeLawExponential.isSelected());
        SwingUtilities.invokeLater(this::packIfVisible);
    }
    private void packIfVisible() { if (isVisible()) pack(); }

    private void loadParameters() {
        // Сначала обновляем модели и значения спиннеров скорости на основе типа дороги
        updateSpeedSpinnersModelsAndValues();

        // Затем устанавливаем состояния кнопок и значения для времени
        speedDeterministicRadio.setSelected(!params.isRandomSpeedFlow());
        speedRandomRadio.setSelected(params.isRandomSpeedFlow());
        // speedDetValueSpinner.setValue(params.getDeterministicSpeedKmh()); // Уже установлено в updateSpeedSpinnersModelsAndValues

        DistributionLaw speedLaw = params.getSpeedDistributionLaw() != null ? params.getSpeedDistributionLaw() : DistributionLaw.NORMAL;
        speedLawUniform.setSelected(speedLaw == DistributionLaw.UNIFORM);
        speedLawNormal.setSelected(speedLaw == DistributionLaw.NORMAL);
        speedLawExponential.setSelected(speedLaw == DistributionLaw.EXPONENTIAL);
        if (params.isRandomSpeedFlow() && !speedLawUniform.isSelected() && !speedLawNormal.isSelected() && !speedLawExponential.isSelected()) {
            speedLawNormal.setSelected(true);
        }
        // Значения speedUniformMin/Max/NormalMean также установлены в updateSpeedSpinnersModelsAndValues

        timeDeterministicRadio.setSelected(!params.isRandomTimeFlow());
        timeRandomRadio.setSelected(params.isRandomTimeFlow());
        timeDetValueSpinner.setValue(params.getDeterministicIntervalSeconds());

        DistributionLaw timeLaw = params.getTimeDistributionLaw() != null ? params.getTimeDistributionLaw() : DistributionLaw.NORMAL;
        timeLawUniform.setSelected(timeLaw == DistributionLaw.UNIFORM);
        timeLawNormal.setSelected(timeLaw == DistributionLaw.NORMAL);
        timeLawExponential.setSelected(timeLaw == DistributionLaw.EXPONENTIAL);
        if (params.isRandomTimeFlow() && !timeLawUniform.isSelected() && !timeLawNormal.isSelected() && !timeLawExponential.isSelected()) {
            timeLawNormal.setSelected(true);
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
        System.out.println("[DEBUG] ModelingSettingsDialog.saveAndClose: Validating with RoadType from params = " + params.getRoadType());
        // Валидация скорости
        if (speedDeterministicRadio.isSelected()) {
            double detSpeedValue = ((Number) speedDetValueSpinner.getValue()).doubleValue();
            SpinnerNumberModel detSpeedModel = (SpinnerNumberModel) speedDetValueSpinner.getModel();
            if (detSpeedValue < (Double)detSpeedModel.getMinimum() || detSpeedValue > (Double)detSpeedModel.getMaximum()){
                JOptionPane.showMessageDialog(this, "Детерминированная скорость (" + String.format("%.0f", detSpeedValue) + " км/ч) выходит за допустимые пределы (" + String.format("%.0f", (Double)detSpeedModel.getMinimum()) + " - " + String.format("%.0f", (Double)detSpeedModel.getMaximum()) + " км/ч) для текущего типа дороги: " + params.getRoadType().getDisplayName() + "!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else if (speedRandomRadio.isSelected()) {
            if (!speedLawUniform.isSelected() && !speedLawNormal.isSelected() && !speedLawExponential.isSelected()){
                JOptionPane.showMessageDialog(this, "Для случайного потока скорости не выбран закон распределения!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (speedLawUniform.isSelected()) {
                double minSpeed = ((Number) speedUniformMinSpinner.getValue()).doubleValue();
                double maxSpeed = ((Number) speedUniformMaxSpinner.getValue()).doubleValue();
                SpinnerNumberModel uniformMinModel = (SpinnerNumberModel) speedUniformMinSpinner.getModel(); // Берем актуальную модель
                if (minSpeed < (Double)uniformMinModel.getMinimum() || maxSpeed > (Double)uniformMinModel.getMaximum() || minSpeed > maxSpeed) {
                    JOptionPane.showMessageDialog(this, "Границы равномерного распределения скорости некорректны или выходят за пределы ("+ String.format("%.0f", (Double)uniformMinModel.getMinimum()) +"-"+ String.format("%.0f", (Double)uniformMinModel.getMaximum()) +") для типа дороги: " + params.getRoadType().getDisplayName() + "!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            if (speedLawNormal.isSelected()) {
                double meanSpeed = ((Number) speedNormalMeanSpinner.getValue()).doubleValue();
                SpinnerNumberModel normalMeanModel = (SpinnerNumberModel) speedNormalMeanSpinner.getModel();
                if (meanSpeed < (Double)normalMeanModel.getMinimum() || meanSpeed > (Double)normalMeanModel.getMaximum()) {
                    JOptionPane.showMessageDialog(this, "Мат. ожидание скорости ("+ String.format("%.0f", meanSpeed) +") выходит за допустимые пределы ("+ String.format("%.0f", (Double)normalMeanModel.getMinimum()) +"-"+ String.format("%.0f", (Double)normalMeanModel.getMaximum()) +") для типа дороги: " + params.getRoadType().getDisplayName() + "!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        // Валидация времени
        if (timeRandomRadio.isSelected()) {
            if (!timeLawUniform.isSelected() && !timeLawNormal.isSelected() && !timeLawExponential.isSelected()){
                JOptionPane.showMessageDialog(this, "Для случайного потока времени не выбран закон распределения!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (timeLawUniform.isSelected()) {
                double minTime = ((Number) timeUniformMinSpinner.getValue()).doubleValue();
                double maxTime = ((Number) timeUniformMaxSpinner.getValue()).doubleValue();
                if (minTime > maxTime) {
                    JOptionPane.showMessageDialog(this, "Минимальное время не может быть больше максимального!", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        // Сохранение параметров скорости
        params.setRandomSpeedFlow(speedRandomRadio.isSelected());
        if (params.isRandomSpeedFlow()){
            if(speedLawUniform.isSelected()) params.setSpeedDistributionLaw(DistributionLaw.UNIFORM);
            else if(speedLawNormal.isSelected()) params.setSpeedDistributionLaw(DistributionLaw.NORMAL);
            else if(speedLawExponential.isSelected()) params.setSpeedDistributionLaw(DistributionLaw.EXPONENTIAL);
            params.setSpeedUniformMinKmh(((Number) speedUniformMinSpinner.getValue()).doubleValue());
            params.setSpeedUniformMaxKmh(((Number) speedUniformMaxSpinner.getValue()).doubleValue());
            params.setSpeedNormalMeanKmh(((Number) speedNormalMeanSpinner.getValue()).doubleValue());
            params.setSpeedNormalVarianceKmh(((Number) speedNormalVarianceSpinner.getValue()).doubleValue());
            params.setSpeedExponentialIntensityPerKmh(((Number) speedExponentialIntensitySpinner.getValue()).doubleValue());
        } else {
            params.setDeterministicSpeedKmh(((Number) speedDetValueSpinner.getValue()).doubleValue());
        }

        // Сохранение параметров времени
        params.setRandomTimeFlow(timeRandomRadio.isSelected());
        if (params.isRandomTimeFlow()){
            if(timeLawUniform.isSelected()) params.setTimeDistributionLaw(DistributionLaw.UNIFORM);
            else if(timeLawNormal.isSelected()) params.setTimeDistributionLaw(DistributionLaw.NORMAL);
            else if(timeLawExponential.isSelected()) params.setTimeDistributionLaw(DistributionLaw.EXPONENTIAL);
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
        // СНАЧАЛА обновляем модели и значения спиннеров скорости на основе ТЕКУЩЕГО типа дороги в params
        updateSpeedSpinnersModelsAndValues();
        // ЗАТЕМ загружаем все остальные сохраненные значения из params в элементы GUI (кнопки, время и т.д.)
        loadParameters(); // loadParameters теперь не должен вызывать updateSpeedSpinners... повторно
        setVisible(true);
        return settingsSaved;
    }
}
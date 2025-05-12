package com.trafficsimulation.gui;

import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.simulation.DistributionLaw;
import com.trafficsimulation.simulation.SimulationParameters;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class SettingsDialog extends JDialog {

    private SimulationParameters params;
    private boolean settingsSaved = false;

    // Дорога
    private JComboBox<RoadType> roadTypeComboBox;
    private JSpinner roadLengthSpinner;
    private JToggleButton dirOneWayButton, dirTwoWayButton;
    private ButtonGroup directionGroup;
    private JSlider lanesSlider;

    // Тоннель
    private JPanel commonRoadSettingsPanelContainer;
    private JPanel tunnelSettingsPanel;
    private JSpinner tunnelRedLightSpinner, tunnelGreenLightSpinner;

    // Скоростной режим
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


    public SettingsDialog(Frame owner, SimulationParameters currentParams) {
        super(owner, "Настройки", true);
        this.params = currentParams;

        initComponents();
        layoutMainDialog();
        addListeners();
        loadParameters();

        updateRoadSettingsVisibility();
        updateSpeedSettingsVisibility();
        updateTimeSettingsVisibility();

        pack();
        setMinimumSize(new Dimension(Math.max(680, getPreferredSize().width), getPreferredSize().height));
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // Дорога
        roadTypeComboBox = new JComboBox<>(new RoadType[]{RoadType.CITY_ROAD, RoadType.HIGHWAY, RoadType.TUNNEL});
        roadLengthSpinner = new JSpinner(new SpinnerNumberModel(5.0, 1.0, 50.0, 0.5));
        dirOneWayButton = new JToggleButton("Одностороннее", true);
        dirTwoWayButton = new JToggleButton("Двустороннее");
        directionGroup = new ButtonGroup();
        directionGroup.add(dirOneWayButton); directionGroup.add(dirTwoWayButton);
        lanesSlider = new JSlider(JSlider.HORIZONTAL, 1, 4, 2);
        lanesSlider.setMajorTickSpacing(1); lanesSlider.setPaintTicks(true);
        lanesSlider.setPaintLabels(true); lanesSlider.setSnapToTicks(true);
        // Тоннель
        tunnelRedLightSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        tunnelGreenLightSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        // Скоростной режим
        speedDeterministicRadio = new JToggleButton("Детерминированный");
        speedRandomRadio = new JToggleButton("Случайный");
        speedFlowTypeGroup = new ButtonGroup();
        speedFlowTypeGroup.add(speedDeterministicRadio); speedFlowTypeGroup.add(speedRandomRadio);
        speedDetValueSpinner = new JSpinner(new SpinnerNumberModel(60.0, 20.0, 130.0, 5.0));
        speedLawUniform = new JToggleButton("Равномерный");
        speedLawNormal = new JToggleButton("Нормальный");
        speedLawExponential = new JToggleButton("Показательный");
        speedLawGroup = new ButtonGroup();
        speedLawGroup.add(speedLawUniform); speedLawGroup.add(speedLawNormal); speedLawGroup.add(speedLawExponential);
        speedUniformMinSpinner = new JSpinner(new SpinnerNumberModel(40.0, 20.0, 130.0, 1.0));
        speedUniformMaxSpinner = new JSpinner(new SpinnerNumberModel(80.0, 20.0, 130.0, 1.0));
        speedNormalMeanSpinner = new JSpinner(new SpinnerNumberModel(60.0, 20.0, 130.0, 1.0));
        speedNormalVarianceSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 40.0, 1.0));
        speedExponentialIntensitySpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.01, 10.0, 0.01)); // Параметр формы, не интенсивность потока
        // Настройка времени
        timeDeterministicRadio = new JToggleButton("Детерминированный");
        timeRandomRadio = new JToggleButton("Случайный");
        timeFlowTypeGroup = new ButtonGroup();
        timeFlowTypeGroup.add(timeDeterministicRadio); timeFlowTypeGroup.add(timeRandomRadio);
        timeDetValueSpinner = new JSpinner(new SpinnerNumberModel(10.0, 10.0, 15.0, 0.5));
        timeLawUniform = new JToggleButton("Равномерный");
        timeLawNormal = new JToggleButton("Нормальный");
        timeLawExponential = new JToggleButton("Показательный");
        timeLawGroup = new ButtonGroup();
        timeLawGroup.add(timeLawUniform); timeLawGroup.add(timeLawNormal); timeLawGroup.add(timeLawExponential);
        timeUniformMinSpinner = new JSpinner(new SpinnerNumberModel(10.0, 10.0, 15.0, 0.1));
        timeUniformMaxSpinner = new JSpinner(new SpinnerNumberModel(15.0, 10.0, 15.0, 0.1));
        timeNormalMeanSpinner = new JSpinner(new SpinnerNumberModel(20.0, 20.0, 120.0, 1.0));
        timeNormalVarianceSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 20.0, 0.1));
        // ИЗМЕНЕНИЕ ЗДЕСЬ: Минимальная интенсивность 0.2 авто/сек, максимальная 20 авто/сек
        timeExponentialIntensitySpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.2, 20.0, 0.1)); // авто/сек

        speedParamsUniformPanel = createSingleLawParameterPanel(new String[]{"Укажите нижнюю границу:", "Укажите верхнюю границу:"}, new String[]{"км/ч", "км/ч"}, speedUniformMinSpinner, speedUniformMaxSpinner);
        speedParamsNormalPanel = createSingleLawParameterPanel(new String[]{"Укажите математическое ожидание:", "Укажите дисперсию:"}, new String[]{"км/ч", "(км/ч)²"}, speedNormalMeanSpinner, speedNormalVarianceSpinner);
        speedParamsExponentialPanel = createSingleLawParameterPanel(new String[]{"Задайте параметр интенсивности:"}, new String[]{"1/(км/ч)"}, speedExponentialIntensitySpinner); // Название параметра может быть неточным для скорости
        timeParamsUniformPanel = createSingleLawParameterPanel(new String[]{"Укажите нижнюю границу:", "Укажите верхнюю границу:"}, new String[]{"сек", "сек"}, timeUniformMinSpinner, timeUniformMaxSpinner);
        timeParamsNormalPanel = createSingleLawParameterPanel(new String[]{"Укажите математическое ожидание:", "Укажите дисперсию:"}, new String[]{"сек", "сек²"}, timeNormalMeanSpinner, timeNormalVarianceSpinner);
        timeParamsExponentialPanel = createSingleLawParameterPanel(new String[]{"Задайте интенсивность времени:"}, new String[]{"авто/сек"}, timeExponentialIntensitySpinner);
    }

    private JPanel createSingleLawParameterPanel(String[] labels, String[] units, JComponent... components) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5); gbc.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; panel.add(new JLabel(labels[i]), gbc);
            JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            valuePanel.add(components[i]);
            if (units != null && units.length > i && units[i] != null) valuePanel.add(new JLabel(" " + units[i]));
            gbc.gridx = 1; gbc.gridy = i; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; panel.add(valuePanel, gbc);
        }
        return panel;
    }

    private void layoutMainDialog() {
        setLayout(new BorderLayout(10, 10));
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel roadSettingsTabPanel = new JPanel();
        roadSettingsTabPanel.setLayout(new BoxLayout(roadSettingsTabPanel, BoxLayout.Y_AXIS));
        roadSettingsTabPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JPanel roadTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roadTypePanel.add(new JLabel("Выберите тип автодороги:"));
        roadTypePanel.add(roadTypeComboBox);
        roadSettingsTabPanel.add(roadTypePanel);
        commonRoadSettingsPanelContainer = createCommonRoadSettingsPanel();
        roadSettingsTabPanel.add(commonRoadSettingsPanelContainer);
        tunnelSettingsPanel = createTunnelSettingsPanel();
        roadSettingsTabPanel.add(tunnelSettingsPanel);
        tabbedPane.addTab("Параметры автодороги", roadSettingsTabPanel);
        JPanel modelingSettingsTabPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        modelingSettingsTabPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        modelingSettingsTabPanel.add(createSpeedSettingsMainPanel());
        modelingSettingsTabPanel.add(createTimeSettingsMainPanel());
        tabbedPane.addTab("Параметры моделирования", modelingSettingsTabPanel);
        add(tabbedPane, BorderLayout.CENTER);
        JButton saveButton = new JButton("Сохранить настройки");
        saveButton.addActionListener(e -> saveAndClose());
        JPanel buttonPanelSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanelSouth.add(saveButton);
        add(buttonPanelSouth, BorderLayout.SOUTH);
    }

    private JPanel createCommonRoadSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0; gbc.gridx = 0; panel.add(new JLabel("Направление движения:"), gbc);
        JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        dirPanel.add(dirOneWayButton); dirPanel.add(dirTwoWayButton);
        gbc.gridx = 1; panel.add(dirPanel, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Количество полос (в 1 напр.):"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; panel.add(lanesSlider, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Длина участка автодороги:"), gbc);
        JPanel lengthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        lengthPanel.add(roadLengthSpinner); lengthPanel.add(new JLabel(" км"));
        gbc.gridx = 1; panel.add(lengthPanel, gbc);
        return panel;
    }
    private JPanel createTunnelSettingsPanel(){
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5); gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = 0; gbc.gridx = 0; panel.add(new JLabel("Длина красного света:"), gbc);
        JPanel redPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        redPanel.add(tunnelRedLightSpinner); redPanel.add(new JLabel(" секунд"));
        gbc.gridx = 1; panel.add(redPanel, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Длина зеленого света:"), gbc);
        JPanel greenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        greenPanel.add(tunnelGreenLightSpinner); greenPanel.add(new JLabel(" секунд"));
        gbc.gridx = 1; panel.add(greenPanel, gbc);
        return panel;
    }

    private JPanel createSpeedSettingsMainPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Настройка скоростного режима"));
        JPanel typeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typeSelectionPanel.add(new JLabel("Выберите тип потока:"));
        typeSelectionPanel.add(speedDeterministicRadio); typeSelectionPanel.add(speedRandomRadio);
        mainPanel.add(typeSelectionPanel);
        speedDeterministicPanel = createSingleLawParameterPanel(new String[]{"Задайте скорость:"}, new String[]{"км/ч"}, speedDetValueSpinner);
        mainPanel.add(speedDeterministicPanel);
        speedRandomPanelContainer = new JPanel();
        speedRandomPanelContainer.setLayout(new BoxLayout(speedRandomPanelContainer, BoxLayout.Y_AXIS));
        JPanel lawSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lawSelectionPanel.add(new JLabel("Выберите закон распределения:"));
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
        mainPanel.setBorder(BorderFactory.createTitledBorder("Настройка времени"));
        JPanel typeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typeSelectionPanel.add(new JLabel("Выберите тип потока:"));
        typeSelectionPanel.add(timeDeterministicRadio); typeSelectionPanel.add(timeRandomRadio);
        mainPanel.add(typeSelectionPanel);
        timeDeterministicPanel = createSingleLawParameterPanel(new String[]{"Задайте интервал появления:"}, new String[]{"сек"}, timeDetValueSpinner);
        mainPanel.add(timeDeterministicPanel);
        timeRandomPanelContainer = new JPanel();
        timeRandomPanelContainer.setLayout(new BoxLayout(timeRandomPanelContainer, BoxLayout.Y_AXIS));
        JPanel lawSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lawSelectionPanel.add(new JLabel("Выберите закон распределения:"));
        lawSelectionPanel.add(timeLawUniform); lawSelectionPanel.add(timeLawNormal); lawSelectionPanel.add(timeLawExponential);
        timeRandomPanelContainer.add(lawSelectionPanel);
        timeRandomPanelContainer.add(timeParamsUniformPanel);
        timeRandomPanelContainer.add(timeParamsNormalPanel);
        timeRandomPanelContainer.add(timeParamsExponentialPanel);
        mainPanel.add(timeRandomPanelContainer);
        return mainPanel;
    }

    private void addListeners() {
        roadTypeComboBox.addActionListener(e -> updateRoadSettingsVisibility());
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

    private void updateRoadSettingsVisibility() {
        RoadType selectedType = (RoadType) roadTypeComboBox.getSelectedItem();
        boolean isTunnel = (selectedType == RoadType.TUNNEL);
        if (commonRoadSettingsPanelContainer != null) commonRoadSettingsPanelContainer.setVisible(!isTunnel);
        if (tunnelSettingsPanel != null) tunnelSettingsPanel.setVisible(isTunnel);
        SwingUtilities.invokeLater(() -> { if (isVisible()) pack(); });
    }

    private void updateSpeedSettingsVisibility() {
        boolean isRandom = speedRandomRadio.isSelected();
        speedDeterministicPanel.setVisible(!isRandom);
        speedRandomPanelContainer.setVisible(isRandom);
        if (isRandom) updateSpeedLawPanelsVisibility();
        SwingUtilities.invokeLater(() -> { if (isVisible()) pack(); });
    }
    private void updateSpeedLawPanelsVisibility() {
        if (!speedRandomRadio.isSelected()) return;
        speedParamsUniformPanel.setVisible(speedLawUniform.isSelected());
        speedParamsNormalPanel.setVisible(speedLawNormal.isSelected());
        speedParamsExponentialPanel.setVisible(speedLawExponential.isSelected());
        SwingUtilities.invokeLater(() -> { if (isVisible()) pack(); });
    }
    private void updateTimeSettingsVisibility() {
        boolean isRandom = timeRandomRadio.isSelected();
        timeDeterministicPanel.setVisible(!isRandom);
        timeRandomPanelContainer.setVisible(isRandom);
        if (isRandom) updateTimeLawPanelsVisibility();
        SwingUtilities.invokeLater(() -> { if (isVisible()) pack(); });
    }
    private void updateTimeLawPanelsVisibility() {
        if (!timeRandomRadio.isSelected()) return;
        timeParamsUniformPanel.setVisible(timeLawUniform.isSelected());
        timeParamsNormalPanel.setVisible(timeLawNormal.isSelected());
        timeParamsExponentialPanel.setVisible(timeLawExponential.isSelected());
        SwingUtilities.invokeLater(() -> { if (isVisible()) pack(); });
    }

    private void loadParameters() {
        roadTypeComboBox.setSelectedItem(params.getRoadType());
        roadLengthSpinner.setValue(params.getRoadLengthKm());
        if (params.getNumberOfDirections() == 1) dirOneWayButton.setSelected(true);
        else dirTwoWayButton.setSelected(true);
        lanesSlider.setValue(params.getLanesPerDirection());

        speedDeterministicRadio.setSelected(!params.isRandomSpeedFlow());
        speedRandomRadio.setSelected(params.isRandomSpeedFlow());
        speedDetValueSpinner.setValue(params.getDeterministicSpeedKmh());
        DistributionLaw speedLaw = params.getSpeedDistributionLaw();
        if (speedLaw == DistributionLaw.UNIFORM) speedLawUniform.setSelected(true);
        else if (speedLaw == DistributionLaw.NORMAL) speedLawNormal.setSelected(true);
        else if (speedLaw == DistributionLaw.EXPONENTIAL) speedLawExponential.setSelected(true);
        else speedLawNormal.setSelected(true);
        speedUniformMinSpinner.setValue(params.getSpeedUniformMinKmh());
        speedUniformMaxSpinner.setValue(params.getSpeedUniformMaxKmh());
        speedNormalMeanSpinner.setValue(params.getSpeedNormalMeanKmh());
        speedNormalVarianceSpinner.setValue(params.getSpeedNormalVarianceKmh());
        speedExponentialIntensitySpinner.setValue(params.getSpeedExponentialIntensityPerKmh());

        timeDeterministicRadio.setSelected(!params.isRandomTimeFlow());
        timeRandomRadio.setSelected(params.isRandomTimeFlow());
        timeDetValueSpinner.setValue(params.getDeterministicIntervalSeconds());
        DistributionLaw timeLaw = params.getTimeDistributionLaw();
        if (timeLaw == DistributionLaw.UNIFORM) timeLawUniform.setSelected(true);
        else if (timeLaw == DistributionLaw.NORMAL) timeLawNormal.setSelected(true);
        else if (timeLaw == DistributionLaw.EXPONENTIAL) timeLawExponential.setSelected(true);
        else timeLawNormal.setSelected(true);
        timeUniformMinSpinner.setValue(params.getTimeUniformMinSec());
        timeUniformMaxSpinner.setValue(params.getTimeUniformMaxSec());
        timeNormalMeanSpinner.setValue(params.getTimeNormalMeanSec());
        timeNormalVarianceSpinner.setValue(params.getTimeNormalVarianceSec());
        timeExponentialIntensitySpinner.setValue(params.getTimeExponentialIntensityPerSec());
    }

    private void saveAndClose() {
        params.setRoadType((RoadType) roadTypeComboBox.getSelectedItem());
        params.setRoadLengthKm(((Number) roadLengthSpinner.getValue()).doubleValue());
        params.setNumberOfDirections(dirOneWayButton.isSelected() ? 1 : 2);
        params.setLanesPerDirection(lanesSlider.getValue());

        params.setRandomSpeedFlow(speedRandomRadio.isSelected());
        if (params.isRandomSpeedFlow()){
            if(speedLawUniform.isSelected()) params.setSpeedDistributionLaw(DistributionLaw.UNIFORM);
            else if(speedLawNormal.isSelected()) params.setSpeedDistributionLaw(DistributionLaw.NORMAL);
            else params.setSpeedDistributionLaw(DistributionLaw.EXPONENTIAL);
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
            else params.setTimeDistributionLaw(DistributionLaw.EXPONENTIAL);
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
        setVisible(true);
        return settingsSaved;
    }
}
package com.trafficsimulation.gui;

import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.simulation.DistributionLaw;
import com.trafficsimulation.simulation.SimulationParameters;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;

public class SettingsDialog extends JDialog {

    private SimulationParameters params;
    private boolean settingsSaved = false;

    // Дорога
    private JComboBox<RoadType> roadTypeComboBox;
    private JSpinner roadLengthSpinner;
    private JSpinner lanesPerDirectionSpinner;
    private JSpinner numDirectionsSpinner;

    // Тип потока
    private JRadioButton deterministicFlowRadio;
    private JRadioButton randomFlowRadio;
    private ButtonGroup flowTypeGroup;

    private JPanel detFlowSettingsPanel;
    private JPanel randomFlowSettingsPanel;

    // Детерминированный поток
    private JSpinner detIntervalSpinner;
    private JSpinner detSpeedSpinner;

    // Случайный поток - Время
    private JComboBox<DistributionLaw> timeDistLawComboBox;
    private JPanel timeParametersHostPanel; // Панель-хозяин для timeUniform/Normal/ExponentialPanel
    private JPanel timeUniformPanel, timeNormalPanel, timeExponentialPanel;
    private JSpinner timeUniformMinSpinner, timeUniformMaxSpinner;
    private JSpinner timeNormalMeanSpinner, timeNormalVarianceSpinner;
    private JSpinner timeExponentialIntensitySpinner;

    // Случайный поток - Скорость
    private JComboBox<DistributionLaw> speedDistLawComboBox;
    private JPanel speedParametersHostPanel; // Панель-хозяин для speedUniform/Normal/ExponentialPanel
    private JPanel speedUniformPanel, speedNormalPanel, speedExponentialPanel;
    private JSpinner speedUniformMinSpinner, speedUniformMaxSpinner;
    private JSpinner speedNormalMeanSpinner, speedNormalVarianceSpinner;
    private JSpinner speedExponentialIntensitySpinner;


    public SettingsDialog(Frame owner, SimulationParameters currentParams) {
        super(owner, "Настройки симуляции", true);
        this.params = currentParams;

        initComponents();
        layoutComponents();
        addListeners();
        loadParameters(); // Важно вызвать ПОСЛЕ layoutComponents и addListeners

        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // Дорога
        roadTypeComboBox = new JComboBox<>(RoadType.values());
        roadLengthSpinner = new JSpinner(new SpinnerNumberModel(5.0, 1.0, 50.0, 0.5));
        lanesPerDirectionSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));
        numDirectionsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 2, 1));

        // Тип потока
        deterministicFlowRadio = new JRadioButton("Детерминированный");
        randomFlowRadio = new JRadioButton("Случайный");
        flowTypeGroup = new ButtonGroup();
        flowTypeGroup.add(deterministicFlowRadio);
        flowTypeGroup.add(randomFlowRadio);

        // Детерминированный поток
        detIntervalSpinner = new JSpinner(new SpinnerNumberModel(15.0, 1.0, 300.0, 0.5));
        detSpeedSpinner = new JSpinner(new SpinnerNumberModel(60.0, 10.0, 150.0, 5.0));

        // Случайный поток - Время
        timeDistLawComboBox = new JComboBox<>(DistributionLaw.values());
        timeUniformMinSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.1, 300.0, 0.1));
        timeUniformMaxSpinner = new JSpinner(new SpinnerNumberModel(15.0, 0.2, 300.0, 0.1));
        timeNormalMeanSpinner = new JSpinner(new SpinnerNumberModel(20.0, 1.0, 300.0, 0.1));
        timeNormalVarianceSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.0, 100.0, 0.1));
        timeExponentialIntensitySpinner = new JSpinner(new SpinnerNumberModel(0.1, 0.001, 5.0, 0.01));

        // Случайный поток - Скорость
        speedDistLawComboBox = new JComboBox<>(DistributionLaw.values());
        speedUniformMinSpinner = new JSpinner(new SpinnerNumberModel(40.0, 10.0, 150.0, 1.0));
        speedUniformMaxSpinner = new JSpinner(new SpinnerNumberModel(80.0, 10.0, 150.0, 1.0));
        speedNormalMeanSpinner = new JSpinner(new SpinnerNumberModel(60.0, 10.0, 150.0, 1.0));
        speedNormalVarianceSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 100.0, 1.0));
        speedExponentialIntensitySpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));

        // Создаем панели для параметров каждого закона (наполним их в createRandomFlowSettingsPanel)
        timeUniformPanel = createParameterPanel(new String[]{"Мин. интервал (с):", "Макс. интервал (с):"}, timeUniformMinSpinner, timeUniformMaxSpinner);
        timeNormalPanel = createParameterPanel(new String[]{"МО интервала (с):", "Дисперсия интервала (с^2):"}, timeNormalMeanSpinner, timeNormalVarianceSpinner);
        timeExponentialPanel = createParameterPanel(new String[]{"Интенсивность (1/с):"}, timeExponentialIntensitySpinner);

        speedUniformPanel = createParameterPanel(new String[]{"Мин. скорость (км/ч):", "Макс. скорость (км/ч):"}, speedUniformMinSpinner, speedUniformMaxSpinner);
        speedNormalPanel = createParameterPanel(new String[]{"МО скорости (км/ч):", "Дисперсия скорости ((км/ч)^2):"}, speedNormalMeanSpinner, speedNormalVarianceSpinner);
        speedExponentialPanel = createParameterPanel(new String[]{"Параметр формы (скорость):"}, speedExponentialIntensitySpinner);
    }

    // Вспомогательный метод для создания панелей с параметрами законов
    private JPanel createParameterPanel(String[] labels, JComponent... components) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; panel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1; gbc.gridy = i; panel.add(components[i], gbc);
        }
        return panel;
    }


    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel mainSettingsPanel = new JPanel();
        mainSettingsPanel.setLayout(new BoxLayout(mainSettingsPanel, BoxLayout.Y_AXIS));
        mainSettingsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainSettingsPanel.add(createRoadSettingsPanel());
        mainSettingsPanel.add(Box.createVerticalStrut(10));
        mainSettingsPanel.add(createFlowTypeSelectionPanel());
        mainSettingsPanel.add(Box.createVerticalStrut(5));

        detFlowSettingsPanel = createDeterministicFlowSettingsPanel();
        randomFlowSettingsPanel = createRandomFlowSettingsPanel();

        mainSettingsPanel.add(detFlowSettingsPanel);
        mainSettingsPanel.add(randomFlowSettingsPanel);

        add(mainSettingsPanel, BorderLayout.CENTER);

        JButton saveButton = new JButton("Сохранить");
        saveButton.addActionListener(e -> saveAndClose());
        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createRoadSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Параметры дороги"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Тип дороги:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; panel.add(roadTypeComboBox, gbc);
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Длина (км):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(roadLengthSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 2; panel.add(new JLabel("Полос в одном направлении:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; panel.add(lanesPerDirectionSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Кол-во направлений:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; panel.add(numDirectionsSpinner, gbc);
        return panel;
    }

    private JPanel createFlowTypeSelectionPanel() {
        JPanel flowTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        flowTypePanel.setBorder(BorderFactory.createTitledBorder("Тип транспортного потока"));
        flowTypePanel.add(deterministicFlowRadio);
        flowTypePanel.add(randomFlowRadio);
        return flowTypePanel;
    }

    private JPanel createDeterministicFlowSettingsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(null,"Настройки детерминированного потока", TitledBorder.LEFT, TitledBorder.TOP));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Интервал появления (сек):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; panel.add(detIntervalSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Скорость машин (км/ч):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panel.add(detSpeedSpinner, gbc);
        return panel;
    }

    private JPanel createRandomFlowSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(null,"Настройки случайного потока", TitledBorder.LEFT, TitledBorder.TOP));

        // --- Настройки ВРЕМЕНИ ---
        JPanel timeSectionPanel = new JPanel(new BorderLayout(5,5)); // Это контейнер для ComboBox и timeParametersHostPanel
        timeSectionPanel.setBorder(BorderFactory.createTitledBorder("Время появления"));
        timeSectionPanel.add(timeDistLawComboBox, BorderLayout.NORTH);
        timeParametersHostPanel = new JPanel(new BorderLayout()); // Используем BorderLayout для смены панелей
        timeSectionPanel.add(timeParametersHostPanel, BorderLayout.CENTER);
        panel.add(timeSectionPanel);

        // --- Настройки СКОРОСТИ ---
        JPanel speedSectionPanel = new JPanel(new BorderLayout(5,5)); // Аналогично для скорости
        speedSectionPanel.setBorder(BorderFactory.createTitledBorder("Скоростной режим"));
        speedSectionPanel.add(speedDistLawComboBox, BorderLayout.NORTH);
        speedParametersHostPanel = new JPanel(new BorderLayout());
        speedSectionPanel.add(speedParametersHostPanel, BorderLayout.CENTER);
        panel.add(speedSectionPanel);

        return panel;
    }

    private void addListeners() {
        ActionListener flowTypeListener = e -> updateFlowPanelsVisibility();
        deterministicFlowRadio.addActionListener(flowTypeListener);
        randomFlowRadio.addActionListener(flowTypeListener);

        timeDistLawComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDistributionPanelVisibility((DistributionLaw) e.getItem(), timeParametersHostPanel,
                        timeUniformPanel, timeNormalPanel, timeExponentialPanel);
            }
        });

        speedDistLawComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateDistributionPanelVisibility((DistributionLaw) e.getItem(), speedParametersHostPanel,
                        speedUniformPanel, speedNormalPanel, speedExponentialPanel);
            }
        });
    }

    private void updateFlowPanelsVisibility() {
        boolean isRandom = randomFlowRadio.isSelected();
        detFlowSettingsPanel.setVisible(!isRandom);
        randomFlowSettingsPanel.setVisible(isRandom);
        if (this.getContentPane() != null) {
            this.pack();
        }
    }

    private void updateDistributionPanelVisibility(DistributionLaw selectedLaw, JPanel hostPanel,
                                                   JPanel uniformPanel, JPanel normalPanel, JPanel exponentialPanel) {
        hostPanel.removeAll(); // Очищаем предыдущую панель параметров

        switch (selectedLaw) {
            case UNIFORM:
                hostPanel.add(uniformPanel, BorderLayout.CENTER);
                break;
            case NORMAL:
                hostPanel.add(normalPanel, BorderLayout.CENTER);
                break;
            case EXPONENTIAL:
                hostPanel.add(exponentialPanel, BorderLayout.CENTER);
                break;
        }
        hostPanel.revalidate();
        hostPanel.repaint();
        //this.pack(); // Вызов pack здесь может быть слишком частым, лучше делать его в updateFlowPanelsVisibility
    }


    private void loadParameters() {
        roadTypeComboBox.setSelectedItem(params.getRoadType());
        roadLengthSpinner.setValue(params.getRoadLengthKm());
        lanesPerDirectionSpinner.setValue(params.getLanesPerDirection());
        numDirectionsSpinner.setValue(params.getNumberOfDirections());

        deterministicFlowRadio.setSelected(!params.isRandomFlow());
        randomFlowRadio.setSelected(params.isRandomFlow());

        detIntervalSpinner.setValue(params.getDeterministicIntervalSeconds());
        detSpeedSpinner.setValue(params.getDeterministicSpeedKmh());

        timeDistLawComboBox.setSelectedItem(params.getTimeDistributionLaw());
        timeUniformMinSpinner.setValue(params.getTimeUniformMinSec());
        timeUniformMaxSpinner.setValue(params.getTimeUniformMaxSec());
        timeNormalMeanSpinner.setValue(params.getTimeNormalMeanSec());
        timeNormalVarianceSpinner.setValue(params.getTimeNormalVarianceSec());
        timeExponentialIntensitySpinner.setValue(params.getTimeExponentialIntensityPerSec());

        speedDistLawComboBox.setSelectedItem(params.getSpeedDistributionLaw());
        speedUniformMinSpinner.setValue(params.getSpeedUniformMinKmh());
        speedUniformMaxSpinner.setValue(params.getSpeedUniformMaxKmh());
        speedNormalMeanSpinner.setValue(params.getSpeedNormalMeanKmh());
        speedNormalVarianceSpinner.setValue(params.getSpeedNormalVarianceKmh());
        speedExponentialIntensitySpinner.setValue(params.getSpeedExponentialIntensityPerKmh());

        updateFlowPanelsVisibility(); // Управляет видимостью detFlowSettingsPanel и randomFlowSettingsPanel
        // Обновляем панели законов после основной логики видимости
        updateDistributionPanelVisibility(params.getTimeDistributionLaw(), timeParametersHostPanel,
                timeUniformPanel, timeNormalPanel, timeExponentialPanel);
        updateDistributionPanelVisibility(params.getSpeedDistributionLaw(), speedParametersHostPanel,
                speedUniformPanel, speedNormalPanel, speedExponentialPanel);
    }

    private void saveAndClose() {
        params.setRoadType((RoadType) roadTypeComboBox.getSelectedItem());
        params.setRoadLengthKm(((Number) roadLengthSpinner.getValue()).doubleValue());
        params.setLanesPerDirection(((Number) lanesPerDirectionSpinner.getValue()).intValue());
        params.setNumberOfDirections(((Number) numDirectionsSpinner.getValue()).intValue());

        params.setRandomFlow(randomFlowRadio.isSelected());

        params.setDeterministicIntervalSeconds(((Number) detIntervalSpinner.getValue()).doubleValue());
        params.setDeterministicSpeedKmh(((Number) detSpeedSpinner.getValue()).doubleValue());

        params.setTimeDistributionLaw((DistributionLaw) timeDistLawComboBox.getSelectedItem());
        params.setTimeUniformMinSec(((Number) timeUniformMinSpinner.getValue()).doubleValue());
        params.setTimeUniformMaxSec(((Number) timeUniformMaxSpinner.getValue()).doubleValue());
        params.setTimeNormalMeanSec(((Number) timeNormalMeanSpinner.getValue()).doubleValue());
        params.setTimeNormalVarianceSec(((Number) timeNormalVarianceSpinner.getValue()).doubleValue());
        params.setTimeExponentialIntensityPerSec(((Number) timeExponentialIntensitySpinner.getValue()).doubleValue());

        params.setSpeedDistributionLaw((DistributionLaw) speedDistLawComboBox.getSelectedItem());
        params.setSpeedUniformMinKmh(((Number) speedUniformMinSpinner.getValue()).doubleValue());
        params.setSpeedUniformMaxKmh(((Number) speedUniformMaxSpinner.getValue()).doubleValue());
        params.setSpeedNormalMeanKmh(((Number) speedNormalMeanSpinner.getValue()).doubleValue());
        params.setSpeedNormalVarianceKmh(((Number) speedNormalVarianceSpinner.getValue()).doubleValue());
        params.setSpeedExponentialIntensityPerKmh(((Number) speedExponentialIntensitySpinner.getValue()).doubleValue());

        settingsSaved = true;
        dispose();
    }

    public boolean showDialog() {
        setVisible(true);
        return settingsSaved;
    }
}
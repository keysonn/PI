package com.trafficsimulation.gui;

import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.simulation.SimulationParameters;

import javax.swing.*;
import java.awt.*;
// ActionListener не нужен, если используем лямбды напрямую

public class RoadSettingsDialog extends JDialog {

    private SimulationParameters params;
    private boolean settingsSaved = false;

    // Компоненты GUI
    private JComboBox<RoadType> roadTypeComboBox;
    private JSpinner roadLengthSpinner;
    private JToggleButton dirOneWayButton, dirTwoWayButton;
    private ButtonGroup directionGroup;
    private JSlider lanesSlider;

    private JPanel commonRoadSettingsPanel; // Панель с общими настройками (направление, полосы)
    private JPanel tunnelSpecificSettingsPanel; // Панель с настройками светофоров для тоннеля
    private JSpinner tunnelRedLightSpinner, tunnelGreenLightSpinner;

    public RoadSettingsDialog(Frame owner, SimulationParameters currentParams) {
        super(owner, "Настройка параметров автодороги", true); // Модальное окно
        this.params = currentParams;

        initComponents();
        layoutComponents();
        addListeners();
        loadParameters();

        updateVisibilityBasedOnRoadType(); // Первоначальная настройка видимости

        pack();
        // Устанавливаем минимальную ширину, чтобы компоненты не сжимались сильно
        setMinimumSize(new Dimension(Math.max(480, getPreferredSize().width), getPreferredSize().height));
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // Тип дороги
        roadTypeComboBox = new JComboBox<>(new RoadType[]{RoadType.CITY_ROAD, RoadType.HIGHWAY, RoadType.TUNNEL});

        // Общие для CITY_ROAD, HIGHWAY (используются и для TUNNEL, но могут быть скрыты)
        roadLengthSpinner = new JSpinner(new SpinnerNumberModel(5.0, 1.0, 50.0, 0.5)); // км
        dirOneWayButton = new JToggleButton("Одностороннее", true);
        dirTwoWayButton = new JToggleButton("Двустороннее");
        directionGroup = new ButtonGroup();
        directionGroup.add(dirOneWayButton);
        directionGroup.add(dirTwoWayButton);
        lanesSlider = new JSlider(JSlider.HORIZONTAL, 1, 4, 2); // полос в одном направлении
        lanesSlider.setMajorTickSpacing(1);
        lanesSlider.setPaintTicks(true);
        lanesSlider.setPaintLabels(true);
        lanesSlider.setSnapToTicks(true);

        // Только для TUNNEL
        tunnelRedLightSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1)); // сек
        tunnelGreenLightSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1)); // сек
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS)); // Вертикальное расположение секций
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Выбор типа автодороги (всегда виден)
        JPanel roadTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roadTypePanel.add(new JLabel("Выберите тип автодороги:"));
        roadTypePanel.add(roadTypeComboBox);
        mainPanel.add(roadTypePanel);
        mainPanel.add(Box.createVerticalStrut(5));

        // 2. Панель с общими настройками (длина, направление, полосы)
        commonRoadSettingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcCommon = new GridBagConstraints();
        gbcCommon.insets = new Insets(5, 5, 5, 5);
        gbcCommon.anchor = GridBagConstraints.WEST;

        // Длина участка (общая для всех)
        gbcCommon.gridy = 0; gbcCommon.gridx = 0; commonRoadSettingsPanel.add(new JLabel("Длина участка автодороги:"), gbcCommon);
        JPanel lengthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lengthPanel.add(roadLengthSpinner); lengthPanel.add(new JLabel(" км"));
        gbcCommon.gridx = 1; commonRoadSettingsPanel.add(lengthPanel, gbcCommon);

        // Направление движения (не для тоннеля)
        gbcCommon.gridy++; gbcCommon.gridx = 0; commonRoadSettingsPanel.add(new JLabel("Направление движения:"), gbcCommon);
        JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dirPanel.add(dirOneWayButton); dirPanel.add(dirTwoWayButton);
        gbcCommon.gridx = 1; commonRoadSettingsPanel.add(dirPanel, gbcCommon);

        // Количество полос (не для тоннеля)
        gbcCommon.gridy++; gbcCommon.gridx = 0; commonRoadSettingsPanel.add(new JLabel("Количество полос (в 1 напр.):"), gbcCommon);
        gbcCommon.gridx = 1; gbcCommon.fill = GridBagConstraints.HORIZONTAL; commonRoadSettingsPanel.add(lanesSlider, gbcCommon);
        gbcCommon.fill = GridBagConstraints.NONE; // Сброс fill

        mainPanel.add(commonRoadSettingsPanel);

        // 3. Панель с настройками специфичными для тоннеля (светофоры)
        tunnelSpecificSettingsPanel = new JPanel(new GridBagLayout());
        // tunnelSpecificSettingsPanel.setBorder(BorderFactory.createTitledBorder("Настройки для тоннеля"));
        GridBagConstraints gbcTunnel = new GridBagConstraints();
        gbcTunnel.insets = new Insets(5, 5, 5, 5);
        gbcTunnel.anchor = GridBagConstraints.WEST;

        gbcTunnel.gridy = 0; gbcTunnel.gridx = 0; tunnelSpecificSettingsPanel.add(new JLabel("Длина красного света:"), gbcTunnel);
        JPanel redPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        redPanel.add(tunnelRedLightSpinner); redPanel.add(new JLabel(" секунд"));
        gbcTunnel.gridx = 1; tunnelSpecificSettingsPanel.add(redPanel, gbcTunnel);

        gbcTunnel.gridy++; gbcTunnel.gridx = 0; tunnelSpecificSettingsPanel.add(new JLabel("Длина зеленого света:"), gbcTunnel);
        JPanel greenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        greenPanel.add(tunnelGreenLightSpinner); greenPanel.add(new JLabel(" секунд"));
        gbcTunnel.gridx = 1; tunnelSpecificSettingsPanel.add(greenPanel, gbcTunnel);
        mainPanel.add(tunnelSpecificSettingsPanel);

        add(mainPanel, BorderLayout.CENTER);

        // Кнопка Сохранить
        JButton saveButton = new JButton("Сохранить настройки");
        saveButton.addActionListener(e -> saveAndClose());
        JPanel buttonPanelSouth = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Кнопка по центру
        buttonPanelSouth.add(saveButton);
        add(buttonPanelSouth, BorderLayout.SOUTH);
    }

    private void addListeners() {
        roadTypeComboBox.addActionListener(e -> updateVisibilityBasedOnRoadType());
    }

    private void updateVisibilityBasedOnRoadType() {
        RoadType selectedType = (RoadType) roadTypeComboBox.getSelectedItem();
        boolean isTunnel = (selectedType == RoadType.TUNNEL);

        // Скрываем/показываем настройки направления и полос
        // Эти элементы находятся внутри commonRoadSettingsPanel, кроме roadLengthSpinner
        // Проще скрыть/показать всю панель commonRoadSettingsPanel, если она содержит только их
        // Но roadLengthSpinner общий. Поэтому будем управлять видимостью отдельных строк.

        // Найдем компоненты для скрытия (это не самый элегантный способ, лучше хранить ссылки на панели/строки)
        for (Component comp : commonRoadSettingsPanel.getComponents()) {
            if (comp instanceof JLabel) {
                String labelText = ((JLabel) comp).getText();
                if (labelText.startsWith("Направление движения:") || labelText.startsWith("Количество полос")) {
                    comp.setVisible(!isTunnel); // Скрываем метку
                    // И соответствующий компонент ввода тоже
                    Component inputComp = getNextNonLabelComponent(commonRoadSettingsPanel, comp);
                    if (inputComp != null) inputComp.setVisible(!isTunnel);
                }
            }
        }
        lanesSlider.setVisible(!isTunnel); // Слайдер напрямую

        tunnelSpecificSettingsPanel.setVisible(isTunnel);

        SwingUtilities.invokeLater(() -> {
            if (isVisible()) { // pack() только если диалог видим, чтобы избежать проблем при инициализации
                pack();
            }
        });
    }

    // Вспомогательный метод для поиска следующего компонента, не являющегося JLabel
    private Component getNextNonLabelComponent(Container parent, Component currentLabel) {
        Component[] components = parent.getComponents();
        for (int i = 0; i < components.length - 1; i++) {
            if (components[i] == currentLabel && !(components[i+1] instanceof JLabel)) {
                return components[i+1];
            }
        }
        return null;
    }


    private void loadParameters() {
        roadTypeComboBox.setSelectedItem(params.getRoadType());
        roadLengthSpinner.setValue(params.getRoadLengthKm());

        if (params.getRoadType() != RoadType.TUNNEL) {
            if (params.getNumberOfDirections() == 1) {
                dirOneWayButton.setSelected(true);
            } else {
                dirTwoWayButton.setSelected(true);
            }
            lanesSlider.setValue(params.getLanesPerDirection());
        }

        // Загружаем параметры светофоров тоннеля (используем значения из SimulationParameters)
        tunnelRedLightSpinner.setValue(params.getTunnelRedDuration());
        tunnelGreenLightSpinner.setValue(params.getTunnelGreenDuration());
    }

    private void saveAndClose() {
        params.setRoadType((RoadType) roadTypeComboBox.getSelectedItem());
        params.setRoadLengthKm(((Number) roadLengthSpinner.getValue()).doubleValue());

        if (params.getRoadType() != RoadType.TUNNEL) {
            params.setNumberOfDirections(dirOneWayButton.isSelected() ? 1 : 2);
            params.setLanesPerDirection(lanesSlider.getValue());
        } else {
            // Для тоннеля, параметры направления/полос не сохраняем из GUI,
            // они могут быть фиксированными или установлены по умолчанию в SimulationParameters/Road
            // Сохраняем длительности светофоров тоннеля
            params.setTunnelRedDuration(((Number) tunnelRedLightSpinner.getValue()).doubleValue());
            params.setTunnelGreenDuration(((Number) tunnelGreenLightSpinner.getValue()).doubleValue());
        }

        settingsSaved = true;
        dispose();
    }

    public boolean showDialog() {
        // Перед показом диалога, обновим видимость на основе текущих параметров
        loadParameters(); // Загружаем актуальные параметры
        updateVisibilityBasedOnRoadType(); // Устанавливаем правильную видимость
        setVisible(true);
        return settingsSaved;
    }
}
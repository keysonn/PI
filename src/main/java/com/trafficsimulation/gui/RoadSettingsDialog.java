package com.trafficsimulation.gui;

import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.simulation.SimulationParameters;

import javax.swing.*;
import java.awt.*;

public class RoadSettingsDialog extends JDialog {

    private SimulationParameters params;
    private boolean settingsSaved = false;

    private JComboBox<RoadType> roadTypeComboBox;
    private JSpinner roadLengthSpinner;

    private JPanel nonTunnelSettingsPanel;
    private JToggleButton dirOneWayButton, dirTwoWayButton;
    private ButtonGroup directionGroup;
    private JSlider lanesSlider;

    private JPanel tunnelSettingsPanel;
    private JSpinner tunnelRedLightSpinner, tunnelGreenLightSpinner, tunnelClearanceSpinner; // Добавлен clearance

    public RoadSettingsDialog(Frame owner, SimulationParameters currentParams) {
        super(owner, "Настройка параметров автодороги", true);
        this.params = currentParams;
        initComponents();
        layoutComponents();
        addListeners();
        loadParameters();
        updateVisibilityBasedOnRoadType();
        pack();
        setMinimumSize(new Dimension(Math.max(500, getPreferredSize().width), getPreferredSize().height)); // Увеличил мин. ширину
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        roadTypeComboBox = new JComboBox<>(new RoadType[]{RoadType.CITY_ROAD, RoadType.HIGHWAY, RoadType.TUNNEL});
        roadLengthSpinner = new JSpinner(new SpinnerNumberModel(5.0, 1.0, 50.0, 0.5));
        dirOneWayButton = new JToggleButton("Одностороннее", true);
        dirTwoWayButton = new JToggleButton("Двустороннее");
        directionGroup = new ButtonGroup();
        directionGroup.add(dirOneWayButton); directionGroup.add(dirTwoWayButton);
        lanesSlider = new JSlider(JSlider.HORIZONTAL, 1, 4, 2);
        lanesSlider.setMajorTickSpacing(1); lanesSlider.setPaintTicks(true);
        lanesSlider.setPaintLabels(true); lanesSlider.setSnapToTicks(true);
        tunnelRedLightSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        tunnelGreenLightSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        tunnelClearanceSpinner = new JSpinner(new SpinnerNumberModel(10, 5, 60, 1)); // Время очистки 5-60 сек
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel roadTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roadTypePanel.add(new JLabel("Выберите тип автодороги:"));
        roadTypePanel.add(roadTypeComboBox);
        mainPanel.add(roadTypePanel);
        mainPanel.add(Box.createVerticalStrut(5));

        // Длина участка - всегда видима
        JPanel lengthPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcLength = new GridBagConstraints();
        gbcLength.insets = new Insets(5,5,5,5); gbcLength.anchor = GridBagConstraints.WEST;
        gbcLength.gridx = 0; gbcLength.gridy = 0; lengthPanel.add(new JLabel("Укажите длину участка автодороги:"), gbcLength);
        JPanel valueLengthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
        valueLengthPanel.add(roadLengthSpinner); valueLengthPanel.add(new JLabel(" км"));
        gbcLength.gridx = 1; lengthPanel.add(valueLengthPanel, gbcLength);
        mainPanel.add(lengthPanel);

        nonTunnelSettingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcNonTunnel = new GridBagConstraints();
        gbcNonTunnel.insets = new Insets(5, 5, 5, 5); gbcNonTunnel.anchor = GridBagConstraints.WEST;
        gbcNonTunnel.gridy = 0; gbcNonTunnel.gridx = 0; nonTunnelSettingsPanel.add(new JLabel("Направление движения:"), gbcNonTunnel);
        JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dirPanel.add(dirOneWayButton); dirPanel.add(dirTwoWayButton);
        gbcNonTunnel.gridx = 1; nonTunnelSettingsPanel.add(dirPanel, gbcNonTunnel);
        gbcNonTunnel.gridy++; gbcNonTunnel.gridx = 0; nonTunnelSettingsPanel.add(new JLabel("Количество полос (в 1 напр.):"), gbcNonTunnel);
        gbcNonTunnel.gridx = 1; gbcNonTunnel.fill = GridBagConstraints.HORIZONTAL; nonTunnelSettingsPanel.add(lanesSlider, gbcNonTunnel);
        mainPanel.add(nonTunnelSettingsPanel);

        tunnelSettingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcTunnel = new GridBagConstraints();
        gbcTunnel.insets = new Insets(5, 5, 5, 5); gbcTunnel.anchor = GridBagConstraints.WEST;
        gbcTunnel.gridy = 0; gbcTunnel.gridx = 0; tunnelSettingsPanel.add(new JLabel("Длина красного света:"), gbcTunnel);
        JPanel redPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        redPanel.add(tunnelRedLightSpinner); redPanel.add(new JLabel(" секунд"));
        gbcTunnel.gridx = 1; tunnelSettingsPanel.add(redPanel, gbcTunnel);
        gbcTunnel.gridy++; gbcTunnel.gridx = 0; tunnelSettingsPanel.add(new JLabel("Длина зеленого света:"), gbcTunnel);
        JPanel greenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        greenPanel.add(tunnelGreenLightSpinner); greenPanel.add(new JLabel(" секунд"));
        gbcTunnel.gridx = 1; tunnelSettingsPanel.add(greenPanel, gbcTunnel);
        gbcTunnel.gridy++; gbcTunnel.gridx = 0; tunnelSettingsPanel.add(new JLabel("Время очистки тоннеля:"), gbcTunnel); // Новое поле
        JPanel clearancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        clearancePanel.add(tunnelClearanceSpinner); clearancePanel.add(new JLabel(" секунд"));
        gbcTunnel.gridx = 1; tunnelSettingsPanel.add(clearancePanel, gbcTunnel);
        mainPanel.add(tunnelSettingsPanel);

        add(mainPanel, BorderLayout.CENTER);
        JButton saveButton = new JButton("Сохранить настройки");
        saveButton.addActionListener(e -> saveAndClose());
        JPanel buttonPanelSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanelSouth.add(saveButton);
        add(buttonPanelSouth, BorderLayout.SOUTH);
    }

    private void addListeners() {
        roadTypeComboBox.addActionListener(e -> updateVisibilityBasedOnRoadType());
    }

    private void updateVisibilityBasedOnRoadType() {
        RoadType selectedType = (RoadType) roadTypeComboBox.getSelectedItem();
        boolean isTunnel = (selectedType == RoadType.TUNNEL);
        if (nonTunnelSettingsPanel != null) nonTunnelSettingsPanel.setVisible(!isTunnel);
        if (tunnelSettingsPanel != null) tunnelSettingsPanel.setVisible(isTunnel);
        SwingUtilities.invokeLater(() -> { if (isVisible()) pack(); });
    }

    private void loadParameters() {
        roadTypeComboBox.setSelectedItem(params.getRoadType());
        roadLengthSpinner.setValue(params.getRoadLengthKm());
        if (params.getNumberOfDirections() == 1) dirOneWayButton.setSelected(true);
        else dirTwoWayButton.setSelected(true);
        lanesSlider.setValue(params.getLanesPerDirection());
        tunnelRedLightSpinner.setValue(params.getTunnelDefaultRedDuration());
        tunnelGreenLightSpinner.setValue(params.getTunnelDefaultGreenDuration());
        tunnelClearanceSpinner.setValue(params.getTunnelClearanceTime());
    }

    private void saveAndClose() {
        params.setRoadType((RoadType) roadTypeComboBox.getSelectedItem());
        params.setRoadLengthKm(((Number) roadLengthSpinner.getValue()).doubleValue());

        if (params.getRoadType() == RoadType.TUNNEL) {
            params.setNumberOfDirections(2);  // Тоннель для реверса всегда двунаправленный в данных
            params.setLanesPerDirection(1);   // Но в каждый момент времени активна одна полоса
            params.setTunnelDefaultRedDuration(((Number) tunnelRedLightSpinner.getValue()).doubleValue());
            params.setTunnelDefaultGreenDuration(((Number) tunnelGreenLightSpinner.getValue()).doubleValue());
            params.setTunnelClearanceTime(((Number) tunnelClearanceSpinner.getValue()).doubleValue());
        } else {
            params.setNumberOfDirections(dirOneWayButton.isSelected() ? 1 : 2);
            params.setLanesPerDirection(lanesSlider.getValue());
        }
        settingsSaved = true;
        dispose();
    }

    public boolean showDialog() {
        loadParameters();
        updateVisibilityBasedOnRoadType();
        setVisible(true);
        return settingsSaved;
    }
}
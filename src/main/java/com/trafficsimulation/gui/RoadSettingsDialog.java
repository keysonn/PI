package com.trafficsimulation.gui;

import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.simulation.SimulationParameters;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;


public class RoadSettingsDialog extends JDialog {

    private SimulationParameters params;
    private boolean settingsSaved = false;

    private JToggleButton cityRoadButton, highwayButton, tunnelButton;
    private ButtonGroup roadTypeGroup;

    private JToggleButton одностороннееButton, двухстороннееButton;
    private ButtonGroup directionGroup;

    private JSlider lanesPerDirectionSlider;
    private JLabel lanesLabel;
    private JLabel directionLabel;

    private JLabel redLightDurationLabel, greenLightDurationLabel;
    private JSpinner redLightDurationSpinner, greenLightDurationSpinner;
    private JPanel tunnelSpecificPanel;
    private JPanel nonTunnelSpecificPanel;

    private final Dimension spinnerPreferredSize = new Dimension(70, 25);
    private final Dimension dialogBaseMinimumSize = new Dimension(450, 300); // Увеличим немного мин. высоту


    public RoadSettingsDialog(Frame owner, SimulationParameters currentParams) {
        super(owner, "Настройка параметров автодороги", true);
        this.params = currentParams;

        initComponents();
        layoutComponents();
        addListeners();

        loadParameters();
        updateFieldVisibility();

        pack();
        Dimension packedSize = getSize();
        setMinimumSize(new Dimension(Math.max(packedSize.width, dialogBaseMinimumSize.width),
                Math.max(packedSize.height, dialogBaseMinimumSize.height)));
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        cityRoadButton = new JToggleButton(RoadType.CITY_ROAD.getDisplayName());
        highwayButton = new JToggleButton(RoadType.HIGHWAY.getDisplayName());
        tunnelButton = new JToggleButton(RoadType.TUNNEL.getDisplayName());
        roadTypeGroup = new ButtonGroup();
        roadTypeGroup.add(cityRoadButton);
        roadTypeGroup.add(highwayButton);
        roadTypeGroup.add(tunnelButton);

        directionLabel = new JLabel("Выберите направление движения:");
        одностороннееButton = new JToggleButton("Одностороннее");
        двухстороннееButton = new JToggleButton("Двустороннее");
        directionGroup = new ButtonGroup();
        directionGroup.add(одностороннееButton);
        directionGroup.add(двухстороннееButton);

        lanesLabel = new JLabel("Выберите количество полос:");
        lanesPerDirectionSlider = new JSlider(1, 4, 1);
        lanesPerDirectionSlider.setMajorTickSpacing(1);
        lanesPerDirectionSlider.setPaintTicks(true);
        lanesPerDirectionSlider.setPaintLabels(true);
        lanesPerDirectionSlider.setSnapToTicks(true);

        redLightDurationLabel = new JLabel("Задайте длину красного света:");
        redLightDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        redLightDurationSpinner.setPreferredSize(spinnerPreferredSize);
        greenLightDurationLabel = new JLabel("Задайте длину зеленого света:");
        greenLightDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        greenLightDurationSpinner.setPreferredSize(spinnerPreferredSize);
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel roadTypePanelContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roadTypePanelContainer.add(new JLabel("Выберите тип автодороги:"));
        add(roadTypePanelContainer, gbc);

        JPanel roadTypeButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0,0));
        roadTypeButtonsPanel.add(cityRoadButton);
        roadTypeButtonsPanel.add(highwayButton);
        roadTypeButtonsPanel.add(tunnelButton);
        gbc.gridy = 1;
        add(roadTypeButtonsPanel, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        nonTunnelSpecificPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcNonTunnel = new GridBagConstraints();
        gbcNonTunnel.insets = new Insets(5, 0, 5, 0);
        gbcNonTunnel.anchor = GridBagConstraints.WEST;

        gbcNonTunnel.gridx = 0; gbcNonTunnel.gridy = 0; gbcNonTunnel.fill = GridBagConstraints.NONE; gbcNonTunnel.weightx = 0.0;
        nonTunnelSpecificPanel.add(directionLabel, gbcNonTunnel);
        JPanel directionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        directionButtonsPanel.add(одностороннееButton);
        directionButtonsPanel.add(двухстороннееButton);
        gbcNonTunnel.gridx = 1; gbcNonTunnel.fill = GridBagConstraints.NONE; gbcNonTunnel.anchor = GridBagConstraints.EAST; gbcNonTunnel.weightx = 1.0;
        nonTunnelSpecificPanel.add(directionButtonsPanel, gbcNonTunnel);

        gbcNonTunnel.gridx = 0; gbcNonTunnel.gridy = 1; gbcNonTunnel.fill = GridBagConstraints.NONE; gbcNonTunnel.weightx = 0.0;
        nonTunnelSpecificPanel.add(lanesLabel, gbcNonTunnel);
        gbcNonTunnel.gridx = 1; gbcNonTunnel.fill = GridBagConstraints.HORIZONTAL; gbcNonTunnel.anchor = GridBagConstraints.EAST; gbcNonTunnel.weightx = 1.0;
        nonTunnelSpecificPanel.add(lanesPerDirectionSlider, gbcNonTunnel);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(nonTunnelSpecificPanel, gbc);

        tunnelSpecificPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcTunnel = new GridBagConstraints();
        gbcTunnel.insets = new Insets(5, 0, 5, 0);
        gbcTunnel.anchor = GridBagConstraints.WEST;
        gbcTunnel.gridy = 0;

        gbcTunnel.gridx = 0; gbcTunnel.weightx = 0.3; gbcTunnel.fill = GridBagConstraints.HORIZONTAL;
        tunnelSpecificPanel.add(redLightDurationLabel, gbcTunnel);
        gbcTunnel.gridx = 1; gbcTunnel.weightx = 0.0; gbcTunnel.fill = GridBagConstraints.NONE; gbcTunnel.anchor = GridBagConstraints.EAST;
        tunnelSpecificPanel.add(redLightDurationSpinner, gbcTunnel);
        gbcTunnel.gridx = 2; gbcTunnel.weightx = 0.0; gbcTunnel.fill = GridBagConstraints.NONE; gbcTunnel.anchor = GridBagConstraints.WEST;
        tunnelSpecificPanel.add(new JLabel("секунд"), gbcTunnel);

        gbcTunnel.gridy = 1;
        gbcTunnel.gridx = 0; gbcTunnel.weightx = 0.3; gbcTunnel.fill = GridBagConstraints.HORIZONTAL; gbcTunnel.anchor = GridBagConstraints.WEST;
        tunnelSpecificPanel.add(greenLightDurationLabel, gbcTunnel);
        gbcTunnel.gridx = 1; gbcTunnel.weightx = 0.0; gbcTunnel.fill = GridBagConstraints.NONE; gbcTunnel.anchor = GridBagConstraints.EAST;
        tunnelSpecificPanel.add(greenLightDurationSpinner, gbcTunnel);
        gbcTunnel.gridx = 2; gbcTunnel.weightx = 0.0; gbcTunnel.fill = GridBagConstraints.NONE; gbcTunnel.anchor = GridBagConstraints.WEST;
        tunnelSpecificPanel.add(new JLabel("секунд"), gbcTunnel);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(tunnelSpecificPanel, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(20, 10, 10, 10);
        JButton saveButton = new JButton("Сохранить настройки");
        saveButton.addActionListener(e -> saveAndClose());
        add(saveButton, gbc);
    }

    private void addListeners() {
        ActionListener roadTypeListener = e -> updateFieldVisibility();
        cityRoadButton.addActionListener(roadTypeListener);
        highwayButton.addActionListener(roadTypeListener);
        tunnelButton.addActionListener(roadTypeListener);
    }

    private RoadType getSelectedRoadType() {
        if (cityRoadButton.isSelected()) return RoadType.CITY_ROAD;
        if (highwayButton.isSelected()) return RoadType.HIGHWAY;
        if (tunnelButton.isSelected()) return RoadType.TUNNEL;
        return params.getRoadType() != null ? params.getRoadType() : RoadType.CITY_ROAD;
    }

    private void updateFieldVisibility() {
        RoadType selectedType = getSelectedRoadType();
        boolean isTunnel = (selectedType == RoadType.TUNNEL);

        nonTunnelSpecificPanel.setVisible(!isTunnel);
        tunnelSpecificPanel.setVisible(isTunnel);

        directionLabel.setEnabled(!isTunnel);
        одностороннееButton.setEnabled(!isTunnel);
        двухстороннееButton.setEnabled(!isTunnel);
        lanesLabel.setEnabled(!isTunnel);
        lanesPerDirectionSlider.setEnabled(!isTunnel);

        if(isTunnel) {
            двухстороннееButton.setSelected(true);
            lanesPerDirectionSlider.setValue(1);
        }
        pack();
        Dimension currentSize = getSize();
        setMinimumSize(new Dimension(Math.max(dialogBaseMinimumSize.width, currentSize.width),
                Math.max(dialogBaseMinimumSize.height, currentSize.height)));
        if (currentSize.height < getMinimumSize().height || currentSize.width < getMinimumSize().width) {
            setSize(getMinimumSize());
        }
    }


    private void loadParameters() {
        RoadType currentType = params.getRoadType();
        if (currentType == RoadType.CITY_ROAD) cityRoadButton.setSelected(true);
        else if (currentType == RoadType.HIGHWAY) highwayButton.setSelected(true);
        else if (currentType == RoadType.TUNNEL) tunnelButton.setSelected(true);
        else cityRoadButton.setSelected(true);

        if (params.getNumberOfDirections() == 1 && currentType != RoadType.TUNNEL) {
            одностороннееButton.setSelected(true);
        } else {
            двухстороннееButton.setSelected(true);
        }
        lanesPerDirectionSlider.setValue(params.getLanesPerDirection());

        redLightDurationSpinner.setValue(params.getTunnelDefaultRedDuration());
        greenLightDurationSpinner.setValue(params.getTunnelDefaultGreenDuration());
    }

    private void saveAndClose() {
        RoadType selectedRoadType = getSelectedRoadType();
        params.setRoadType(selectedRoadType);

        if (selectedRoadType == RoadType.TUNNEL) {
            params.setNumberOfDirections(2);
            params.setLanesPerDirection(1);
            params.setTunnelDefaultRedDuration(((Number) redLightDurationSpinner.getValue()).doubleValue());
            params.setTunnelDefaultGreenDuration(((Number) greenLightDurationSpinner.getValue()).doubleValue());
        } else {
            params.setNumberOfDirections(одностороннееButton.isSelected() ? 1 : 2);
            params.setLanesPerDirection(lanesPerDirectionSlider.getValue());
        }

        settingsSaved = true;
        dispose();
    }

    public boolean showDialog() {
        loadParameters();
        updateFieldVisibility();
        setVisible(true);
        return settingsSaved;
    }
}
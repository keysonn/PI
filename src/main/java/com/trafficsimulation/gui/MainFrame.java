package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadSign;
import com.trafficsimulation.model.RoadSignType;
import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;
import com.trafficsimulation.simulation.SimulationEngine;
import com.trafficsimulation.simulation.SimulationParameters;


public class MainFrame extends JFrame {

    private SimulationPanel simulationPanel;
    private SimulationEngine simulationEngine;
    private SimulationParameters simulationParameters;

    private JButton roadSettingsButton;
    private JButton modelingSettingsButton;
    private JButton addTrafficLightIconButton;
    private JButton addRoadSignIconButton;
    private JButton helpButton;

    private JToggleButton speed1xButton, speed2xButton, speed3xButton;
    private ButtonGroup speedGroup;
    private JButton runGenerationButton;
    private JButton pauseButton;
    private JButton stopButton;

    private enum PlacementMode { NONE, ADD_TRAFFIC_LIGHT, ADD_ROAD_SIGN }
    private PlacementMode currentPlacementMode = PlacementMode.NONE;

    private static final double MIN_OBJECT_SPACING_METERS = 15.0;
    private static final double MIN_EDGE_SPACING_METERS = 7.5;
    private static final int ROAD_RENDER_HEIGHT_FOR_CLICK = 80;


    public MainFrame() {
        setTitle("Система моделирования движения транспорта v1.2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 700));

        simulationParameters = new SimulationParameters();
        simulationPanel = new SimulationPanel();
        simulationEngine = new SimulationEngine(simulationParameters, simulationPanel);

        simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());

        JToolBar topToolBar = createTopToolBar();
        JPanel bottomControlPanel = createBottomControlPanel();

        add(topToolBar, BorderLayout.NORTH);
        add(simulationPanel, BorderLayout.CENTER);
        add(bottomControlPanel, BorderLayout.SOUTH);

        addSimulationPanelMouseListener();

        pack();
        setLocationRelativeTo(null);
    }

    private JToolBar createTopToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        roadSettingsButton = new JButton("Параметры автодороги");
        modelingSettingsButton = new JButton("Параметры моделирования");
        ImageIcon lightIcon = loadImageIcon("icons/traffic_light_icon.png", "Светофор");
        ImageIcon signIcon = loadImageIcon("icons/road_sign_icon.png", "Знак");
        addTrafficLightIconButton = new JButton(lightIcon);
        addTrafficLightIconButton.setToolTipText("Добавить светофор (макс. 2)");
        if (lightIcon == null) addTrafficLightIconButton.setText("Св");
        addRoadSignIconButton = new JButton(signIcon);
        addRoadSignIconButton.setToolTipText("Добавить дорожный знак");
        if (signIcon == null) addRoadSignIconButton.setText("Зн");
        helpButton = new JButton("Справка");
        roadSettingsButton.addActionListener(e -> openRoadSettingsDialog());
        modelingSettingsButton.addActionListener(e -> openModelingSettingsDialog());
        addTrafficLightIconButton.addActionListener(e -> {
            if (!canPlaceObject()) return;
            if (simulationEngine.getRoad() != null && simulationEngine.getRoad().getTrafficLights().size() >= 2) {
                JOptionPane.showMessageDialog(this, "Достигнут лимит светофоров (макс. 2).", "Лимит", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            currentPlacementMode = PlacementMode.ADD_TRAFFIC_LIGHT;
            simulationPanel.setPlacementMode(true, "Светофор");
        });
        addRoadSignIconButton.addActionListener(e -> {
            if (!canPlaceObject()) return;
            currentPlacementMode = PlacementMode.ADD_ROAD_SIGN;
            simulationPanel.setPlacementMode(true, "Знак огр. скорости");
        });
        helpButton.addActionListener(e -> showAboutDialog());
        toolBar.add(roadSettingsButton);
        toolBar.add(modelingSettingsButton);
        toolBar.addSeparator(new Dimension(10,0));
        toolBar.add(addTrafficLightIconButton);
        toolBar.add(addRoadSignIconButton);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(helpButton);
        return toolBar;
    }

    private ImageIcon loadImageIcon(String path, String description) {
        URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            ImageIcon icon = new ImageIcon(imgURL, description);
            if (icon.getIconWidth() > 24 || icon.getIconHeight() > 24) {
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImg, description);
            }
            return icon;
        } else {
            System.err.println("Не удалось загрузить иконку: " + path);
            return null;
        }
    }

    private JPanel createBottomControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        speed1xButton = new JToggleButton("1x", true);
        speed2xButton = new JToggleButton("2x");
        speed3xButton = new JToggleButton("3x");
        speedGroup = new ButtonGroup();
        speedGroup.add(speed1xButton); speedGroup.add(speed2xButton); speedGroup.add(speed3xButton);
        ActionListener speedListener = e -> {
            double factor = 1.0;
            if (speed2xButton.isSelected()) factor = 2.0;
            else if (speed3xButton.isSelected()) factor = 3.0;
            simulationParameters.setSimulationSpeedFactor(factor);
        };
        speed1xButton.addActionListener(speedListener);
        speed2xButton.addActionListener(speedListener);
        speed3xButton.addActionListener(speedListener);
        speedPanel.add(new JLabel("Скорость:"));
        speedPanel.add(speed1xButton);
        speedPanel.add(speed2xButton);
        speedPanel.add(speed3xButton);
        JPanel mainControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        runGenerationButton = new JButton("Запустить генерацию!");
        pauseButton = new JButton("Пауза");
        stopButton = new JButton("Стоп");
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);
        runGenerationButton.addActionListener(e -> {
            currentPlacementMode = PlacementMode.NONE;
            simulationPanel.setPlacementMode(false, null);
            simulationEngine.startSimulation();
            setBottomControlsEnabledState(false);
            runGenerationButton.setText("Генерация...");
        });
        pauseButton.addActionListener(e -> {
            if (simulationEngine.isRunning() && !simulationEngine.isPaused()) {
                simulationEngine.pauseSimulation();
                pauseButton.setText("Продолжить");
            } else if (simulationEngine.isRunning() && simulationEngine.isPaused()){
                simulationEngine.resumeSimulation();
                pauseButton.setText("Пауза");
            }
        });
        stopButton.addActionListener(e -> {
            simulationEngine.stopSimulation();
            setBottomControlsEnabledState(true);
            runGenerationButton.setText("Запустить генерацию!");
            currentPlacementMode = PlacementMode.NONE;
            simulationPanel.setPlacementMode(false, null);
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        });
        mainControlsPanel.add(pauseButton);
        mainControlsPanel.add(stopButton);
        mainControlsPanel.add(runGenerationButton);
        panel.add(speedPanel, BorderLayout.WEST);
        panel.add(mainControlsPanel, BorderLayout.EAST);
        return panel;
    }

    private void setBottomControlsEnabledState(boolean simNotRunningOrStopped) {
        runGenerationButton.setEnabled(simNotRunningOrStopped);
        pauseButton.setEnabled(!simNotRunningOrStopped);
        if (simNotRunningOrStopped) {
            pauseButton.setText("Пауза");
        }
        stopButton.setEnabled(!simNotRunningOrStopped);
    }

    private void handleDialogOpening(){
        if (simulationEngine.isRunning() && !simulationEngine.isPaused()) {
            simulationEngine.pauseSimulation();
            pauseButton.setText("Продолжить");
        }
        currentPlacementMode = PlacementMode.NONE;
        simulationPanel.setPlacementMode(false, null);
    }

    private void handleDialogClosing(boolean settingsWereSaved){
        if (settingsWereSaved) {
            if (simulationEngine.isRunning()) {
                simulationEngine.stopSimulation();
            }
            setBottomControlsEnabledState(true);
            runGenerationButton.setText("Запустить генерацию!");
            simulationEngine.updateParameters(simulationParameters);
        } else {
            if (simulationEngine.isRunning() && pauseButton.getText().equals("Продолжить")) {
                simulationEngine.resumeSimulation();
                pauseButton.setText("Пауза");
            }
        }
    }

    private void openRoadSettingsDialog() {
        handleDialogOpening();
        RoadSettingsDialog roadDlg = new RoadSettingsDialog(this, simulationParameters);
        boolean settingsWereSaved = roadDlg.showDialog();
        handleDialogClosing(settingsWereSaved);
    }

    private void openModelingSettingsDialog() {
        handleDialogOpening();
        ModelingSettingsDialog modelingDlg = new ModelingSettingsDialog(this, simulationParameters);
        boolean settingsWereSaved = modelingDlg.showDialog();
        handleDialogClosing(settingsWereSaved);
    }

    private boolean canPlaceObject(){
        if (simulationEngine.getRoad() == null) {
            JOptionPane.showMessageDialog(this, "Сначала настройте параметры дороги.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (simulationEngine.isRunning()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Симуляция активна. Для добавления объектов ее необходимо остановить.\nОстановить симуляцию?",
                    "Симуляция активна", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                stopButton.doClick();
            } else {
                currentPlacementMode = PlacementMode.NONE;
                simulationPanel.setPlacementMode(false, null);
                return false;
            }
        }
        return true;
    }

    private void showAboutDialog() {
        AboutDialog aboutDialog = new AboutDialog(this);
        aboutDialog.setVisible(true);
    }

    private void addSimulationPanelMouseListener() {
        simulationPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentPlacementMode == PlacementMode.NONE || simulationEngine.getRoad() == null) {
                    return;
                }
                Road currentRoad = simulationEngine.getRoad();
                RoadType currentRoadType = simulationParameters.getRoadType();
                if (currentRoadType == null) currentRoadType = RoadType.CITY_ROAD;

                double roadLengthMeters = currentRoad.getLength();
                if (roadLengthMeters <= 0) return;

                int roadCenterYOnPanel = simulationPanel.getHeight() / 2;
                int roadRenderStartY = roadCenterYOnPanel - ROAD_RENDER_HEIGHT_FOR_CLICK / 2;
                int roadRenderEndY = roadCenterYOnPanel + ROAD_RENDER_HEIGHT_FOR_CLICK / 2;

                if (e.getY() < roadRenderStartY || e.getY() > roadRenderEndY) {
                    return;
                }

                double clickXRatio = (double) e.getX() / simulationPanel.getWidth();
                double positionOnRoad = clickXRatio * roadLengthMeters;

                if (positionOnRoad < 0 || positionOnRoad > roadLengthMeters) {
                    currentPlacementMode = PlacementMode.NONE;
                    simulationPanel.setPlacementMode(false, null);
                    return;
                }
                if (!isPlacementPositionValid(positionOnRoad)) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Невозможно разместить объект слишком близко к другому объекту или к краю дороги.\n" +
                                    "Мин. отступ от края: " + MIN_EDGE_SPACING_METERS + "м, мин. отступ между объектами: " + MIN_OBJECT_SPACING_METERS + "м.",
                            "Ошибка размещения", JOptionPane.WARNING_MESSAGE);
                    currentPlacementMode = PlacementMode.NONE;
                    simulationPanel.setPlacementMode(false, null);
                    return;
                }

                int determinedTargetDirection = 0;
                if (currentRoad.getNumberOfDirections() == 2) {
                    if (currentRoadType == RoadType.TUNNEL) {
                        determinedTargetDirection = -1;
                    } else {
                        if (e.getY() < roadCenterYOnPanel) {
                            determinedTargetDirection = 0;
                        } else {
                            determinedTargetDirection = 1;
                        }
                    }
                }

                if (currentPlacementMode == PlacementMode.ADD_TRAFFIC_LIGHT) {
                    openTrafficLightSettingsDialog(positionOnRoad, determinedTargetDirection);
                } else if (currentPlacementMode == PlacementMode.ADD_ROAD_SIGN) {
                    openRoadSignSettingsDialog(positionOnRoad, determinedTargetDirection);
                }
                currentPlacementMode = PlacementMode.NONE;
                simulationPanel.setPlacementMode(false, null);
            }
        });
    }

    private boolean isPlacementPositionValid(double newPosition) {
        if (simulationEngine.getRoad() == null) return false;
        if (newPosition < MIN_EDGE_SPACING_METERS ||
                newPosition > (simulationEngine.getRoad().getLength() - MIN_EDGE_SPACING_METERS)) {
            return false;
        }
        List<TrafficLight> lights = simulationEngine.getRoad().getTrafficLights();
        if (lights != null) {
            for (TrafficLight light : lights) {
                if (Math.abs(light.getPosition() - newPosition) < MIN_OBJECT_SPACING_METERS) return false;
            }
        }
        List<RoadSign> signs = simulationEngine.getRoad().getRoadSigns();
        if (signs != null) {
            for (RoadSign sign : signs) {
                if (Math.abs(sign.getPosition() - newPosition) < MIN_OBJECT_SPACING_METERS) return false;
            }
        }
        return true;
    }

    private void openTrafficLightSettingsDialog(double position, int determinedTargetDirection) {
        if (simulationEngine.getRoad() == null || simulationEngine.getRoad().getTrafficLights().size() >= 2) {
            JOptionPane.showMessageDialog(this, "Достигнут лимит светофоров (макс. 2) или дорога не инициализирована.", "Лимит/Ошибка", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (simulationParameters.getRoadType() == RoadType.TUNNEL) {
            JOptionPane.showMessageDialog(this, "Для тоннеля светофоры управляются автоматически и не добавляются вручную.", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JSpinner redDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        JSpinner greenDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(5,5,5,5);

        panel.add(new JLabel("Длительность красного (с):"), gbc);
        gbc.gridx = 1; panel.add(redDurationSpinner, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Длительность зеленого (с):"), gbc);
        gbc.gridx = 1; panel.add(greenDurationSpinner, gbc);

        // Метка о направлении УДАЛЕНА из диалога

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Настроить светофор на поз. " + String.format("%.1f", position) + "м",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            double red = ((Number) redDurationSpinner.getValue()).doubleValue();
            double green = ((Number) greenDurationSpinner.getValue()).doubleValue();
            TrafficLightState initialState = TrafficLightState.GREEN;

            simulationEngine.getRoad().addTrafficLight(
                    new TrafficLight(position, red, green, initialState, determinedTargetDirection)
            );
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }

    private void openRoadSignSettingsDialog(double position, int determinedTargetDirection) {
        RoadType currentRoadType = simulationParameters.getRoadType();
        if (currentRoadType == null) currentRoadType = RoadType.CITY_ROAD;
        List<Integer> availableSpeeds = new ArrayList<>();
        switch (currentRoadType) {
            case CITY_ROAD: availableSpeeds.add(40); availableSpeeds.add(50); availableSpeeds.add(60); availableSpeeds.add(80); break;
            case HIGHWAY: availableSpeeds.add(70); availableSpeeds.add(90); availableSpeeds.add(110); availableSpeeds.add(130); break;
            case TUNNEL: availableSpeeds.add(40); availableSpeeds.add(50); availableSpeeds.add(60); availableSpeeds.add(70); break;
            default: availableSpeeds.add(50); break;
        }
        JComboBox<Integer> speedComboBox = new JComboBox<>(availableSpeeds.toArray(new Integer[0]));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(5,5,5,5);
        panel.add(new JLabel("Ограничение скорости:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; panel.add(speedComboBox, gbc);
        gbc.gridy++; gbc.gridx = 1; panel.add(new JLabel(" км/ч"), gbc);

        // Метка о направлении УДАЛЕНА из диалога

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Добавить знак на поз. " + String.format("%.1f", position) + "м",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Integer selectedSpeedLimit = (Integer) speedComboBox.getSelectedItem();
            if (selectedSpeedLimit == null) return;
            simulationEngine.getRoad().addRoadSign(
                    new RoadSign(position, RoadSignType.SPEED_LIMIT, determinedTargetDirection, selectedSpeedLimit.doubleValue())
            );
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }
}
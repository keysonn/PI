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
    // private JButton removeObjectButton; // Убираем, удаление теперь по прямому клику
    private JButton helpButton;

    private JToggleButton speed1xButton, speed2xButton, speed3xButton;
    private ButtonGroup speedGroup;
    private JButton runGenerationButton;
    private JButton pauseButton;
    private JButton stopButton;

    private enum UserInteractionMode { NONE, ADD_TRAFFIC_LIGHT, ADD_ROAD_SIGN } // Убрали REMOVE_OBJECT
    private UserInteractionMode currentUserMode = UserInteractionMode.NONE;
    private String currentPlacementHint = "";

    private static final double MIN_OBJECT_SPACING_METERS = 15.0;
    private static final double MIN_EDGE_SPACING_METERS = 7.5;
    private static final int ROAD_CLICK_AREA_HEIGHT_OFFSET = 10;
    private static final int OBJECT_CLICK_RADIUS = 15;


    public MainFrame() {
        setTitle("Система моделирования движения транспорта v1.5"); // Обновим версию
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
        helpButton = new JButton("Справка");

        Dimension iconButtonSize = new Dimension(32, 32); // Чуть побольше для иконок

        ImageIcon lightIcon = loadImageIcon("icons/traffic_light_icon.png", "Добавить светофор");
        addTrafficLightIconButton = new JButton(lightIcon);
        configureIconButton(addTrafficLightIconButton, "Добавить светофор (макс. 2 на не-тоннель)", iconButtonSize, lightIcon == null ? "Св+" : null);

        ImageIcon signIcon = loadImageIcon("icons/road_sign_icon.png", "Добавить знак");
        addRoadSignIconButton = new JButton(signIcon);
        configureIconButton(addRoadSignIconButton, "Добавить дорожный знак", iconButtonSize, signIcon == null ? "Зн+" : null);

        // Кнопка для удаления объектов убрана

        roadSettingsButton.addActionListener(e -> openRoadSettingsDialog());
        modelingSettingsButton.addActionListener(e -> openModelingSettingsDialog());

        addTrafficLightIconButton.addActionListener(e -> {
            if (!canInteractWithRoadObjects()) return;
            if (simulationEngine.getRoad() != null && simulationEngine.getRoad().getType() != RoadType.TUNNEL &&
                    simulationEngine.getRoad().getTrafficLights().stream().filter(tl -> !tl.isExternallyControlled()).count() >= 2) {
                JOptionPane.showMessageDialog(this, "Достигнут лимит пользовательских светофоров (макс. 2).", "Лимит", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            currentUserMode = UserInteractionMode.ADD_TRAFFIC_LIGHT;
            currentPlacementHint = "Добавление Светофора";
            simulationPanel.setPlacementMode(true, currentPlacementHint);
        });

        addRoadSignIconButton.addActionListener(e -> {
            if (!canInteractWithRoadObjects()) return;
            currentUserMode = UserInteractionMode.ADD_ROAD_SIGN;
            currentPlacementHint = "Добавление Знака огр. скорости";
            simulationPanel.setPlacementMode(true, currentPlacementHint);
        });

        helpButton.addActionListener(e -> showAboutDialog());

        toolBar.add(roadSettingsButton);
        toolBar.add(modelingSettingsButton);
        toolBar.addSeparator(new Dimension(10,0));
        toolBar.add(addTrafficLightIconButton);
        toolBar.add(addRoadSignIconButton);
        // toolBar.addSeparator(); // Сепаратор перед удалением не нужен
        // toolBar.add(removeObjectButton); // Убрали кнопку удаления
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(helpButton);
        return toolBar;
    }

    private void configureIconButton(JButton button, String tooltip, Dimension size, String fallbackText) {
        button.setToolTipText(tooltip);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setFocusPainted(false);
        button.setMargin(new Insets(2,2,2,2)); // Уменьшаем отступы внутри кнопки
        if (button.getIcon() == null && fallbackText != null) {
            button.setText(fallbackText);
        } else if (button.getIcon() != null) {
            button.setText(null);
        }
    }

    private ImageIcon loadImageIcon(String path, String description) {
        // ... (без изменений) ...
        URL imgURL = getClass().getClassLoader().getResource(path);
        if (imgURL != null) {
            ImageIcon icon = new ImageIcon(imgURL, description);
            if (icon.getIconWidth() > 28 || icon.getIconHeight() > 28) { // Макс. размер иконки для кнопки 32x32 с отступами
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImg, description);
            }
            return icon;
        } else {
            System.err.println("Не удалось загрузить иконку: " + path + " (для " + description + ")");
            return null;
        }
    }

    private JPanel createBottomControlPanel() {
        // ... (код без изменений, кнопка Стоп будет доработана в SimulationEngine) ...
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
        speedPanel.add(speed1xButton); speedPanel.add(speed2xButton); speedPanel.add(speed3xButton);

        JPanel mainControlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        runGenerationButton = new JButton("Запустить генерацию!");
        pauseButton = new JButton("Пауза");
        stopButton = new JButton("Стоп");
        pauseButton.setEnabled(false); stopButton.setEnabled(false);

        runGenerationButton.addActionListener(e -> {
            currentUserMode = UserInteractionMode.NONE;
            simulationPanel.setPlacementMode(false, null);
            if (simulationEngine.isRunning() && !simulationEngine.isPaused()) {
            } else {
                simulationEngine.startSimulation();
            }
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
            pauseButton.setText("Пауза");
            currentUserMode = UserInteractionMode.NONE;
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
        // ... (без изменений) ...
        runGenerationButton.setEnabled(simNotRunningOrStopped);
        pauseButton.setEnabled(!simNotRunningOrStopped);
        if (simNotRunningOrStopped) {
            pauseButton.setText("Пауза");
        }
        stopButton.setEnabled(!simNotRunningOrStopped);
    }

    private void handleDialogOpening(){
        // ... (без изменений) ...
        if (simulationEngine.isRunning() && !simulationEngine.isPaused()) {
            simulationEngine.pauseSimulation();
            pauseButton.setText("Продолжить");
        }
        currentUserMode = UserInteractionMode.NONE;
        simulationPanel.setPlacementMode(false, null);
    }

    private void handleDialogClosing(boolean settingsWereSaved){
        // ... (без изменений) ...
        if (settingsWereSaved) {
            simulationEngine.updateParameters(simulationParameters);
            setBottomControlsEnabledState(true);
            runGenerationButton.setText("Запустить генерацию!");
            pauseButton.setText("Пауза");
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        } else {
            if (simulationEngine.isRunning() && pauseButton.getText().equals("Продолжить")) {
                simulationEngine.resumeSimulation();
                pauseButton.setText("Пауза");
            }
        }
    }
    private void openRoadSettingsDialog() { /* ... (без изменений) ... */ handleDialogOpening(); RoadSettingsDialog roadDlg = new RoadSettingsDialog(this, simulationParameters); boolean settingsWereSaved = roadDlg.showDialog(); handleDialogClosing(settingsWereSaved); }
    private void openModelingSettingsDialog() {  /* ... (без изменений) ... */ handleDialogOpening(); ModelingSettingsDialog modelingDlg = new ModelingSettingsDialog(this, simulationParameters); boolean settingsWereSaved = modelingDlg.showDialog(); handleDialogClosing(settingsWereSaved); }

    private boolean canInteractWithRoadObjects(){
        // ... (без изменений) ...
        if (simulationEngine.getRoad() == null) {
            JOptionPane.showMessageDialog(this, "Сначала настройте параметры дороги.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (simulationEngine.isRunning()) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Симуляция активна. Для изменения объектов на дороге ее необходимо остановить.\nОстановить симуляцию?",
                    "Симуляция активна", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                stopButton.doClick();
            } else {
                currentUserMode = UserInteractionMode.NONE;
                simulationPanel.setPlacementMode(false, null);
                return false;
            }
        }
        return true;
    }

    private void showAboutDialog() { /* ... (без изменений) ... */ AboutDialog aboutDialog = new AboutDialog(this); aboutDialog.setVisible(true); }

    private void addSimulationPanelMouseListener() {
        simulationPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (simulationEngine.getRoad() == null) {
                    return;
                }
                Road currentRoad = simulationEngine.getRoad();
                RoadType currentRoadType = simulationParameters.getRoadType();
                if (currentRoadType == null && currentRoad != null) currentRoadType = currentRoad.getType();
                if (currentRoadType == null) currentRoadType = RoadType.CITY_ROAD;

                double roadModelLength = currentRoad.getLength();
                if (roadModelLength <= 0) return;

                int totalLanes = currentRoad.getNumberOfLanes();
                if (totalLanes == 0) totalLanes = 1;
                int roadRenderHeight = SimulationPanel.TARGET_LANE_VISUAL_HEIGHT * totalLanes;
                int roadCenterY = simulationPanel.getHeight() / 2;
                int roadVisualTopY = roadCenterY - roadRenderHeight / 2;

                if (currentUserMode == UserInteractionMode.ADD_TRAFFIC_LIGHT || currentUserMode == UserInteractionMode.ADD_ROAD_SIGN) {
                    int clickAreaTopY = roadVisualTopY - ROAD_CLICK_AREA_HEIGHT_OFFSET;
                    int clickAreaBottomY = roadVisualTopY + roadRenderHeight + ROAD_CLICK_AREA_HEIGHT_OFFSET;
                    if (e.getY() < clickAreaTopY || e.getY() > clickAreaBottomY) {
                        return;
                    }

                    double clickXRatio = (double) e.getX() / simulationPanel.getWidth();
                    double positionOnRoad = clickXRatio * roadModelLength;

                    if (positionOnRoad < 0 || positionOnRoad > roadModelLength) {
                        currentUserMode = UserInteractionMode.NONE; simulationPanel.setPlacementMode(false, null); return;
                    }
                    if (!isPlacementPositionValid(positionOnRoad)) {
                        JOptionPane.showMessageDialog(MainFrame.this, "Невозможно разместить объект слишком близко к другому объекту или к краю дороги.\n" +
                                "Мин. отступ от края: " + MIN_EDGE_SPACING_METERS + "м, мин. отступ между объектами: " + MIN_OBJECT_SPACING_METERS + "м.", "Ошибка размещения", JOptionPane.WARNING_MESSAGE);
                        currentUserMode = UserInteractionMode.NONE; simulationPanel.setPlacementMode(false, null); return;
                    }

                    int determinedModelDirection = 0;
                    if (currentRoad.getNumberOfDirections() == 2 && currentRoadType != RoadType.TUNNEL) {
                        if (e.getY() < roadCenterY) determinedModelDirection = 1;
                        else determinedModelDirection = 0;
                    } else if (currentRoad.getNumberOfDirections() == 1) {
                        determinedModelDirection = 0;
                    } else if (currentRoadType == RoadType.TUNNEL) {
                        determinedModelDirection = -1;
                    }

                    if (currentUserMode == UserInteractionMode.ADD_TRAFFIC_LIGHT) {
                        openTrafficLightSettingsDialog(positionOnRoad, determinedModelDirection);
                    } else if (currentUserMode == UserInteractionMode.ADD_ROAD_SIGN) {
                        openRoadSignSettingsDialog(positionOnRoad, determinedModelDirection);
                    }
                    currentUserMode = UserInteractionMode.NONE;
                    simulationPanel.setPlacementMode(false, null);
                } else if (currentUserMode == UserInteractionMode.NONE) { // Нет активного режима добавления -> проверяем удаление
                    if (!canInteractWithRoadObjects()) return; // Остановит симуляцию, если нужно

                    TrafficLight lightToRemove = findTrafficLightAtScreenPosition(e.getX(), e.getY(), roadVisualTopY, roadRenderHeight);
                    if (lightToRemove != null) {
                        if (currentRoadType == RoadType.TUNNEL && lightToRemove.isExternallyControlled()) {
                            JOptionPane.showMessageDialog(MainFrame.this, "Системные светофоры тоннеля не могут быть удалены.", "Удаление запрещено", JOptionPane.WARNING_MESSAGE);
                        } else {
                            int confirm = JOptionPane.showConfirmDialog(MainFrame.this, "Удалить этот светофор?", "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (confirm == JOptionPane.YES_OPTION) {
                                currentRoad.removeTrafficLight(lightToRemove);
                                simulationPanel.updateSimulationState(currentRoad, simulationEngine.getSimulationTime());
                            }
                        }
                        return;
                    }

                    RoadSign signToRemove = findRoadSignAtScreenPosition(e.getX(), e.getY(), roadVisualTopY, roadRenderHeight);
                    if (signToRemove != null) {
                        int confirm = JOptionPane.showConfirmDialog(MainFrame.this, "Удалить этот дорожный знак?", "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if (confirm == JOptionPane.YES_OPTION) {
                            currentRoad.removeRoadSign(signToRemove);
                            simulationPanel.updateSimulationState(currentRoad, simulationEngine.getSimulationTime());
                        }
                        return;
                    }
                }
            }
        });
    }

    private TrafficLight findTrafficLightAtScreenPosition(int screenX, int screenY, int roadTopY, int roadHeight) {
        // ... (код без изменений, использует SimulationPanel.КОНСТАНТЫ) ...
        if (simulationEngine.getRoad() == null) return null;
        Road road = simulationEngine.getRoad();
        int panelWidth = simulationPanel.getWidth();

        for (TrafficLight light : road.getTrafficLights()) {
            int lightModelDir = light.getTargetDirection();
            boolean isAboveRoad = (road.getType() == RoadType.TUNNEL) ? (lightModelDir == 1) : (lightModelDir == 1 || lightModelDir == -1);

            int lightCenterX = (int) ((light.getPosition() / road.getLength()) * panelWidth);

            int lightSignalVisualTopY = isAboveRoad ?
                    roadTopY - SimulationPanel.OBJECT_SIDE_OFFSET - SimulationPanel.TRAFFIC_LIGHT_POLE_HEIGHT - SimulationPanel.TRAFFIC_LIGHT_VISUAL_WIDTH :
                    roadTopY + roadHeight + SimulationPanel.OBJECT_SIDE_OFFSET + SimulationPanel.TRAFFIC_LIGHT_POLE_HEIGHT;

            Rectangle clickRect = new Rectangle(
                    lightCenterX - SimulationPanel.TRAFFIC_LIGHT_VISUAL_WIDTH / 2 - OBJECT_CLICK_RADIUS / 2,
                    lightSignalVisualTopY - OBJECT_CLICK_RADIUS / 2,
                    SimulationPanel.TRAFFIC_LIGHT_VISUAL_WIDTH + OBJECT_CLICK_RADIUS,
                    SimulationPanel.TRAFFIC_LIGHT_VISUAL_WIDTH + OBJECT_CLICK_RADIUS
            );

            if (clickRect.contains(screenX, screenY)) {
                return light;
            }
        }
        return null;
    }

    private RoadSign findRoadSignAtScreenPosition(int screenX, int screenY, int roadTopY, int roadHeight) {
        // ... (код без изменений, использует SimulationPanel.КОНСТАНТЫ) ...
        if (simulationEngine.getRoad() == null) return null;
        Road road = simulationEngine.getRoad();
        int panelWidth = simulationPanel.getWidth();

        for (RoadSign sign : road.getRoadSigns()) {
            int signModelDir = sign.getTargetDirection();
            boolean primaryIsAbove = (road.getType() == RoadType.TUNNEL) || (signModelDir == 1) || (signModelDir == -1 && road.getNumberOfDirections() == 2);

            int signCenterX = (int) ((sign.getPosition() / road.getLength()) * panelWidth);

            int signVisualTopY = primaryIsAbove ?
                    roadTopY - SimulationPanel.OBJECT_SIDE_OFFSET - SimulationPanel.ROAD_SIGN_POLE_HEIGHT - SimulationPanel.ROAD_SIGN_SIZE :
                    roadTopY + roadHeight + SimulationPanel.OBJECT_SIDE_OFFSET + SimulationPanel.ROAD_SIGN_POLE_HEIGHT;

            Rectangle clickRectPrimary = new Rectangle(
                    signCenterX - SimulationPanel.ROAD_SIGN_SIZE / 2 - OBJECT_CLICK_RADIUS / 2,
                    signVisualTopY - OBJECT_CLICK_RADIUS / 2,
                    SimulationPanel.ROAD_SIGN_SIZE + OBJECT_CLICK_RADIUS,
                    SimulationPanel.ROAD_SIGN_SIZE + OBJECT_CLICK_RADIUS
            );
            if (clickRectPrimary.contains(screenX, screenY)) return sign;

            if (signModelDir == -1 && road.getNumberOfDirections() == 2 && primaryIsAbove) {
                int bottomSignVisualTopY = roadTopY + roadHeight + SimulationPanel.OBJECT_SIDE_OFFSET + SimulationPanel.ROAD_SIGN_POLE_HEIGHT;
                Rectangle clickRectBottom = new Rectangle(
                        signCenterX - SimulationPanel.ROAD_SIGN_SIZE / 2 - OBJECT_CLICK_RADIUS / 2,
                        bottomSignVisualTopY - OBJECT_CLICK_RADIUS / 2,
                        SimulationPanel.ROAD_SIGN_SIZE + OBJECT_CLICK_RADIUS,
                        SimulationPanel.ROAD_SIGN_SIZE + OBJECT_CLICK_RADIUS
                );
                if (clickRectBottom.contains(screenX, screenY)) return sign;
            }
        }
        return null;
    }

    private boolean isPlacementPositionValid(double newPosition) {
        // ... (без изменений) ...
        if (simulationEngine.getRoad() == null) return false;
        if (newPosition < MIN_EDGE_SPACING_METERS || newPosition > (simulationEngine.getRoad().getLength() - MIN_EDGE_SPACING_METERS)) {
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

    private void openTrafficLightSettingsDialog(double position, int modelTargetDirection) {
        // ... (без изменений) ...
        if (simulationEngine.getRoad() == null ) {
            JOptionPane.showMessageDialog(this, "Дорога не инициализирована.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (simulationParameters.getRoadType() == RoadType.TUNNEL) {
            JOptionPane.showMessageDialog(this, "Для тоннеля светофоры управляются автоматически и не добавляются вручную.", "Информация", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        long userTrafficLightsCount = simulationEngine.getRoad().getTrafficLights().stream()
                .filter(tl -> !tl.isExternallyControlled()).count();
        if (userTrafficLightsCount >= 2) {
            JOptionPane.showMessageDialog(this, "Достигнут лимит пользовательских светофоров (макс. 2).", "Лимит", JOptionPane.INFORMATION_MESSAGE);
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

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Настроить светофор на поз. " + String.format("%.1f", position) + "м (напр. " + modelTargetDirection + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            double red = ((Number) redDurationSpinner.getValue()).doubleValue();
            double green = ((Number) greenDurationSpinner.getValue()).doubleValue();
            TrafficLightState initialState = TrafficLightState.GREEN;
            simulationEngine.getRoad().addTrafficLight(
                    new TrafficLight(position, red, green, initialState, modelTargetDirection)
            );
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }

    private void openRoadSignSettingsDialog(double position, int modelTargetDirection) {
        // ... (без изменений) ...
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

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Добавить знак на поз. " + String.format("%.1f", position) + "м (напр. " + modelTargetDirection + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            Integer selectedSpeedLimit = (Integer) speedComboBox.getSelectedItem();
            if (selectedSpeedLimit == null) return;
            simulationEngine.getRoad().addRoadSign(
                    new RoadSign(position, RoadSignType.SPEED_LIMIT, modelTargetDirection, selectedSpeedLimit.doubleValue())
            );
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }
}
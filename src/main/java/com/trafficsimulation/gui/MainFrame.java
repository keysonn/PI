package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.List;

import com.trafficsimulation.model.RoadSign;
import com.trafficsimulation.model.RoadSignType;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;
import com.trafficsimulation.simulation.SimulationEngine;
import com.trafficsimulation.simulation.SimulationParameters;
// Старый SettingsDialog больше не импортируется

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

    public MainFrame() {
        setTitle("Система моделирования движения транспорта v0.9"); // Обновляем версию
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1000, 700));

        simulationParameters = new SimulationParameters(); // С новыми флагами isRandomSpeedFlow, isRandomTimeFlow
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
        System.out.println("Конструктор MainFrame выполнен, компоненты новой структуры добавлены.");
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
        addTrafficLightIconButton.setToolTipText("Добавить светофор");
        if (lightIcon == null) addTrafficLightIconButton.setText("Св");

        addRoadSignIconButton = new JButton(signIcon);
        addRoadSignIconButton.setToolTipText("Добавить дорожный знак");
        if (signIcon == null) addRoadSignIconButton.setText("Зн");

        helpButton = new JButton("Справка");

        roadSettingsButton.addActionListener(e -> openSpecificRoadSettingsDialog());
        modelingSettingsButton.addActionListener(e -> openSpecificModelingSettingsDialog());

        addTrafficLightIconButton.addActionListener(e -> {
            if (!canPlaceObject()) return;
            if (simulationEngine.getRoad().getTrafficLights().size() >= 2) {
                JOptionPane.showMessageDialog(this, "Достигнут лимит светофоров (макс. 2).", "Лимит", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            currentPlacementMode = PlacementMode.ADD_TRAFFIC_LIGHT;
            simulationPanel.setPlacementMode(true, "Светофор");
            System.out.println("Режим: Добавление светофора. Кликните на дорогу.");
        });

        addRoadSignIconButton.addActionListener(e -> {
            if (!canPlaceObject()) return;
            currentPlacementMode = PlacementMode.ADD_ROAD_SIGN;
            simulationPanel.setPlacementMode(true, "Знак");
            System.out.println("Режим: Добавление знака. Кликните на дорогу.");
        });
        helpButton.addActionListener(e -> showHelpDialog());

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
            System.err.println("Не удалось загрузить иконку: " + path + ". Убедитесь, что файл находится в src/main/resources/" + path);
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
            System.out.println("Скорость симуляции установлена на: " + factor + "x");
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
            System.out.println("Нажата кнопка Запустить генерацию!");
            currentPlacementMode = PlacementMode.NONE;
            simulationPanel.setPlacementMode(false, null);
            simulationEngine.startSimulation();
            setBottomControlsEnabledState(false);
            runGenerationButton.setText("Генерация...");
        });

        pauseButton.addActionListener(e -> {
            if (pauseButton.getText().equals("Пауза")) {
                simulationEngine.pauseSimulation();
                pauseButton.setText("Продолжить");
            } else {
                simulationEngine.resumeSimulation();
                pauseButton.setText("Пауза");
            }
        });

        stopButton.addActionListener(e -> {
            System.out.println("Нажата кнопка Стоп");
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
        boolean wasRunningAndNotPaused = !runGenerationButton.isEnabled() && pauseButton.getText().equals("Пауза");
        if (wasRunningAndNotPaused) {
            simulationEngine.pauseSimulation();
            pauseButton.setText("Продолжить");
        }
        currentPlacementMode = PlacementMode.NONE;
        simulationPanel.setPlacementMode(false, null);
    }

    private void handleDialogClosing(boolean settingsWereSaved){
        boolean wasPausedByDialog = !runGenerationButton.isEnabled() && pauseButton.getText().equals("Продолжить");

        if (settingsWereSaved) {
            System.out.println("Настройки сохранены. Применяем...");
            simulationEngine.stopSimulation();
            setBottomControlsEnabledState(true);
            runGenerationButton.setText("Запустить генерацию!");
            simulationEngine.initializeSimulation();
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), 0);
            System.out.println("Симуляция переинициализирована с новыми параметрами.");
        } else {
            System.out.println("Настройки не были сохранены.");
            if (wasPausedByDialog) {
                simulationEngine.resumeSimulation();
                pauseButton.setText("Пауза");
            }
        }
    }

    private void openSpecificRoadSettingsDialog() {
        System.out.println("Открытие окна настроек параметров автодороги");
        handleDialogOpening();
        RoadSettingsDialog roadDlg = new RoadSettingsDialog(MainFrame.this, simulationParameters);
        boolean settingsWereSaved = roadDlg.showDialog();
        handleDialogClosing(settingsWereSaved);
    }

    private void openSpecificModelingSettingsDialog() {
        System.out.println("Открытие окна настроек параметров моделирования");
        handleDialogOpening();

        ModelingSettingsDialog modelingDlg = new ModelingSettingsDialog(MainFrame.this, simulationParameters);
        boolean settingsWereSaved = modelingDlg.showDialog();

        handleDialogClosing(settingsWereSaved);
    }

    private boolean canPlaceObject(){
        if (simulationEngine.getRoad() == null) {
            JOptionPane.showMessageDialog(this, "Сначала инициализируйте дорогу (через Настройки или Запуск).", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        boolean isSimulating = !runGenerationButton.isEnabled();
        boolean isPausedWaitingResume = pauseButton.isEnabled() && pauseButton.getText().equals("Продолжить");
        if (isSimulating || isPausedWaitingResume) {
            if (stopButton.isEnabled()) {
                stopButton.doClick();
            } else {
                simulationEngine.stopSimulation();
                setBottomControlsEnabledState(true);
                runGenerationButton.setText("Запустить генерацию!");
            }
        }
        return true;
    }

    private void showHelpDialog() {
        JTextArea helpTextArea = new JTextArea(15, 50);
        helpTextArea.setText("Система моделирования движения транспорта v0.9\n\n" +
                "Разработчики:\n" +
                " - Пожидаев Н.А.\n" +
                " - Адаев Н.О.\n\n" +
                "Руководитель: Зеленко Л.С.\n" +
                "Самарский университет, 2025\n\n" +
                "Инструкция:\n" +
                "1. Задайте параметры дороги и моделирования через соответствующие кнопки на верхней панели.\n" +
                "2. Для добавления светофора или знака нажмите соответствующую кнопку-иконку,\n" +
                "   затем кликните на желаемую позицию на дороге в основном окне.\n" +
                "3. Используйте кнопки внизу для запуска, паузы, остановки симуляции и выбора скорости.\n" +
                "4. Максимальное количество светофоров на дороге - 2.\n" +
                "5. Объекты не могут быть размещены слишком близко друг к другу или к краям дороги.\n"
        );
        helpTextArea.setWrapStyleWord(true);
        helpTextArea.setLineWrap(true);
        helpTextArea.setEditable(false);
        helpTextArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(helpTextArea);
        JOptionPane.showMessageDialog(this, scrollPane, "Справка", JOptionPane.INFORMATION_MESSAGE);
    }

    private void addSimulationPanelMouseListener() {
        simulationPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentPlacementMode == PlacementMode.NONE || simulationEngine.getRoad() == null) {
                    return;
                }
                double roadLengthMeters = simulationEngine.getRoad().getLength();
                if (roadLengthMeters <= 0) return;

                double clickXRatio = (double) e.getX() / simulationPanel.getWidth();
                double positionOnRoad = clickXRatio * roadLengthMeters;

                if (positionOnRoad < 0 || positionOnRoad > roadLengthMeters) {
                    currentPlacementMode = PlacementMode.NONE;
                    simulationPanel.setPlacementMode(false, null);
                    return;
                }
                if (!isPlacementPositionValid(positionOnRoad)) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Невозможно разместить объект слишком близко к другому или к краю дороги.",
                            "Ошибка размещения", JOptionPane.WARNING_MESSAGE);
                    currentPlacementMode = PlacementMode.NONE;
                    simulationPanel.setPlacementMode(false, null);
                    return;
                }
                if (currentPlacementMode == PlacementMode.ADD_TRAFFIC_LIGHT) {
                    openTrafficLightSettingsDialog(positionOnRoad);
                } else if (currentPlacementMode == PlacementMode.ADD_ROAD_SIGN) {
                    openRoadSignSettingsDialog(positionOnRoad);
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
            System.out.println("Попытка разместить объект слишком близко к краю дороги: " + String.format("%.1f", newPosition));
            return false;
        }
        List<TrafficLight> lights = simulationEngine.getRoad().getTrafficLights();
        if (lights != null) {
            for (TrafficLight light : lights) {
                if (Math.abs(light.getPosition() - newPosition) < MIN_OBJECT_SPACING_METERS) {
                    System.out.println("Слишком близко к светофору " + light.getId() + " на " + String.format("%.1f", light.getPosition()));
                    return false;
                }
            }
        }
        List<RoadSign> signs = simulationEngine.getRoad().getRoadSigns();
        if (signs != null) {
            for (RoadSign sign : signs) {
                if (Math.abs(sign.getPosition() - newPosition) < MIN_OBJECT_SPACING_METERS) {
                    System.out.println("Слишком близко к знаку " + sign.getId() + " на " + String.format("%.1f", sign.getPosition()));
                    return false;
                }
            }
        }
        return true;
    }

    private void openTrafficLightSettingsDialog(double position) {
        if (simulationEngine.getRoad().getTrafficLights().size() >= 2) {
            JOptionPane.showMessageDialog(this, "Достигнут лимит светофоров (макс. 2).", "Лимит", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JSpinner redDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        JSpinner greenDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));

        JComboBox<String> directionComboBox = null;
        String[] directionChoices = {"Направление 1 (->)", "Направление 2 (<-)"};
        if (simulationEngine.getRoad() != null && simulationEngine.getRoad().getNumberOfDirections() == 2) {
            directionComboBox = new JComboBox<>(directionChoices);
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(2,2,2,2);

        panel.add(new JLabel("Длительность красного (с):"), gbc);
        gbc.gridx = 1; panel.add(redDurationSpinner, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Длительность зеленого (с):"), gbc);
        gbc.gridx = 1; panel.add(greenDurationSpinner, gbc);

        if (directionComboBox != null) {
            gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Действует на:"), gbc);
            gbc.gridx = 1; panel.add(directionComboBox, gbc);
        }

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Настроить светофор на позиции " + String.format("%.1f", position) + "м",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            double red = ((Number) redDurationSpinner.getValue()).doubleValue();
            double green = ((Number) greenDurationSpinner.getValue()).doubleValue();
            TrafficLightState initialState = TrafficLightState.GREEN;
            int targetDirection = 0;
            if (simulationEngine.getRoad() != null && simulationEngine.getRoad().getNumberOfDirections() == 1) {
                targetDirection = 0;
            } else if (directionComboBox != null && directionComboBox.getSelectedIndex() == 1) {
                targetDirection = 1;
            }
            simulationEngine.getRoad().addTrafficLight(
                    new TrafficLight(position, red, green, initialState, targetDirection)
            );
            System.out.println("Добавлен светофор: pos=" + position + ", R=" + red + ", G=" + green + ", Init=" + initialState + ", Dir=" + targetDirection);
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }

    private void openRoadSignSettingsDialog(double position) {
        RoadSignType[] signTypes = RoadSignType.values();
        JComboBox<RoadSignType> typeComboBox = new JComboBox<>(signTypes);

        JComboBox<String> directionComboBox = null;
        String[] directionChoices = {"Направление 1 (->)", "Направление 2 (<-)"};
        if (simulationEngine.getRoad() != null && simulationEngine.getRoad().getNumberOfDirections() == 2) {
            directionComboBox = new JComboBox<>(directionChoices);
        }

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(2,2,2,2);

        panel.add(new JLabel("Тип знака:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; panel.add(typeComboBox, gbc);
        gbc.fill = GridBagConstraints.NONE;

        if (directionComboBox != null) {
            gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Действует на:"), gbc);
            gbc.gridx = 1; panel.add(directionComboBox, gbc);
        }

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Добавить дорожный знак на позиции " + String.format("%.1f", position) + "м:",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            RoadSignType selectedType = (RoadSignType) typeComboBox.getSelectedItem();
            int targetDirection = 0;
            if (simulationEngine.getRoad() != null && simulationEngine.getRoad().getNumberOfDirections() == 1) {
                targetDirection = 0;
            } else if (directionComboBox != null && directionComboBox.getSelectedIndex() == 1) {
                targetDirection = 1;
            }
            simulationEngine.getRoad().addRoadSign(
                    new RoadSign(position, selectedType, targetDirection)
            );
            System.out.println("Добавлен знак: pos=" + position + ", тип=" + selectedType + ", Dir=" + targetDirection);
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }
}
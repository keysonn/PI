package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.trafficsimulation.model.RoadSignType;
import com.trafficsimulation.model.TrafficLightState;
import com.trafficsimulation.simulation.SimulationEngine;
import com.trafficsimulation.simulation.SimulationParameters;

public class MainFrame extends JFrame {

    private SimulationPanel simulationPanel;
    private SimulationEngine simulationEngine;
    private SimulationParameters simulationParameters;

    private JButton startButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton settingsButton;
    private JButton addTrafficLightButton;
    private JButton addRoadSignButton;

    private enum PlacementMode { NONE, ADD_TRAFFIC_LIGHT, ADD_ROAD_SIGN }
    private PlacementMode currentPlacementMode = PlacementMode.NONE;

    public MainFrame() {
        setTitle("Система моделирования движения транспорта v0.4"); // Обновим версию
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        simulationParameters = new SimulationParameters();
        simulationPanel = new SimulationPanel();
        simulationEngine = new SimulationEngine(simulationParameters, simulationPanel);

        // Обновляем панель начальным состоянием дороги СРАЗУ ПОСЛЕ СОЗДАНИЯ ДВИЖКА
        simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());

        JPanel controlPanel = createControlPanel();

        add(simulationPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        addSimulationPanelMouseListener();

        pack();
        setLocationRelativeTo(null);
        System.out.println("Конструктор MainFrame выполнен, компоненты добавлены.");
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        panel.setBorder(BorderFactory.createEtchedBorder());

        startButton = new JButton("Старт");
        pauseButton = new JButton("Пауза");
        stopButton = new JButton("Стоп");
        settingsButton = new JButton("Настройки");
        addTrafficLightButton = new JButton("Добавить светофор");
        addRoadSignButton = new JButton("Добавить знак");

        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        startButton.addActionListener(e -> {
            System.out.println("Нажата кнопка Старт");
            currentPlacementMode = PlacementMode.NONE;
            simulationPanel.setPlacementMode(false, null);
            simulationEngine.startSimulation();
            setControlsEnabledState(false);
        });

        pauseButton.addActionListener(new ActionListener() {
            private boolean isPaused = false; // Это состояние должно управляться извне или синхронизироваться с движком
            @Override
            public void actionPerformed(ActionEvent e) {
                // Лучше проверять состояние движка, а не хранить локальное isPaused
                if (pauseButton.getText().equals("Пауза")) { // Если сейчас "Пауза", значит надо поставить на паузу
                    simulationEngine.pauseSimulation();
                    pauseButton.setText("Продолжить");
                } else { // Если сейчас "Продолжить", значит надо снять с паузы
                    simulationEngine.resumeSimulation();
                    pauseButton.setText("Пауза");
                }
            }
        });

        stopButton.addActionListener(e -> {
            System.out.println("Нажата кнопка Стоп");
            simulationEngine.stopSimulation();
            setControlsEnabledState(true);
            currentPlacementMode = PlacementMode.NONE;
            simulationPanel.setPlacementMode(false, null);
            // После остановки показываем текущее состояние дороги (с машинами, если они есть)
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        });

        settingsButton.addActionListener(e -> {
            System.out.println("Нажата кнопка Настройки");
            boolean wasRunningAndNotPaused = !startButton.isEnabled() && pauseButton.getText().equals("Пауза");
            boolean wasPaused = !startButton.isEnabled() && pauseButton.getText().equals("Продолжить");

            if (wasRunningAndNotPaused) {
                simulationEngine.pauseSimulation();
                pauseButton.setText("Продолжить");
            }

            currentPlacementMode = PlacementMode.NONE;
            simulationPanel.setPlacementMode(false, null);

            SettingsDialog settingsDialog = new SettingsDialog(MainFrame.this, simulationParameters);
            boolean settingsWereSaved = settingsDialog.showDialog();

            if (settingsWereSaved) {
                System.out.println("Настройки сохранены. Применяем...");
                simulationEngine.stopSimulation(); // Обязательно остановить перед полной переинициализацией
                setControlsEnabledState(true);     // Сначала кнопки включить (т.к. симуляция остановлена)
                simulationEngine.initializeSimulation();
                simulationPanel.updateSimulationState(simulationEngine.getRoad(), 0); // Обновить панель с новой дорогой
                System.out.println("Симуляция переинициализирована с новыми параметрами.");
            } else {
                System.out.println("Настройки не были сохранены.");
                if (wasRunningAndNotPaused) { // Если была запущена и не на паузе до открытия настроек
                    simulationEngine.resumeSimulation();
                    pauseButton.setText("Пауза");
                } else if (wasPaused) {
                    // Если была на паузе, оставляем на паузе, но кнопка должна быть "Продолжить"
                    pauseButton.setText("Продолжить");
                }
            }
        });

        addTrafficLightButton.addActionListener(e -> {
            if (simulationEngine.getRoad() == null) {
                JOptionPane.showMessageDialog(this, "Сначала инициализируйте дорогу.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (simulationEngine.getRoad().getTrafficLights().size() >= 2) {
                JOptionPane.showMessageDialog(this, "Достигнут лимит светофоров (макс. 2).", "Лимит", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (!startButton.isEnabled() && stopButton.isEnabled()) { // Если симуляция запущена или на паузе
                stopButton.doClick(); // Останавливаем для безопасного добавления
            }
            currentPlacementMode = PlacementMode.ADD_TRAFFIC_LIGHT;
            simulationPanel.setPlacementMode(true, "Светофор");
            System.out.println("Режим: Добавление светофора. Кликните на дорогу.");
        });

        addRoadSignButton.addActionListener(e -> {
            if (simulationEngine.getRoad() == null) {
                JOptionPane.showMessageDialog(this, "Сначала инициализируйте дорогу.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!startButton.isEnabled() && stopButton.isEnabled()) {
                stopButton.doClick();
            }
            currentPlacementMode = PlacementMode.ADD_ROAD_SIGN;
            simulationPanel.setPlacementMode(true, "Знак");
            System.out.println("Режим: Добавление знака. Кликните на дорогу.");
        });

        panel.add(startButton);
        panel.add(pauseButton);
        panel.add(stopButton);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(addTrafficLightButton);
        panel.add(addRoadSignButton);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(settingsButton);

        return panel;
    }

    private void setControlsEnabledState(boolean simNotRunningOrStopped) {
        startButton.setEnabled(simNotRunningOrStopped);
        settingsButton.setEnabled(simNotRunningOrStopped);
        addTrafficLightButton.setEnabled(simNotRunningOrStopped);
        addRoadSignButton.setEnabled(simNotRunningOrStopped);

        pauseButton.setEnabled(!simNotRunningOrStopped);
        if (simNotRunningOrStopped) {
            pauseButton.setText("Пауза");
        }
        stopButton.setEnabled(!simNotRunningOrStopped);
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
                    System.out.println("Позиция вне дороги.");
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

    private void openTrafficLightSettingsDialog(double position) {
        if (simulationEngine.getRoad().getTrafficLights().size() >= 2) {
            JOptionPane.showMessageDialog(this, "Достигнут лимит светофоров (макс. 2).", "Лимит", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JSpinner redDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        JSpinner greenDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        panel.add(new JLabel("Длительность красного (с):")); panel.add(redDurationSpinner);
        panel.add(new JLabel("Длительность зеленого (с):")); panel.add(greenDurationSpinner);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Настроить светофор на позиции " + String.format("%.1f", position) + "м",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            double red = ((Number) redDurationSpinner.getValue()).doubleValue();
            double green = ((Number) greenDurationSpinner.getValue()).doubleValue();
            simulationEngine.getRoad().addTrafficLight(
                    new com.trafficsimulation.model.TrafficLight(position, red, green, TrafficLightState.RED)
            );
            System.out.println("Добавлен светофор: pos=" + position + ", R=" + red + ", G=" + green);
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }

    private void openRoadSignSettingsDialog(double position) {
        RoadSignType[] signTypes = RoadSignType.values();
        RoadSignType selectedType = (RoadSignType) JOptionPane.showInputDialog(
                this, "Выберите тип знака для позиции " + String.format("%.1f", position) + "м:",
                "Добавить дорожный знак", JOptionPane.PLAIN_MESSAGE, null, signTypes, signTypes[0]
        );
        if (selectedType != null) {
            simulationEngine.getRoad().addRoadSign(
                    new com.trafficsimulation.model.RoadSign(position, selectedType)
            );
            System.out.println("Добавлен знак: pos=" + position + ", тип=" + selectedType);
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }
}
package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List; // Для проверки существующих объектов

import com.trafficsimulation.model.RoadSign; // Для проверки существующих объектов
import com.trafficsimulation.model.RoadSignType;
import com.trafficsimulation.model.TrafficLight; // Для проверки существующих объектов
import com.trafficsimulation.model.TrafficLightState;
import com.trafficsimulation.simulation.SimulationEngine;
import com.trafficsimulation.simulation.SimulationParameters;
// import com.trafficsimulation.gui.VisualizationMode; // УДАЛЯЕМ ЭТОТ ИМПОРТ

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
    // private JComboBox<VisualizationMode> vizModeComboBox; // УДАЛЯЕМ ПОЛЕ

    private enum PlacementMode { NONE, ADD_TRAFFIC_LIGHT, ADD_ROAD_SIGN }
    private PlacementMode currentPlacementMode = PlacementMode.NONE;

    // Минимальное расстояние между объектами на дороге (в метрах)
    private static final double MIN_OBJECT_SPACING_METERS = 15.0;


    public MainFrame() {
        setTitle("Система моделирования движения транспорта v0.6"); // Обновим версию
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        simulationParameters = new SimulationParameters();
        simulationPanel = new SimulationPanel();
        // simulationPanel.setVisualizationMode(VisualizationMode.STANDARD); // УДАЛЯЕМ
        simulationEngine = new SimulationEngine(simulationParameters, simulationPanel);

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
        // vizModeComboBox = new JComboBox<>(VisualizationMode.values()); // УДАЛЯЕМ
        // vizModeComboBox.setToolTipText("Выберите режим визуализации"); // УДАЛЯЕМ
        /* // УДАЛЯЕМ ActionListener для vizModeComboBox
        vizModeComboBox.addActionListener(e -> {
            VisualizationMode selectedMode = (VisualizationMode) vizModeComboBox.getSelectedItem();
            if (selectedMode != null && simulationPanel != null) {
                simulationPanel.setVisualizationMode(selectedMode);
            }
        });
        */

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
            @Override
            public void actionPerformed(ActionEvent e) {
                if (pauseButton.getText().equals("Пауза")) {
                    simulationEngine.pauseSimulation();
                    pauseButton.setText("Продолжить");
                } else {
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
                simulationEngine.stopSimulation();
                setControlsEnabledState(true);
                simulationEngine.initializeSimulation();
                simulationPanel.updateSimulationState(simulationEngine.getRoad(), 0);
                System.out.println("Симуляция переинициализирована с новыми параметрами.");
            } else {
                System.out.println("Настройки не были сохранены.");
                if (wasRunningAndNotPaused) {
                    simulationEngine.resumeSimulation();
                    pauseButton.setText("Пауза");
                } else if (wasPaused) {
                    pauseButton.setText("Продолжить");
                }
            }
        });

        addTrafficLightButton.addActionListener(e -> {
            if (simulationEngine.getRoad() == null) {
                JOptionPane.showMessageDialog(this, "Сначала инициализируйте дорогу.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (simulationEngine.getRoad().getTrafficLights().size() >= 2) { // ТЗ: максимальное количество светофоров – 2
                JOptionPane.showMessageDialog(this, "Достигнут лимит светофоров (макс. 2).", "Лимит", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (!startButton.isEnabled() && stopButton.isEnabled()) {
                stopButton.doClick();
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
            // Здесь можно добавить лимит на общее количество знаков, если он есть в ТЗ.
            // Пока что без лимита на знаки (кроме контроля наложения).
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
        panel.add(Box.createHorizontalStrut(10));
        panel.add(addTrafficLightButton);
        panel.add(addRoadSignButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(settingsButton);
        // panel.add(Box.createHorizontalStrut(10)); // УДАЛЯЕМ
        // panel.add(new JLabel("Вид:")); // УДАЛЯЕМ
        // panel.add(vizModeComboBox); // УДАЛЯЕМ

        return panel;
    }

    private void setControlsEnabledState(boolean simNotRunningOrStopped) {
        startButton.setEnabled(simNotRunningOrStopped);
        settingsButton.setEnabled(simNotRunningOrStopped);
        addTrafficLightButton.setEnabled(simNotRunningOrStopped);
        addRoadSignButton.setEnabled(simNotRunningOrStopped);
        // vizModeComboBox.setEnabled(true); // УДАЛЯЕМ (комбобокса больше нет)

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

                // --- КОНТРОЛЬ РАЗМЕЩЕНИЯ: Проверка наложения ---
                if (!isPlacementPositionValid(positionOnRoad)) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Невозможно разместить объект слишком близко к другому.",
                            "Ошибка размещения", JOptionPane.WARNING_MESSAGE);
                    currentPlacementMode = PlacementMode.NONE;
                    simulationPanel.setPlacementMode(false, null);
                    return;
                }
                // --- КОНЕЦ КОНТРОЛЯ РАЗМЕЩЕНИЯ ---


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

    /**
     * Проверяет, можно ли разместить новый объект на указанной позиции
     * (не слишком ли близко к существующим светофорам или знакам).
     */
    private boolean isPlacementPositionValid(double newPosition) {
        if (simulationEngine.getRoad() == null) return false; // На всякий случай

        List<TrafficLight> lights = simulationEngine.getRoad().getTrafficLights();
        if (lights != null) {
            for (TrafficLight light : lights) {
                if (Math.abs(light.getPosition() - newPosition) < MIN_OBJECT_SPACING_METERS) {
                    return false; // Слишком близко к существующему светофору
                }
            }
        }
        List<RoadSign> signs = simulationEngine.getRoad().getRoadSigns();
        if (signs != null) {
            for (RoadSign sign : signs) {
                if (Math.abs(sign.getPosition() - newPosition) < MIN_OBJECT_SPACING_METERS) {
                    return false; // Слишком близко к существующему знаку
                }
            }
        }
        return true; // Место свободно
    }


    private void openTrafficLightSettingsDialog(double position) {
        // Проверка лимита светофоров уже есть в ActionListener кнопки
        JSpinner redDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        JSpinner greenDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1));
        // Можно добавить выбор начального состояния
        JComboBox<TrafficLightState> initialStateComboBox = new JComboBox<>(new TrafficLightState[]{TrafficLightState.RED, TrafficLightState.GREEN});

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(2,2,2,2);
        panel.add(new JLabel("Длительность красного (с):"), gbc);
        gbc.gridx = 1; panel.add(redDurationSpinner, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Длительность зеленого (с):"), gbc);
        gbc.gridx = 1; panel.add(greenDurationSpinner, gbc);
        gbc.gridy++; gbc.gridx = 0; panel.add(new JLabel("Начальное состояние:"), gbc);
        gbc.gridx = 1; panel.add(initialStateComboBox, gbc);


        int result = JOptionPane.showConfirmDialog(this, panel,
                "Настроить светофор на позиции " + String.format("%.1f", position) + "м",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            double red = ((Number) redDurationSpinner.getValue()).doubleValue();
            double green = ((Number) greenDurationSpinner.getValue()).doubleValue();
            TrafficLightState initialState = (TrafficLightState) initialStateComboBox.getSelectedItem();
            simulationEngine.getRoad().addTrafficLight(
                    new com.trafficsimulation.model.TrafficLight(position, red, green, initialState)
            );
            System.out.println("Добавлен светофор: pos=" + position + ", R=" + red + ", G=" + green + ", Init=" + initialState);
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
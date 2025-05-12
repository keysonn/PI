package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import com.trafficsimulation.model.RoadSign;
import com.trafficsimulation.model.RoadSignType;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState; // Используется в openTrafficLightSettingsDialog
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

    private static final double MIN_OBJECT_SPACING_METERS = 15.0;
    private static final double MIN_EDGE_SPACING_METERS = 7.5;


    public MainFrame() {
        setTitle("Система моделирования движения транспорта v0.7");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        simulationParameters = new SimulationParameters();
        simulationPanel = new SimulationPanel();
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
            if (simulationEngine.getRoad().getTrafficLights().size() >= 2) {
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
            System.out.println("Попытка разместить объект слишком близко к краю дороги: " + newPosition);
            return false;
        }
        List<TrafficLight> lights = simulationEngine.getRoad().getTrafficLights();
        if (lights != null) {
            for (TrafficLight light : lights) {
                if (Math.abs(light.getPosition() - newPosition) < MIN_OBJECT_SPACING_METERS) {
                    System.out.println("Слишком близко к светофору " + light.getId() + " на " + light.getPosition());
                    return false;
                }
            }
        }
        List<RoadSign> signs = simulationEngine.getRoad().getRoadSigns();
        if (signs != null) {
            for (RoadSign sign : signs) {
                if (Math.abs(sign.getPosition() - newPosition) < MIN_OBJECT_SPACING_METERS) {
                    System.out.println("Слишком близко к знаку " + sign.getId() + " на " + sign.getPosition());
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
        if (simulationEngine.getRoad().getNumberOfDirections() == 2) {
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
            int targetDirection = 0;
            if (simulationEngine.getRoad().getNumberOfDirections() == 1) {
                targetDirection = 0;
            } else if (directionComboBox != null && directionComboBox.getSelectedIndex() == 1) {
                targetDirection = 1;
            }
            simulationEngine.getRoad().addTrafficLight(
                    new TrafficLight(position, red, green, targetDirection) // Используем новый конструктор
            );
            System.out.println("Добавлен светофор: pos=" + position + ", R=" + red + ", G=" + green + ", Dir=" + targetDirection + " (нач. сост. GREEN)");
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }

    private void openRoadSignSettingsDialog(double position) {
        RoadSignType[] signTypes = RoadSignType.values();
        JComboBox<RoadSignType> typeComboBox = new JComboBox<>(signTypes);

        JComboBox<String> directionComboBox = null;
        String[] directionChoices = {"Направление 1 (->)", "Направление 2 (<-)"};
        if (simulationEngine.getRoad().getNumberOfDirections() == 2) {
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
            if (simulationEngine.getRoad().getNumberOfDirections() == 1) {
                targetDirection = 0;
            } else if (directionComboBox != null && directionComboBox.getSelectedIndex() == 1) {
                targetDirection = 1;
            }
            simulationEngine.getRoad().addRoadSign(
                    new RoadSign(position, selectedType, targetDirection) // Используем новый конструктор
            );
            System.out.println("Добавлен знак: pos=" + position + ", тип=" + selectedType + ", Dir=" + targetDirection);
            simulationPanel.updateSimulationState(simulationEngine.getRoad(), simulationEngine.getSimulationTime());
        }
    }
}
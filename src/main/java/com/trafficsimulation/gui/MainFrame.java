package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.trafficsimulation.simulation.SimulationEngine;
import com.trafficsimulation.simulation.SimulationParameters;

/**
 * Главное окно приложения симуляции.
 * Содержит панель отрисовки и кнопки управления.
 */
public class MainFrame extends JFrame {

    private SimulationPanel simulationPanel;
    private SimulationEngine simulationEngine;
    private SimulationParameters simulationParameters;

    private JButton startButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton settingsButton;

    public MainFrame() {
        setTitle("Система моделирования движения транспорта v0.2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        simulationParameters = new SimulationParameters();
        simulationPanel = new SimulationPanel();
        simulationEngine = new SimulationEngine(simulationParameters, simulationPanel);

        JPanel controlPanel = createControlPanel();

        add(simulationPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        System.out.println("Конструктор MainFrame выполнен, компоненты добавлены.");
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBorder(BorderFactory.createEtchedBorder());

        startButton = new JButton("Старт");
        pauseButton = new JButton("Пауза");
        stopButton = new JButton("Стоп");
        settingsButton = new JButton("Настройки");

        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Нажата кнопка Старт");
                simulationEngine.startSimulation();
                startButton.setEnabled(false);
                settingsButton.setEnabled(false);
                pauseButton.setEnabled(true);
                pauseButton.setText("Пауза");
                stopButton.setEnabled(true);
            }
        });

        pauseButton.addActionListener(new ActionListener() {
            private boolean isPaused = false;
            @Override
            public void actionPerformed(ActionEvent e) {
                isPaused = !isPaused;
                if (isPaused) {
                    System.out.println("Нажата кнопка Пауза");
                    simulationEngine.pauseSimulation();
                    pauseButton.setText("Продолжить");
                } else {
                    System.out.println("Нажата кнопка Продолжить");
                    simulationEngine.resumeSimulation();
                    pauseButton.setText("Пауза");
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Нажата кнопка Стоп");
                simulationEngine.stopSimulation();
                startButton.setEnabled(true);
                settingsButton.setEnabled(true);
                pauseButton.setEnabled(false);
                pauseButton.setText("Пауза");
                stopButton.setEnabled(false);
                simulationPanel.updateSimulationState(null, 0);
            }
        });

        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Нажата кнопка Настройки");
                SettingsDialog settingsDialog = new SettingsDialog(MainFrame.this, simulationParameters);
                boolean settingsWereSaved = settingsDialog.showDialog();

                if (settingsWereSaved) {
                    System.out.println("Настройки сохранены. Применяем...");
                    simulationEngine.stopSimulation();
                    startButton.setEnabled(true);
                    settingsButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    pauseButton.setText("Пауза");
                    stopButton.setEnabled(false);

                    // simulationEngine.updateParameters(simulationParameters); // Не обязательно, т.к. работаем с той же ссылкой
                    simulationEngine.initializeSimulation();
                    simulationPanel.updateSimulationState(simulationEngine.getRoad(), 0);
                    System.out.println("Симуляция переинициализирована с новыми параметрами.");
                } else {
                    System.out.println("Настройки не были сохранены (нажата Отмена).");
                }
            }
        });

        panel.add(startButton);
        panel.add(pauseButton);
        panel.add(stopButton);
        panel.add(Box.createHorizontalStrut(50));
        panel.add(settingsButton);

        return panel;
    }
}
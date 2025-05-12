package com.trafficsimulation.gui;

// Импорты для компонентов Swing
import javax.swing.*;
import java.awt.*; // Для BorderLayout, Dimension, Color
import java.awt.event.ActionEvent; // Для обработки нажатий кнопок
import java.awt.event.ActionListener; // Для обработки нажатий кнопок

// Импорты наших классов симуляции
import com.trafficsimulation.simulation.SimulationEngine;
import com.trafficsimulation.simulation.SimulationParameters;

/**
 * Главное окно приложения симуляции.
 * Содержит панель отрисовки и кнопки управления.
 */
public class MainFrame extends JFrame {

    private SimulationPanel simulationPanel; // Панель, где рисуется симуляция
    private SimulationEngine simulationEngine; // Движок симуляции
    private SimulationParameters simulationParameters; // Параметры симуляции

    // Кнопки управления
    private JButton startButton;
    private JButton pauseButton;
    private JButton stopButton;
    private JButton settingsButton; // Кнопка для вызова настроек (пока не работает)

    /**
     * Конструктор главного окна.
     */
    public MainFrame() {
        // 1. Настройка окна
        setTitle("Система моделирования движения транспорта v0.2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Устанавливаем менеджер компоновки BorderLayout для окна.
        // Он позволяет размещать компоненты по сторонам (NORTH, SOUTH, EAST, WEST, CENTER).
        setLayout(new BorderLayout());

        // 2. Создание основных компонентов симуляции
        simulationParameters = new SimulationParameters(); // Создаем объект с параметрами
        simulationPanel = new SimulationPanel();           // Создаем панель для рисования
        // !!! Важно: передаем ссылку на панель в движок, чтобы он мог ее обновлять
        simulationEngine = new SimulationEngine(simulationParameters, simulationPanel);

        // 3. Создание панели управления с кнопками
        JPanel controlPanel = createControlPanel();

        // 4. Добавление компонентов в окно
        // Панель симуляции размещаем в центре
        add(simulationPanel, BorderLayout.CENTER);
        // Панель управления размещаем внизу
        add(controlPanel, BorderLayout.SOUTH);

        // 5. Финальная настройка окна
        pack(); // Автоматически подбирает размер окна под размер компонентов
        setLocationRelativeTo(null); // Центрируем окно после pack()
        // setMinimumSize(new Dimension(600, 400)); // Можно задать минимальный размер

        System.out.println("Конструктор MainFrame выполнен, компоненты добавлены.");
    }

    /**
     * Создает и настраивает панель управления с кнопками.
     * @return Готовая панель управления (JPanel).
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(); // Создаем новую панель
        // Используем FlowLayout: кнопки будут располагаться в ряд
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Центрирование, отступы 10px
        panel.setBorder(BorderFactory.createEtchedBorder()); // Добавляем рамку для красоты

        // Создаем кнопки
        startButton = new JButton("Старт");
        pauseButton = new JButton("Пауза");
        stopButton = new JButton("Стоп");
        settingsButton = new JButton("Настройки");

        // Изначально кнопки Пауза и Стоп неактивны, т.к. симуляция не запущена
        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        // --- Добавление обработчиков нажатий на кнопки ---
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Нажата кнопка Старт");
                simulationEngine.startSimulation();
                // Обновляем состояние кнопок
                startButton.setEnabled(false);
                settingsButton.setEnabled(false); // Запрещаем менять настройки во время симуляции
                pauseButton.setEnabled(true);
                pauseButton.setText("Пауза"); // Убедимся, что текст "Пауза"
                stopButton.setEnabled(true);
            }
        });

        pauseButton.addActionListener(new ActionListener() {
            private boolean isPaused = false; // Внутреннее состояние кнопки Пауза/Продолжить
            @Override
            public void actionPerformed(ActionEvent e) {
                isPaused = !isPaused; // Инвертируем состояние паузы
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
                // Возвращаем кнопки в исходное состояние
                startButton.setEnabled(true);
                settingsButton.setEnabled(true);
                pauseButton.setEnabled(false);
                pauseButton.setText("Пауза");
                stopButton.setEnabled(false);
                // Сбросим состояние панели отрисовки (можно добавить метод reset в SimulationPanel)
                simulationPanel.updateSimulationState(null, 0); // Показываем "Симуляция не запущена"
            }
        });

        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Нажата кнопка Настройки");
                // TODO: Открыть диалоговое окно настроек
                JOptionPane.showMessageDialog(MainFrame.this,
                        "Окно настроек еще не реализовано.",
                        "Настройки",
                        JOptionPane.INFORMATION_MESSAGE);
                // После закрытия окна настроек, возможно, нужно будет обновить параметры:
                // simulationEngine.updateParameters(simulationParameters);
                // и переинициализировать симуляцию, если параметры дороги изменились
                // simulationEngine.stopSimulation(); // Остановить старую
                // simulationEngine.initializeSimulation(); // Инициализировать с новыми параметрами
                // simulationPanel.updateSimulationState(simulationEngine.getRoad(), 0); // Обновить панель
            }
        });

        // Добавляем кнопки на панель управления
        panel.add(startButton);
        panel.add(pauseButton);
        panel.add(stopButton);
        panel.add(Box.createHorizontalStrut(50)); // Добавляем промежуток перед настройками
        panel.add(settingsButton);

        return panel; // Возвращаем созданную панель
    }

    // Можно добавить главный метод сюда для быстрого теста только этого окна,
    // но основной запуск через TrafficSimulationApp
    /*
    public static void main(String[] args) {
         SwingUtilities.invokeLater(() -> {
             MainFrame frame = new MainFrame();
             frame.setVisible(true);
         });
    }
    */
}
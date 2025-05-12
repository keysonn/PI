package com.trafficsimulation.main; // Замените com.trafficsimulation на ваше имя пакета, если оно другое

// Импортируем нужные классы Swing для работы с интерфейсом
import javax.swing.SwingUtilities;
// Импортируем наш будущий класс главного окна
import com.trafficsimulation.gui.MainFrame;

/**
 * Главный класс для запуска приложения симуляции дорожного движения.
 */
public class TrafficSimulationApp {

    /**
     * Точка входа в программу.
     * @param args Аргументы командной строки (не используются).
     */
    public static void main(String[] args) {
        // Весь код, связанный с созданием и обновлением интерфейса Swing,
        // должен выполняться в специальном потоке - Event Dispatch Thread (EDT).
        // SwingUtilities.invokeLater гарантирует, что код внутри run() будет выполнен в EDT.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Этот код выполнится в потоке EDT
                System.out.println("Запуск приложения в потоке EDT...");
                // Создаем экземпляр главного окна (класс MainFrame будет создан следующим)
                MainFrame mainFrame = new MainFrame();
                // Делаем окно видимым для пользователя
                mainFrame.setVisible(true);
                System.out.println("Главное окно создано и сделано видимым.");
            }
        });
        System.out.println("Метод main завершил свою работу (но приложение продолжит работать из-за потока EDT).");
    }
}
package com.trafficsimulation.main;

import javax.swing.SwingUtilities;
import com.trafficsimulation.gui.MainFrame;

public class TrafficSimulationApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("Запуск приложения в потоке EDT...");
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                System.out.println("Главное окно создано и сделано видимым.");
            }
        });
        System.out.println("Метод main завершил свою работу (но приложение продолжит работать из-за потока EDT).");
    }
}
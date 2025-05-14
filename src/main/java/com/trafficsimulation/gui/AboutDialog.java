package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class AboutDialog extends JDialog {

    public AboutDialog(Frame owner) {
        super(owner, "Сведения о разработчиках", true); // Модальное окно
        initComponents();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        // Основная панель с информацией
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        infoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);


        addLabelToPanel("Самарский университет", infoPanel, Font.PLAIN, 14, true);
        addLabelToPanel("Институт информатики и кибернетики", infoPanel, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(10));
        addLabelToPanel("Курсовой проект по дисциплине \"Программная инженерия\"", infoPanel, Font.BOLD, 16, true);
        addLabelToPanel("Тема проекта: \"Система моделирования движения транспорта", infoPanel, Font.PLAIN, 14, true);
        addLabelToPanel("на автодороге (в тоннеле /на автостраде)\"", infoPanel, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(20));
        addLabelToPanel("Разработчики - обучающиеся группы 6303-020302D:", infoPanel, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(5));
        addLabelToPanel("Н.А. Пожидаев", infoPanel, Font.PLAIN, 14, true);
        addLabelToPanel("Н.О. Адаев", infoPanel, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(10));
        addLabelToPanel("Самара 2025", infoPanel, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(20));


        // Кнопка "О системе"
        JButton aboutSystemButton = new JButton("О системе");
        aboutSystemButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Центрируем кнопку
        aboutSystemButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openHelpFile();
            }
        });

        infoPanel.add(aboutSystemButton);

        // Кнопка "OK" для закрытия диалога (опционально, но удобно)
        JButton okButton = new JButton("OK");
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.addActionListener(e -> dispose());
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(okButton);


        getContentPane().add(infoPanel, BorderLayout.CENTER);
    }

    private void addLabelToPanel(String text, JPanel panel, int fontStyle, int fontSize, boolean centered) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", fontStyle, fontSize));
        if (centered) {
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        panel.add(label);
    }

    private void openHelpFile() {
        System.out.println("Attempting to open help file...");
        try {
            // Пытаемся получить URL ресурса из classpath
            // Путь должен быть ОТНОСИТЕЛЬНО ПАПКИ RESOURCES
            // Если help.html лежит прямо в src/main/resources, то путь будет "help.html"
            URL helpFileUrl = getClass().getClassLoader().getResource("help.html");

            if (helpFileUrl == null) {
                System.err.println("Help file 'help.html' NOT FOUND in classpath resources!");
                // Попробуем найти в текущей директории (менее надежно, но для отладки)
                File localFile = new File("help.html");
                if (localFile.exists() && !localFile.isDirectory()) {
                    System.out.println("Found 'help.html' in current working directory. Attempting to open...");
                    helpFileUrl = localFile.toURI().toURL();
                } else {
                    // Попробуем найти в корне проекта, если resources не настроены правильно
                    localFile = new File("src/main/resources/help.html");
                    if (localFile.exists() && !localFile.isDirectory()) {
                        System.out.println("Found 'src/main/resources/help.html'. Attempting to open...");
                        helpFileUrl = localFile.toURI().toURL();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Файл справки 'help.html' не найден.\nУбедитесь, что он находится в папке src/main/resources.",
                                "Ошибка открытия справки", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            System.out.println("Help file URL: " + helpFileUrl);
            File htmlFile = new File(helpFileUrl.toURI());
            System.out.println("Attempting to open file: " + htmlFile.getAbsolutePath());

            if (!htmlFile.exists()) {
                System.err.println("File object created, but file does not exist at path: " + htmlFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this,
                        "Файл справки не существует по пути: " + htmlFile.getAbsolutePath(),
                        "Ошибка открытия справки", JOptionPane.ERROR_MESSAGE);
                return;
            }


            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    System.out.println("Desktop.Action.BROWSE is supported. Browsing URI: " + htmlFile.toURI());
                    desktop.browse(htmlFile.toURI());
                    System.out.println("Browse command issued.");
                } else {
                    System.err.println("Desktop.Action.BROWSE is NOT supported.");
                    JOptionPane.showMessageDialog(this,
                            "Не удалось открыть файл справки: действие 'BROWSE' не поддерживается на вашей системе.",
                            "Ошибка открытия справки", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                System.err.println("Desktop is NOT supported.");
                JOptionPane.showMessageDialog(this,
                        "Не удалось открыть файл справки: класс Desktop не поддерживается на вашей системе.",
                        "Ошибка открытия справки", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | URISyntaxException | NullPointerException ex) { // Добавил NullPointerException на всякий случай
            System.err.println("Error opening help file:");
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Ошибка при открытии файла справки: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                    "Критическая ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
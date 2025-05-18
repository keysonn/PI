package com.trafficsimulation.gui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class AboutDialog extends JDialog {

    public AboutDialog(Frame owner) {
        super(owner, "Сведения о разработчиках", true);
        initComponents();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        infoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        addLabelToPanel("Самарский университет", infoPanel, Font.SANS_SERIF, Font.PLAIN, 14, true);
        addLabelToPanel("Институт информатики и кибернетики", infoPanel, Font.SANS_SERIF, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(10));
        addLabelToPanel("Курсовой проект по дисциплине \"Программная инженерия\"", infoPanel, Font.SANS_SERIF, Font.BOLD, 16, true);
        addLabelToPanel("Тема проекта: \"Система моделирования движения транспорта", infoPanel, Font.SANS_SERIF, Font.PLAIN, 14, true);
        addLabelToPanel("на автодороге (в тоннеле /на автостраде)\"", infoPanel, Font.SANS_SERIF, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(20));
        addLabelToPanel("Разработчики - обучающиеся группы 6303-020302D:", infoPanel, Font.SANS_SERIF, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(5));
        addLabelToPanel("Н.А. Пожидаев", infoPanel, Font.SANS_SERIF, Font.PLAIN, 14, true);
        addLabelToPanel("Н.О. Адаев", infoPanel, Font.SANS_SERIF, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(10));
        addLabelToPanel("Самара 2025", infoPanel, Font.SANS_SERIF, Font.PLAIN, 14, true);
        infoPanel.add(Box.createVerticalStrut(20));

        JButton aboutSystemButton = new JButton("О системе (help.html)");
        aboutSystemButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        aboutSystemButton.addActionListener(e -> openHelpFile());
        infoPanel.add(aboutSystemButton);
        infoPanel.add(Box.createVerticalStrut(10));

        JButton okButton = new JButton("OK");
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.addActionListener(e -> dispose());
        infoPanel.add(okButton);

        getContentPane().add(infoPanel, BorderLayout.CENTER);
    }

    private void addLabelToPanel(String text, JPanel panel, String fontName, int fontStyle, int fontSize, boolean centered) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(fontName, fontStyle, fontSize));
        if (centered) {
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        panel.add(label);
    }

    private void openHelpFile() {
        try {
            URL helpFileUrl = getClass().getClassLoader().getResource("help.html");
            if (helpFileUrl == null) {
                File localFile = new File("help.html"); // Пытаемся найти рядом с JAR
                if (localFile.exists() && !localFile.isDirectory()) {
                    helpFileUrl = localFile.toURI().toURL();
                } else {
                    // Пытаемся найти в src/main/resources (для запуска из IDE)
                    localFile = new File("src/main/resources/help.html");
                    if (localFile.exists() && !localFile.isDirectory()) {
                        helpFileUrl = localFile.toURI().toURL();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Файл справки 'help.html' не найден.\nПроверьте наличие файла в папке с программой или в папке src/main/resources.",
                                "Ошибка открытия справки", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            File htmlFile = new File(helpFileUrl.toURI());
            if (!htmlFile.exists()) {
                JOptionPane.showMessageDialog(this,
                        "Файл справки не существует по пути: " + htmlFile.getAbsolutePath(),
                        "Ошибка открытия справки", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(htmlFile.toURI());
            } else {
                JOptionPane.showMessageDialog(this,
                        "Не удалось открыть файл справки: действие 'BROWSE' не поддерживается.",
                        "Ошибка открытия справки", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException | URISyntaxException | NullPointerException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка при открытии файла справки: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(),
                    "Критическая ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}
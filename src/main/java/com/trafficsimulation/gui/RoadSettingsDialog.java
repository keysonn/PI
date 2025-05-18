package com.trafficsimulation.gui;

import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.simulation.SimulationParameters;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class RoadSettingsDialog extends JDialog {

    private SimulationParameters params; // Ссылка на объект параметров
    private boolean settingsSaved = false;

    private JComboBox<RoadType> roadTypeComboBox;
    private JRadioButton одностороннееRadioButton, двухстороннееRadioButton;
    private ButtonGroup directionGroup;
    private JSlider lanesPerDirectionSlider;

    // Элементы для тоннеля (кроме времени очистки)
    private JLabel redLightDurationLabel, greenLightDurationLabel; // tunnelClearanceTimeLabel УДАЛЕН
    private JSpinner redLightDurationSpinner, greenLightDurationSpinner; // tunnelClearanceTimeSpinner УДАЛЕН
    private JPanel tunnelSpecificPanel; // Панель для специфичных настроек тоннеля

    public RoadSettingsDialog(Frame owner, SimulationParameters currentParams) {
        super(owner, "Настройка параметров автодороги", true);
        this.params = currentParams;

        initComponents();
        layoutComponents();
        addListeners();

        loadParameters(); // Загружаем текущие параметры в диалог
        updateFieldVisibility(currentParams.getRoadType()); // Обновляем видимость полей

        pack();
        setMinimumSize(getPreferredSize()); // Чтобы нельзя было сделать меньше
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        roadTypeComboBox = new JComboBox<>(RoadType.values());

        одностороннееRadioButton = new JRadioButton("Одностороннее");
        двухстороннееRadioButton = new JRadioButton("Двустороннее");
        directionGroup = new ButtonGroup();
        directionGroup.add(одностороннееRadioButton);
        directionGroup.add(двухстороннееRadioButton);

        lanesPerDirectionSlider = new JSlider(1, 4, params.getLanesPerDirection());
        lanesPerDirectionSlider.setMajorTickSpacing(1);
        lanesPerDirectionSlider.setPaintTicks(true);
        lanesPerDirectionSlider.setPaintLabels(true);
        lanesPerDirectionSlider.setSnapToTicks(true);

        // Настройки для тоннеля
        redLightDurationLabel = new JLabel("Длина красного света:");
        redLightDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1)); // сек
        greenLightDurationLabel = new JLabel("Длина зеленого света:");
        greenLightDurationSpinner = new JSpinner(new SpinnerNumberModel(30, 20, 100, 1)); // сек

        // tunnelClearanceTimeLabel и tunnelClearanceTimeSpinner УДАЛЕНЫ
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Тип автодороги
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Выберите тип автодороги:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; add(roadTypeComboBox, gbc);

        // Направление движения (панель)
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; add(new JLabel("Направление движения:"), gbc);
        JPanel directionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        directionPanel.add(одностороннееRadioButton);
        directionPanel.add(двухстороннееRadioButton);
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; add(directionPanel, gbc);

        // Количество полос
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; add(new JLabel("Количество полос (в 1 напр.):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.HORIZONTAL; add(lanesPerDirectionSlider, gbc);

        // Панель для специфичных настроек тоннеля
        tunnelSpecificPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcTunnel = new GridBagConstraints();
        gbcTunnel.insets = new Insets(2, 0, 2, 5); // Меньше отступы внутри панели
        gbcTunnel.anchor = GridBagConstraints.WEST;

        gbcTunnel.gridx = 0; gbcTunnel.gridy = 0; tunnelSpecificPanel.add(redLightDurationLabel, gbcTunnel);
        gbcTunnel.gridx = 1; gbcTunnel.gridy = 0; gbcTunnel.fill = GridBagConstraints.HORIZONTAL; tunnelSpecificPanel.add(redLightDurationSpinner, gbcTunnel);
        gbcTunnel.gridx = 2; gbcTunnel.gridy = 0; gbcTunnel.fill = GridBagConstraints.NONE; tunnelSpecificPanel.add(new JLabel("секунд"), gbcTunnel);

        gbcTunnel.gridx = 0; gbcTunnel.gridy = 1; gbcTunnel.fill = GridBagConstraints.NONE; tunnelSpecificPanel.add(greenLightDurationLabel, gbcTunnel);
        gbcTunnel.gridx = 1; gbcTunnel.gridy = 1; gbcTunnel.fill = GridBagConstraints.HORIZONTAL; tunnelSpecificPanel.add(greenLightDurationSpinner, gbcTunnel);
        gbcTunnel.gridx = 2; gbcTunnel.gridy = 1; gbcTunnel.fill = GridBagConstraints.NONE; tunnelSpecificPanel.add(new JLabel("секунд"), gbcTunnel);

        // Удалены строки для tunnelClearanceTimeLabel и tunnelClearanceTimeSpinner

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(tunnelSpecificPanel, gbc);


        // Кнопки
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Сохранить настройки");
        saveButton.addActionListener(e -> saveAndClose());
        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> { settingsSaved = false; dispose(); });
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        gbc.insets = new Insets(15, 10, 5, 10); // Отступ сверху для кнопок
        add(buttonPanel, gbc);
    }

    private void addListeners() {
        roadTypeComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateFieldVisibility((RoadType) e.getItem());
            }
        });
    }

    private void updateFieldVisibility(RoadType selectedType) {
        boolean isTunnel = (selectedType == RoadType.TUNNEL);
        boolean isNotTunnel = !isTunnel;

        одностороннееRadioButton.setEnabled(isNotTunnel);
        двухстороннееRadioButton.setEnabled(isNotTunnel);
        lanesPerDirectionSlider.setEnabled(isNotTunnel);
        // Если это тоннель, то количество направлений и полос фиксировано (2 и 1)
        // Можно здесь принудительно установить эти значения, если они не были выбраны
        if(isTunnel) {
            двухстороннееRadioButton.setSelected(true); // Тоннель всегда двухсторонний в нашей модели
            lanesPerDirectionSlider.setValue(1); // И всегда одна полоса на направление
        }


        redLightDurationLabel.setVisible(isTunnel);
        redLightDurationSpinner.setVisible(isTunnel);
        greenLightDurationLabel.setVisible(isTunnel);
        greenLightDurationSpinner.setVisible(isTunnel);
        tunnelSpecificPanel.setVisible(isTunnel); // Показываем/скрываем всю панель

        // Удалены строки для tunnelClearanceTimeLabel и tunnelClearanceTimeSpinner

        pack(); // Перепаковываем диалог, чтобы подогнать размер под видимые компоненты
    }


    private void loadParameters() {
        roadTypeComboBox.setSelectedItem(params.getRoadType());

        if (params.getNumberOfDirections() == 1) {
            одностороннееRadioButton.setSelected(true);
        } else {
            двухстороннееRadioButton.setSelected(true);
        }
        lanesPerDirectionSlider.setValue(params.getLanesPerDirection());

        // Загрузка параметров тоннеля
        if (params.getRoadType() == RoadType.TUNNEL) {
            redLightDurationSpinner.setValue(params.getTunnelDefaultRedDuration());
            greenLightDurationSpinner.setValue(params.getTunnelDefaultGreenDuration());
            // Загрузка для tunnelClearanceTimeSpinner УДАЛЕНА
        }
        // Обновляем видимость на основе загруженного типа
        updateFieldVisibility(params.getRoadType());
    }

    private void saveAndClose() {
        RoadType selectedRoadType = (RoadType) roadTypeComboBox.getSelectedItem();
        params.setRoadType(selectedRoadType);

        if (selectedRoadType == RoadType.TUNNEL) {
            params.setNumberOfDirections(2); // Принудительно для тоннеля
            params.setLanesPerDirection(1);  // Принудительно для тоннеля
            params.setTunnelDefaultRedDuration(((Number) redLightDurationSpinner.getValue()).doubleValue());
            params.setTunnelDefaultGreenDuration(((Number) greenLightDurationSpinner.getValue()).doubleValue());
            // Сохранение для tunnelClearanceTimeSpinner УДАЛЕНО
        } else {
            params.setNumberOfDirections(одностороннееRadioButton.isSelected() ? 1 : 2);
            params.setLanesPerDirection(lanesPerDirectionSlider.getValue());
        }

        settingsSaved = true;
        dispose();
    }

    public boolean showDialog() {
        // loadParameters(); // Вызывается теперь из конструктора и при смене типа
        setVisible(true);
        return settingsSaved;
    }
}
package com.trafficsimulation.gui;

public enum VisualizationMode {
    STANDARD("Стандартный"),
    SPEED_COLOR("Цвет по скорости");
    // DEBUG_INFO("Отладка"); // Можно будет добавить позже

    private final String displayName;

    VisualizationMode(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        // Это имя будет отображаться в JComboBox
        return displayName;
    }
}
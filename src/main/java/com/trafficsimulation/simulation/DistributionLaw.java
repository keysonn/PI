package com.trafficsimulation.simulation;

public enum DistributionLaw {
    UNIFORM("Равномерный"),
    NORMAL("Нормальный"),
    EXPONENTIAL("Показательный");

    private final String displayName;

    DistributionLaw(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
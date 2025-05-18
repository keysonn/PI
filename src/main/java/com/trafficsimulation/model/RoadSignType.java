package com.trafficsimulation.model;

public enum RoadSignType {
    SPEED_LIMIT("Ограничение скорости");

    private final String displayName;

    RoadSignType(String displayName) {
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
package com.trafficsimulation.model;

public enum RoadType {
    CITY_ROAD("Городская дорога", 40, 80, 60),
    HIGHWAY("Автомагистраль", 70, 130, 90),
    TUNNEL("Тоннель", 40, 70, 50);

    private final String displayName;
    private final double minSpeedLimitKmh;
    private final double maxSpeedLimitKmh;
    private final double defaultSpeedLimitKmh;

    RoadType(String displayName, double minSpeedKmh, double maxSpeedKmh, double defaultSpeedKmh) {
        this.displayName = displayName;
        this.minSpeedLimitKmh = minSpeedKmh;
        this.maxSpeedLimitKmh = maxSpeedKmh;
        this.defaultSpeedLimitKmh = defaultSpeedKmh;
    }

    public String getDisplayName() { return displayName; }
    public double getMinSpeedLimitKmh() { return minSpeedLimitKmh; }
    public double getMaxSpeedLimitKmh() { return maxSpeedLimitKmh; }
    public double getDefaultSpeedLimitKmh() { return defaultSpeedLimitKmh; }
    public double getMinSpeedLimitMs() { return minSpeedLimitKmh / 3.6; }
    public double getMaxSpeedLimitMs() { return maxSpeedLimitKmh / 3.6; }
    public double getDefaultSpeedLimitMs() { return defaultSpeedLimitKmh / 3.6; }

    @Override
    public String toString() { return displayName; }
}
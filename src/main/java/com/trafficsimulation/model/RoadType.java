package com.trafficsimulation.model;

public enum RoadType {
    CITY_ROAD(40.0 / 3.6, 80.0 / 3.6),    // Городская дорога: 40-80 км/ч (скорости в м/с)
    HIGHWAY(70.0 / 3.6, 130.0 / 3.6),      // Автомагистраль: 70-130 км/ч (скорости в м/с)
    TUNNEL(40.0 / 3.6, 70.0 / 3.6);        // Тоннель: 40-70 км/ч (скорости в м/с) - можно настроить

    private final double minSpeedLimitMs; // Минимальная "типичная" или разрешенная скорость на этом типе дороги в м/с
    private final double maxSpeedLimitMs; // Максимальная разрешенная скорость на этом типе дороги в м/с

    RoadType(double minSpeedLimitMs, double maxSpeedLimitMs) {
        this.minSpeedLimitMs = minSpeedLimitMs;
        this.maxSpeedLimitMs = maxSpeedLimitMs;
    }

    public double getMinSpeedLimitMs() {
        return minSpeedLimitMs;
    }

    public double getMaxSpeedLimitMs() {
        return maxSpeedLimitMs;
    }

    public double getMinSpeedLimitKmh() {
        return minSpeedLimitMs * 3.6;
    }

    public double getMaxSpeedLimitKmh() {
        return maxSpeedLimitMs * 3.6;
    }
}
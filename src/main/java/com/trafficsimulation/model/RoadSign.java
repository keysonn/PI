package com.trafficsimulation.model;

/**
 * Представляет дорожный знак на определенной позиции.
 */
public class RoadSign {

    private static int idCounter = 0;

    private final int id;
    private final double position;   // Позиция на дороге (метры)
    private final RoadSignType type; // Тип знака

    /**
     * Конструктор знака.
     * @param position Позиция (метры).
     * @param type Тип знака.
     */
    public RoadSign(double position, RoadSignType type) {
        this.id = idCounter++;
        this.position = position;
        this.type = type;
        System.out.println("Создан RoadSign ID=" + this.id + " типа " + this.type + " на позиции " + this.position);
    }

    // --- Геттеры ---
    public int getId() { return id; }
    public double getPosition() { return position; }
    public RoadSignType getType() { return type; }

    /**
     * Возвращает ограничение скорости, если это знак ограничения.
     * @return Скорость в м/с или -1, если знак не скоростной.
     */
    public double getSpeedLimitValue() {
        switch (type) {
            case SPEED_LIMIT_30: return 30.0 / 3.6; // Переводим км/ч в м/с
            case SPEED_LIMIT_60: return 60.0 / 3.6;
            case SPEED_LIMIT_90: return 90.0 / 3.6;
            default: return -1.0; // Знак не является ограничением скорости
        }
    }

    @Override
    public String toString() {
        return String.format("Sign{id=%d, pos=%.1f, type=%s}", id, position, type);
    }
}
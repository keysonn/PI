package com.trafficsimulation.model;

public class RoadSign {

    private static int idCounter = 0;
    private static final boolean DETAILED_LOGGING = true; // Для отладки создания знаков

    private final int id;
    private final double position;        // Позиция знака на дороге в метрах
    private final RoadSignType type;      // Тип знака (например, SPEED_LIMIT)
    private final int targetDirection;  // На какое направление действует: 0 (->), 1 (<-), -1 (оба)
    private final double speedLimitKmh;   // Значение ограничения скорости в км/ч, если это знак SPEED_LIMIT

    /**
     * Конструктор для знаков ограничения скорости.
     * @param position позиция на дороге (м)
     * @param type тип знака (должен быть SPEED_LIMIT)
     * @param targetDirection направление действия
     * @param speedLimitKmh значение ограничения скорости в км/ч
     */
    public RoadSign(double position, RoadSignType type, int targetDirection, double speedLimitKmh) {
        this.id = idCounter++;
        this.position = position;
        this.type = type;
        this.targetDirection = targetDirection;

        if (type == RoadSignType.SPEED_LIMIT) {
            this.speedLimitKmh = speedLimitKmh;
        } else {
            this.speedLimitKmh = 0; // Для не-скоростных знаков лимит не применяется
            if (speedLimitKmh > 0 && DETAILED_LOGGING) {
                System.err.println("Warning: RoadSign constructor called with speedLimitKmh for a non-SPEED_LIMIT sign type. Speed limit ignored.");
            }
        }

        if (DETAILED_LOGGING) {
            System.out.println(this.toString() + " создан.");
        }
    }

    /**
     * Конструктор для не-скоростных знаков (если такие появятся).
     * @param position позиция на дороге (м)
     * @param type тип знака
     * @param targetDirection направление действия
     */
    public RoadSign(double position, RoadSignType type, int targetDirection) {
        this(position, type, targetDirection, 0); // speedLimitKmh по умолчанию 0
    }

    public int getId() {
        return id;
    }

    public double getPosition() {
        return position;
    }

    public RoadSignType getType() {
        return type;
    }

    public int getTargetDirection() {
        return targetDirection;
    }

    /**
     * Возвращает значение ограничения скорости в км/ч.
     * Актуально только для знаков типа SPEED_LIMIT.
     * @return значение ограничения скорости в км/ч, или 0 если не применимо.
     */
    public double getSpeedLimitKmh() {
        return speedLimitKmh;
    }

    /**
     * Возвращает значение ограничения скорости в м/с.
     * Актуально только для знаков типа SPEED_LIMIT.
     * @return значение ограничения скорости в м/с, или -1.0 если не применимо (для совместимости с Car.update).
     */
    public double getSpeedLimitValue() { // Возвращает в м/с
        if (this.type == RoadSignType.SPEED_LIMIT && this.speedLimitKmh > 0) {
            return this.speedLimitKmh / 3.6;
        }
        return -1.0; // Обозначает отсутствие ограничения или неприменимость
    }

    @Override
    public String toString() {
        if (type == RoadSignType.SPEED_LIMIT) {
            return String.format("RoadSign[ID=%d, Type=%s, Limit=%.0fkm/h, Pos=%.1fm, Dir=%d]",
                    id, type, speedLimitKmh, position, targetDirection);
        } else {
            return String.format("RoadSign[ID=%d, Type=%s, Pos=%.1fm, Dir=%d]",
                    id, type, position, targetDirection);
        }
    }
}
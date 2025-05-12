package com.trafficsimulation.model;

public class RoadSign {

    private static int idCounter = 0;
    private final int id;
    private final double position;
    private final RoadSignType type;
    private final int targetDirection; // 0 для направления 0, 1 для направления 1

    public RoadSign(double position, RoadSignType type, int targetDirection) {
        this.id = idCounter++;
        this.position = position;
        this.type = type;
        this.targetDirection = targetDirection;
        System.out.println("Создан RoadSign ID=" + this.id + " типа " + this.type +
                " на поз. " + String.format("%.1f",this.position) + " для напр. " + this.targetDirection);
    }

    public int getTargetDirection() {
        return targetDirection;
    }

    public int getId() { return id; }
    public double getPosition() { return position; }
    public RoadSignType getType() { return type; }

    public double getSpeedLimitValue() {
        switch (type) {
            case SPEED_LIMIT_30: return 30.0 / 3.6;
            case SPEED_LIMIT_60: return 60.0 / 3.6;
            case SPEED_LIMIT_90: return 90.0 / 3.6;
            default: return -1.0;
        }
    }

    @Override
    public String toString() {
        return String.format("Sign{id=%d, dir=%d, pos=%.1f, type=%s}",
                id, targetDirection, position, type);
    }
}
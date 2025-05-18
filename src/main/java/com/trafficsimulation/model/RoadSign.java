package com.trafficsimulation.model;

import java.util.concurrent.atomic.AtomicLong;

public class RoadSign {
    private static final AtomicLong idCounter = new AtomicLong(0);
    private final long id;
    private double position;
    private RoadSignType type;
    private int targetDirection;
    private double speedLimitKmh;

    public RoadSign(double position, RoadSignType type, int targetDirection, double speedLimitKmh) {
        this.id = idCounter.incrementAndGet();
        this.position = position;
        this.type = type;
        this.targetDirection = targetDirection;
        if (type == RoadSignType.SPEED_LIMIT) {
            this.speedLimitKmh = speedLimitKmh;
        } else {
            this.speedLimitKmh = -1;
        }
    }

    public long getId() { return id; }
    public double getPosition() { return position; }
    public RoadSignType getType() { return type; }
    public int getTargetDirection() { return targetDirection; }
    public double getSpeedLimitKmh() { return speedLimitKmh; }
    public double getSpeedLimitValue() { // Returns speed limit in m/s for convenience
        if (type == RoadSignType.SPEED_LIMIT && speedLimitKmh > 0) {
            return speedLimitKmh / 3.6;
        }
        return -1;
    }
}
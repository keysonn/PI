package com.trafficsimulation.model;

import java.util.Random; // <-- ДОБАВЛЕН ИМПОРТ
import java.util.concurrent.atomic.AtomicLong;

public class Car {

    private static final AtomicLong idCounter = new AtomicLong(0);
    private static final Random randomGenerator = new Random(System.currentTimeMillis()); // <-- ДОБАВЛЕНО СТАТИЧЕСКОЕ ПОЛЕ

    // Параметры для IDM и поведения
    private static final double SAFE_TIME_HEADWAY = 2.2;
    private static final double MIN_GAP = 8.0;
    private static final double CRITICAL_GAP_FOR_EMERGENCY_BRAKE = 2.0;
    private static final double MAX_EMERGENCY_DECEL = 9.0;
    private static final double MIN_SPEED_DIFFERENCE_MAINTAIN_MS = 5.0 / 3.6;
    private static final double FOLLOW_SPEED_BUFFER_MS = 2.0 / 3.6;

    private final long id;
    private double position;
    private double currentSpeed;
    private double maxSpeed;
    private double accelerationParam;
    private double baseDecelerationParam;

    private int laneIndex;
    private final int direction;

    public Car(double initialPosition, double initialSpeed, double maxSpeed,
               double accelerationParam, double baseDecelerationParam, int laneIndex, int direction) {
        this.id = idCounter.incrementAndGet();
        this.position = initialPosition;
        this.currentSpeed = Math.abs(initialSpeed);
        this.maxSpeed = Math.abs(maxSpeed);
        this.accelerationParam = accelerationParam;
        this.baseDecelerationParam = baseDecelerationParam;
        this.laneIndex = laneIndex;
        this.direction = direction;
    }

    public void update(double deltaTime, Car leadCar, double distanceToLeadBumperToBumper,
                       double effectiveSpeedLimit, TrafficLightState nextLightState, double distanceToLightAbs) {
        if (deltaTime <= 0) return;

        double desiredFreewaySpeed = Math.min(this.maxSpeed, effectiveSpeedLimit);
        final int delta = 4;
        double freeRoadComponent = this.accelerationParam * (1 - Math.pow(this.currentSpeed / Math.max(0.1, desiredFreewaySpeed), delta));

        double interactionComponentLeader = 0.0;
        if (leadCar != null && distanceToLeadBumperToBumper < 200) {
            double s_leader = distanceToLeadBumperToBumper;
            double delta_v_leader = this.currentSpeed - leadCar.getCurrentSpeed();
            double s_star_leader = MIN_GAP + Math.max(0, this.currentSpeed * SAFE_TIME_HEADWAY +
                    (this.currentSpeed * delta_v_leader) /
                            (2 * Math.sqrt(this.accelerationParam * this.baseDecelerationParam)));
            if (s_leader > 0.1) {
                interactionComponentLeader = -this.accelerationParam * Math.pow(s_star_leader / s_leader, 2);
                if (s_leader < s_star_leader * 1.8 && s_leader > MIN_GAP * 1.2 && this.currentSpeed > leadCar.getCurrentSpeed()) {
                    double targetSpeedToMaintainDiff = leadCar.getCurrentSpeed() + MIN_SPEED_DIFFERENCE_MAINTAIN_MS;
                    if (this.currentSpeed > targetSpeedToMaintainDiff && interactionComponentLeader > -this.baseDecelerationParam * 0.75) {
                        double requiredDecelForSpeedDiff = (this.currentSpeed - targetSpeedToMaintainDiff) / (SAFE_TIME_HEADWAY * 0.5);
                        interactionComponentLeader = Math.min(interactionComponentLeader, -Math.min(this.baseDecelerationParam * 0.75, requiredDecelForSpeedDiff));
                    }
                }
                if (s_leader < MIN_GAP * 1.2 && interactionComponentLeader > -this.baseDecelerationParam) {
                    interactionComponentLeader = -this.baseDecelerationParam;
                }
            } else {
                interactionComponentLeader = -MAX_EMERGENCY_DECEL;
            }
            if (s_leader < CRITICAL_GAP_FOR_EMERGENCY_BRAKE) {
                interactionComponentLeader = Math.min(interactionComponentLeader, -MAX_EMERGENCY_DECEL * (CRITICAL_GAP_FOR_EMERGENCY_BRAKE / Math.max(0.01, s_leader) ) );
                if (leadCar.getCurrentSpeed() < 0.5 && s_leader < CRITICAL_GAP_FOR_EMERGENCY_BRAKE / 2) {
                    this.currentSpeed = 0;
                }
            }
        }

        double interactionComponentLight = 0.0;
        if (nextLightState == TrafficLightState.RED &&
                distanceToLightAbs < 100 && distanceToLightAbs > 0.1) {
            double s_light = distanceToLightAbs;
            double s_star_light = MIN_GAP + this.currentSpeed * SAFE_TIME_HEADWAY;
            if (s_light > 0.1) {
                interactionComponentLight = -this.accelerationParam * Math.pow(s_star_light / s_light, 2);
            } else {
                interactionComponentLight = -MAX_EMERGENCY_DECEL;
            }
            if (this.currentSpeed > 0.5 && s_light < s_star_light * 0.7 && s_light < 25) { // Увеличил немного дистанцию для более раннего торможения
                interactionComponentLight = Math.min(interactionComponentLight, -this.baseDecelerationParam);
            }
            if (this.currentSpeed < 1.0 && s_light < MIN_GAP) {
                interactionComponentLight = Math.min(interactionComponentLight, -this.currentSpeed / Math.max(deltaTime, 0.1));
            }
            if (s_light < CRITICAL_GAP_FOR_EMERGENCY_BRAKE) {
                interactionComponentLight = Math.min(interactionComponentLight, -MAX_EMERGENCY_DECEL);
                if (s_light < CRITICAL_GAP_FOR_EMERGENCY_BRAKE / 2) this.currentSpeed = 0;
            }
        }

        double finalInteractionComponent;
        boolean leaderIsPrimaryObstacle = (leadCar != null && distanceToLeadBumperToBumper < 200);
        boolean lightIsPrimaryObstacle = (nextLightState == TrafficLightState.RED && distanceToLightAbs < 100);

        if (leaderIsPrimaryObstacle && lightIsPrimaryObstacle) {
            finalInteractionComponent = Math.min(interactionComponentLeader, interactionComponentLight);
        } else if (leaderIsPrimaryObstacle) {
            finalInteractionComponent = interactionComponentLeader;
        } else if (lightIsPrimaryObstacle) {
            finalInteractionComponent = interactionComponentLight;
        } else {
            finalInteractionComponent = 0.0;
        }

        double finalAcceleration = freeRoadComponent + finalInteractionComponent;
        if (finalInteractionComponent >= -this.baseDecelerationParam && finalInteractionComponent < 0) {
            finalAcceleration = Math.max(finalAcceleration, -this.baseDecelerationParam);
        } else if (finalInteractionComponent < -this.baseDecelerationParam) {
            finalAcceleration = finalInteractionComponent;
        }
        finalAcceleration = Math.max(finalAcceleration, -MAX_EMERGENCY_DECEL);

        this.currentSpeed += finalAcceleration * deltaTime;
        this.currentSpeed = Math.max(0, this.currentSpeed);
        this.currentSpeed = Math.min(this.currentSpeed, this.maxSpeed);

        if (leadCar != null && distanceToLeadBumperToBumper < MIN_GAP / 2) {
            boolean goingToCollideDir0 = this.direction == 0 && (this.position + this.currentSpeed * deltaTime) > (leadCar.getPosition() - MIN_GAP / 4);
            boolean goingToCollideDir1 = this.direction == 1 && (this.position - this.currentSpeed * deltaTime) < (leadCar.getPosition() + MIN_GAP / 4);

            if (goingToCollideDir0 || goingToCollideDir1) {
                if (this.currentSpeed > leadCar.getCurrentSpeed()) {
                    this.currentSpeed = Math.max(0, leadCar.getCurrentSpeed() - FOLLOW_SPEED_BUFFER_MS);
                }
                if (distanceToLeadBumperToBumper < 0.2) {
                    this.currentSpeed = Math.max(0, leadCar.getCurrentSpeed() - FOLLOW_SPEED_BUFFER_MS * 2);
                    if (this.direction == 0) {
                        this.position = Math.min(this.position, leadCar.getPosition() - (MIN_GAP / 3 + randomGenerator.nextDouble()*0.5)); // <-- ИЗМЕНЕНО
                    } else {
                        this.position = Math.max(this.position, leadCar.getPosition() + (MIN_GAP / 3 + randomGenerator.nextDouble()*0.5)); // <-- ИЗМЕНЕНО
                    }
                }
            }
        }

        double deltaPos = this.currentSpeed * deltaTime + 0.5 * finalAcceleration * deltaTime * deltaTime;
        if (Math.abs(this.currentSpeed) < 0.01 && Math.abs(finalAcceleration) < 0.01 && Math.abs(deltaPos) > 0.01) {
            deltaPos = 0;
        }

        if (this.direction == 0) {
            this.position += deltaPos;
        } else {
            this.position -= deltaPos;
        }
    }

    public long getId() { return id; }
    public double getPosition() { return position; }
    public double getCurrentSpeed() { return currentSpeed; }
    public int getLaneIndex() { return laneIndex; }
    public double getMaxSpeed() { return maxSpeed; }
    public int getDirection() { return direction; }

    @Override
    public String toString() {
        return String.format("Car{id=%d, dir=%d, pos=%.1f, spd=%.1f (%.1f km/h), lane=%d}",
                id, direction, position, currentSpeed, currentSpeed * 3.6, laneIndex);
    }
}
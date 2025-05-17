package com.trafficsimulation.model;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class Car {

    private static final AtomicLong idCounter = new AtomicLong(0);
    // private static final Random randomGenerator = new Random(System.currentTimeMillis());

    public static final double SAFE_TIME_HEADWAY = 2.0;
    public static final double MIN_GAP = 2.5;
    public static final int ACCELERATION_EXPONENT = 4;
    public static final double APPROX_CAR_LENGTH = 5.0;

    public static final double POLITENESS_FACTOR = 0.3; // Пока не используем активно, но оставляем
    public static final double ACCELERATION_THRESHOLD_GAIN = 0.15; // Немного уменьшим порог для большей активности
    public static final double LANE_CHANGE_RIGHT_BIAS = 0.1; // Небольшой "бонус" к ускорению для правой полосы, чтобы стимулировать возврат
    public static final double SAFE_DECELERATION_FOR_OTHERS = 2.5;

    private final long id;
    private double position;
    private double currentSpeed;
    private double desiredSpeed;
    private double maxSpeed;

    private double accelerationParam;
    private double baseDecelerationParam;

    private int laneIndex;
    private final int direction;

    private boolean wantsToChangeLaneLeft = false;
    private boolean wantsToChangeLaneRight = false;
    private double timeSinceLastLaneChange = 10.0;
    private static final double LANE_CHANGE_COOLDOWN = 2.5; // Немного уменьшим кулдаун

    private static final boolean CAR_DEBUG_LOGGING = true;

    public Car(double initialPosition, double initialSpeed, double maxSpeed,
               double accelerationParam, double baseDecelerationParam, int laneIndex, int direction) {
        this.id = idCounter.incrementAndGet();
        this.position = initialPosition;
        this.currentSpeed = Math.max(0, initialSpeed);
        this.maxSpeed = Math.max(0, maxSpeed);
        this.desiredSpeed = this.maxSpeed;
        this.accelerationParam = Math.max(0.5, accelerationParam);
        this.baseDecelerationParam = Math.max(1.0, baseDecelerationParam);
        this.laneIndex = laneIndex;
        this.direction = direction;
    }

    public void updateDesiredSpeed(double externalSpeedLimitMs) {
        this.desiredSpeed = Math.min(this.maxSpeed, externalSpeedLimitMs);
        this.desiredSpeed = Math.max(0, this.desiredSpeed);
    }

    public void update(double deltaTime, Car leadCar, double distanceToLeadBumperToBumper,
                       double effectiveSpeedLimit, TrafficLightState nextLightState, double distanceToLightAbs) {
        if (deltaTime <= 0) return;
        updateDesiredSpeed(effectiveSpeedLimit);

        double deltaV = (leadCar != null) ? this.currentSpeed - leadCar.getCurrentSpeed() : 0;
        double s_star = MIN_GAP + Math.max(0, this.currentSpeed * SAFE_TIME_HEADWAY +
                (this.currentSpeed * deltaV) /
                        (2 * Math.sqrt(this.accelerationParam * this.baseDecelerationParam)));
        double freeRoadTerm = this.accelerationParam * (1 - Math.pow(this.currentSpeed / Math.max(0.1, this.desiredSpeed), ACCELERATION_EXPONENT));
        double interactionTerm = 0.0;
        if (leadCar != null && distanceToLeadBumperToBumper < 200) {
            interactionTerm = -this.accelerationParam * Math.pow(s_star / Math.max(0.1, distanceToLeadBumperToBumper), 2);
        }
        double lightInteractionTerm = 0.0;
        if (nextLightState == TrafficLightState.RED && distanceToLightAbs < 100 && distanceToLightAbs > 0.01) {
            double s_star_light = MIN_GAP + this.currentSpeed * SAFE_TIME_HEADWAY;
            lightInteractionTerm = -this.accelerationParam * Math.pow(s_star_light / Math.max(0.1, distanceToLightAbs), 2);
            if (this.currentSpeed > 0.5 && distanceToLightAbs < MIN_GAP * 1.5 && distanceToLightAbs < this.currentSpeed * SAFE_TIME_HEADWAY) {
                lightInteractionTerm = Math.min(lightInteractionTerm, -this.baseDecelerationParam * 1.2);
            }
            if (this.currentSpeed < 0.5 && distanceToLightAbs < MIN_GAP * 0.5) {
                this.currentSpeed = 0;
                lightInteractionTerm = -100;
            }
        }
        double finalInteraction = Math.min(interactionTerm, lightInteractionTerm);
        if (leadCar == null && (nextLightState != TrafficLightState.RED || distanceToLightAbs > 100) ) {
            finalInteraction = 0;
        }

        double finalAcceleration = freeRoadTerm + finalInteraction;
        finalAcceleration = Math.max(-this.baseDecelerationParam * 2.5, Math.min(finalAcceleration, this.accelerationParam));

        double previousSpeed = this.currentSpeed;
        this.currentSpeed += finalAcceleration * deltaTime;
        this.currentSpeed = Math.max(0, this.currentSpeed);
        this.currentSpeed = Math.min(this.currentSpeed, this.desiredSpeed);

        if (this.currentSpeed < 0.01 && Math.abs(finalAcceleration) < 0.01 &&
                ((nextLightState == TrafficLightState.RED && distanceToLightAbs < MIN_GAP *0.8) ||
                        (leadCar != null && distanceToLeadBumperToBumper < MIN_GAP*0.8 && leadCar.getCurrentSpeed() < 0.1)) ) {
            // Стоим
        } else {
            double averageSpeedForInterval = (previousSpeed + this.currentSpeed) / 2.0;
            double deltaPos = averageSpeedForInterval * deltaTime;
            if (this.direction == 0) {
                this.position += deltaPos;
            } else {
                this.position -= deltaPos;
            }
        }

        if (this.direction == 1 && CAR_DEBUG_LOGGING && deltaTime > 0.001) { // Логируем только если есть движение
            System.out.printf("Car ID %d (Dir 1) updated. Pos: %.2f, Speed: %.2f m/s (%.1f kmh), Accel: %.2f, DesiredSpd: %.1f kmh, dt: %.4f%n",
                    id, position, currentSpeed, currentSpeed * 3.6, finalAcceleration, desiredSpeed*3.6, deltaTime);
        }

        timeSinceLastLaneChange += deltaTime;
        wantsToChangeLaneLeft = false;
        wantsToChangeLaneRight = false;
    }

    /**
     * Оценивает целесообразность смены полосы.
     * @param ownAcceleration текущее ожидаемое ускорение на своей полосе
     * @param leftLanePotentialAccel потенциальное ускорение на левой полосе (null, если смена невозможна)
     * @param rightLanePotentialAccel потенциальное ускорение на правой полосе (null, если смена невозможна)
     * @param isCurrentLaneTheRightmost является ли текущая полоса самой правой в данном направлении
     */
    public void evaluateLaneChange(double ownAcceleration,
                                   Double leftLanePotentialAccel,
                                   Double rightLanePotentialAccel,
                                   boolean isCurrentLaneTheRightmost) {

        if (timeSinceLastLaneChange < LANE_CHANGE_COOLDOWN) {
            this.wantsToChangeLaneLeft = false;
            this.wantsToChangeLaneRight = false;
            return;
        }

        this.wantsToChangeLaneLeft = false;
        this.wantsToChangeLaneRight = false;

        // 1. Проверка смены НАПРАВО (приоритет, если мы не на крайней правой)
        if (!isCurrentLaneTheRightmost && rightLanePotentialAccel != null) {
            // Перестраиваемся направо, если это выгоднее ИЛИ если это "правильная" полоса (LANE_CHANGE_RIGHT_BIAS)
            // и это не приведет к значительному проигрышу в ускорении.
            // old_accel_other_follower - old_accel_self_follower > p * (..) - a_thr ... сложная формула MOBIL
            // Упрощенно: если правая полоса не сильно хуже ИЛИ она лучше + бонус за "правильность"
            if (rightLanePotentialAccel + LANE_CHANGE_RIGHT_BIAS > ownAcceleration + ACCELERATION_THRESHOLD_GAIN) {
                // И если это не сильно хуже, чем оставаться на текущей (или даже лучше)
                if (rightLanePotentialAccel >= ownAcceleration - ACCELERATION_THRESHOLD_GAIN * 0.5) { // Не теряем слишком много
                    this.wantsToChangeLaneRight = true;
                }
            }
        }

        // 2. Проверка смены НАЛЕВО (обычно для обгона)
        if (leftLanePotentialAccel != null) {
            // Перестраиваемся налево, если это ЗНАЧИТЕЛЬНО выгоднее, чем текущая полоса ИЛИ выгоднее, чем правая (если правая была вариантом)
            boolean leftIsBetterThanCurrent = leftLanePotentialAccel > ownAcceleration + ACCELERATION_THRESHOLD_GAIN;
            boolean leftIsBetterThanRightOption = true; // По умолчанию, если правая не рассматривается или хуже

            if (wantsToChangeLaneRight && rightLanePotentialAccel != null) { // Если уже есть желание направо
                leftIsBetterThanRightOption = leftLanePotentialAccel > rightLanePotentialAccel + ACCELERATION_THRESHOLD_GAIN * 0.2; // Левая должна быть ощутимо лучше правой
            }

            if (leftIsBetterThanCurrent && leftIsBetterThanRightOption) {
                this.wantsToChangeLaneLeft = true;
                this.wantsToChangeLaneRight = false; // Отменяем желание направо, если левая предпочтительнее
            }
        }

        // Дополнительная проверка: если мы очень медленно едем (<50% от желаемой),
        // а текущее ускорение очень низкое (например, уперлись в кого-то),
        // и есть ЛЮБОЙ выигрыш на соседней полосе, пытаемся перестроиться.
        if (!wantsToChangeLaneLeft && !wantsToChangeLaneRight && currentSpeed < desiredSpeed * 0.5 && ownAcceleration < 0.1) {
            if (leftLanePotentialAccel != null && leftLanePotentialAccel > ownAcceleration + 0.05) { // Даже небольшой выигрыш
                wantsToChangeLaneLeft = true;
            } else if (rightLanePotentialAccel != null && rightLanePotentialAccel > ownAcceleration + 0.05) {
                wantsToChangeLaneRight = true;
            }
        }
    }


    public boolean wantsToChangeLaneLeft() { return wantsToChangeLaneLeft; }
    public boolean wantsToChangeLaneRight() { return wantsToChangeLaneRight; }

    public void setLaneIndex(int newLaneIndex) {
        // if (CAR_DEBUG_LOGGING) System.out.println("Car " + id + " SETLANE from " + this.laneIndex + " to " + newLaneIndex);
        this.laneIndex = newLaneIndex;
        this.wantsToChangeLaneLeft = false;
        this.wantsToChangeLaneRight = false;
        this.timeSinceLastLaneChange = 0.0;
    }

    public long getId() { return id; }
    public double getPosition() { return position; }
    public double getCurrentSpeed() { return currentSpeed; }
    public int getLaneIndex() { return laneIndex; }
    public double getMaxSpeed() { return maxSpeed; }
    public double getDesiredSpeed() { return desiredSpeed; }
    public int getDirection() { return direction; }
    public double getAccelerationParam() { return accelerationParam; }
    public double getBaseDecelerationParam() { return baseDecelerationParam; }

    @Override
    public String toString() {
        return String.format("Car{id=%d, dir=%d, lane=%d, pos=%.1f, spd=%.1f (%.1f km/h), desiredSpd=%.1f km/h}",
                id, direction, laneIndex, position, currentSpeed, currentSpeed * 3.6, desiredSpeed * 3.6);
    }
}
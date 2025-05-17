package com.trafficsimulation.model;

import java.util.concurrent.atomic.AtomicLong;

public class Car {

    private static final AtomicLong idCounter = new AtomicLong(0);

    // IDM Параметры
    public static final double SAFE_TIME_HEADWAY = 1.5; // s, Уменьшим немного, чтобы были чуть агрессивнее в следовании
    public static final double MIN_GAP = 2.5;           // m, Минимальный зазор до препятствия (s0)
    public static final int ACCELERATION_EXPONENT = 4;  // Дельта, стандартный показатель степени для IDM
    public static final double APPROX_CAR_LENGTH = 4.5; // m, Длина машины

    // Параметры для смены полос
    public static final double POLITENESS_FACTOR = 0.1;         // p, Сделаем их менее "вежливыми" (меньше думают о других при оценке своего выигрыша)
    public static final double ACCELERATION_THRESHOLD_GAIN = 0.3; // a_thr, Немного увеличим порог выигрыша, чтобы не меняли полосу по мелочи
    public static final double LANE_CHANGE_RIGHT_BIAS_ACCEL = 0.15; // "Бонус" к ускорению для правой полосы (стимул вернуться)
    public static final double SAFE_DECELERATION_FOR_OTHERS = 2.8; // b_safe, Макс. безопасное замедление для других (м/с^2) - чуть строже
    private static final double LANE_CHANGE_DURATION = 1.8;      // s, Длительность маневра смены полосы - чуть дольше для плавности
    private static final double LANE_CHANGE_COOLDOWN = 2.5;      // s, Минимальное время между сменами полос

    private final long id;
    private double position;
    private double currentSpeed;
    private double desiredSpeed;
    private double maxSpeed;

    private double accelerationParam;
    private double baseDecelerationParam;

    private int currentLaneIndex;
    private final int direction;

    private boolean isChangingLane = false;
    private int targetLaneForChange = -1;
    private double laneChangeProgress = 0.0;
    private double timeSinceLastLaneChange = LANE_CHANGE_COOLDOWN;

    private boolean wantsToChangeLaneLeft = false;
    private boolean wantsToChangeLaneRight = false;

    private static final boolean CAR_DEBUG_LOGGING = false; // По умолчанию выключим для производительности

    public Car(double initialPosition, double initialSpeed, double maxSpeed,
               double accelerationParam, double baseDecelerationParam,
               int localLaneIndex, int direction) {
        this.id = idCounter.incrementAndGet();
        this.position = initialPosition;
        this.currentSpeed = Math.max(0, initialSpeed);
        this.maxSpeed = Math.max(0, maxSpeed);
        this.desiredSpeed = this.maxSpeed;
        this.accelerationParam = Math.max(0.5, accelerationParam);
        this.baseDecelerationParam = Math.max(1.0, baseDecelerationParam);
        this.currentLaneIndex = localLaneIndex;
        this.direction = direction;
    }

    public void updateDesiredSpeed(double externalSpeedLimitMs) {
        this.desiredSpeed = Math.min(this.maxSpeed, externalSpeedLimitMs);
        this.desiredSpeed = Math.max(0, this.desiredSpeed);
    }

    public void update(double deltaTime, Car leadCar, double distanceToLeadBumperToBumper,
                       double effectiveSpeedLimit, TrafficLightState nextLightState, double distanceToLightAbs) {
        if (deltaTime <= 0) return;

        timeSinceLastLaneChange += deltaTime;

        if (isChangingLane) {
            laneChangeProgress += deltaTime / LANE_CHANGE_DURATION;
            if (laneChangeProgress >= 1.0) {
                isChangingLane = false;
                laneChangeProgress = 0.0;
                this.currentLaneIndex = this.targetLaneForChange;
                this.targetLaneForChange = -1;
                timeSinceLastLaneChange = 0.0;
                // if (CAR_DEBUG_LOGGING) System.out.println("Car " + id + " FINISHED lane change to local " + this.currentLaneIndex);
            }
        }

        updateDesiredSpeed(effectiveSpeedLimit);

        double deltaV = (leadCar != null) ? this.currentSpeed - leadCar.getCurrentSpeed() : 0;
        double s_star = MIN_GAP + Math.max(0, this.currentSpeed * SAFE_TIME_HEADWAY +
                (this.currentSpeed * deltaV) / (2 * Math.sqrt(this.accelerationParam * this.baseDecelerationParam)));

        double freeRoadTerm = this.accelerationParam * (1 - Math.pow(this.currentSpeed / Math.max(0.1, this.desiredSpeed), ACCELERATION_EXPONENT));
        double interactionTerm = 0.0;
        if (leadCar != null && distanceToLeadBumperToBumper < 200) {
            interactionTerm = -this.accelerationParam * Math.pow(s_star / Math.max(0.1, distanceToLeadBumperToBumper), 2);
        }

        double lightInteractionTerm = 0.0;
        if (nextLightState == TrafficLightState.RED && distanceToLightAbs < 100 && distanceToLightAbs > 0.01) {
            double s_star_light = MIN_GAP + this.currentSpeed * SAFE_TIME_HEADWAY;
            lightInteractionTerm = -this.accelerationParam * Math.pow(s_star_light / Math.max(0.1, distanceToLightAbs), 2);
            if (this.currentSpeed > 0.5 && distanceToLightAbs < MIN_GAP * 1.5 && distanceToLightAbs < this.currentSpeed * SAFE_TIME_HEADWAY * 0.7) {
                lightInteractionTerm = Math.min(lightInteractionTerm, -this.baseDecelerationParam * 1.2);
            }
            if (this.currentSpeed < 0.5 && distanceToLightAbs < MIN_GAP * 0.5) {
                this.currentSpeed = 0;
                lightInteractionTerm = -100;
            }
        }

        double finalInteraction = Math.min(interactionTerm, lightInteractionTerm);
        if (leadCar == null && (nextLightState != TrafficLightState.RED || distanceToLightAbs > 100 || distanceToLightAbs <= 0.01) ) {
            finalInteraction = 0;
        }

        double finalAcceleration = freeRoadTerm + finalInteraction;
        finalAcceleration = Math.max(-this.baseDecelerationParam * 2.5, Math.min(finalAcceleration, this.accelerationParam));

        double previousSpeed = this.currentSpeed;
        this.currentSpeed += finalAcceleration * deltaTime;
        this.currentSpeed = Math.max(0, this.currentSpeed);
        this.currentSpeed = Math.min(this.currentSpeed, this.desiredSpeed);

        boolean fullyStoppedCondition = this.currentSpeed < 0.01 && Math.abs(finalAcceleration) < 0.01 &&
                ( (nextLightState == TrafficLightState.RED && distanceToLightAbs < MIN_GAP *0.8) ||
                        (leadCar != null && distanceToLeadBumperToBumper < MIN_GAP*0.8 && leadCar.getCurrentSpeed() < 0.1) );

        if (!fullyStoppedCondition) {
            double averageSpeedForInterval = (previousSpeed + this.currentSpeed) / 2.0;
            double deltaPos = averageSpeedForInterval * deltaTime;
            if (this.direction == 0) {
                this.position += deltaPos;
            } else {
                this.position -= deltaPos;
            }
        } else {
            this.currentSpeed = 0;
        }

        wantsToChangeLaneLeft = false;
        wantsToChangeLaneRight = false;
    }

    public void evaluateLaneChangeDecision(double ownAcceleration,
                                           Double leftLanePotentialAccel_noSafetyCheck,
                                           Double rightLanePotentialAccel_noSafetyCheck,
                                           boolean isCurrentLaneTheRightmost,
                                           boolean isCurrentLaneTheLeftmost,
                                           boolean overallSafetyLeft,
                                           boolean overallSafetyRight) {

        if (isChangingLane || timeSinceLastLaneChange < LANE_CHANGE_COOLDOWN) {
            this.wantsToChangeLaneLeft = false;
            this.wantsToChangeLaneRight = false;
            return;
        }

        this.wantsToChangeLaneLeft = false;
        this.wantsToChangeLaneRight = false;

        // --- Решение о смене НАПРАВО (возврат на "правильную" полосу) ---
        if (!isCurrentLaneTheRightmost && rightLanePotentialAccel_noSafetyCheck != null && overallSafetyRight) {
            double incentiveRight = rightLanePotentialAccel_noSafetyCheck - ownAcceleration;
            incentiveRight += LANE_CHANGE_RIGHT_BIAS_ACCEL; // Добавляем бонус за правую полосу

            // Условие MOBIL: a_n_adj > a_c + p * (a_o_adj - a_o_c) - a_thr
            // Здесь: rightLanePotentialAccel > ownAcceleration - LANE_CHANGE_RIGHT_BIAS_ACCEL + ACCELERATION_THRESHOLD_GAIN (упрощенно, если p=0)
            // Или, если правая полоса просто "достаточно хороша" с учетом бонуса
            if (incentiveRight > ACCELERATION_THRESHOLD_GAIN * 0.5) { // Меньший порог для возврата направо
                this.wantsToChangeLaneRight = true;
            }
        }

        // --- Решение о смене НАЛЕВО (обгон) ---
        if (!isCurrentLaneTheLeftmost && leftLanePotentialAccel_noSafetyCheck != null && overallSafetyLeft) {
            double incentiveLeft = leftLanePotentialAccel_noSafetyCheck - ownAcceleration;

            // Сравниваем с текущей ситуацией И с вариантом перестроения направо (если он рассматривается)
            boolean preferLeftOverCurrent = incentiveLeft > ACCELERATION_THRESHOLD_GAIN;
            boolean preferLeftOverRightOption = true;

            if (this.wantsToChangeLaneRight && rightLanePotentialAccel_noSafetyCheck != null) {
                // Если уже хотим направо, то налево должно быть ЗНАЧИТЕЛЬНО лучше, чем направо (с учетом бонуса за правую)
                preferLeftOverRightOption = leftLanePotentialAccel_noSafetyCheck > (rightLanePotentialAccel_noSafetyCheck + LANE_CHANGE_RIGHT_BIAS_ACCEL + ACCELERATION_THRESHOLD_GAIN * 0.2);
            }

            if (preferLeftOverCurrent && preferLeftOverRightOption) {
                this.wantsToChangeLaneLeft = true;
                this.wantsToChangeLaneRight = false; // Отменяем желание направо
            }
        }

        // Дополнительная логика: если мы очень медленно едем (уперлись) и текущее ускорение почти 0,
        // а на соседней полосе ЛЮБОЙ безопасный выигрыш, то пытаемся перестроиться.
        if (!this.wantsToChangeLaneLeft && !this.wantsToChangeLaneRight &&
                currentSpeed < desiredSpeed * 0.4 && // Едем медленнее 40% от желаемой
                ownAcceleration < 0.05 && ownAcceleration > -0.5) { // Почти не ускоряемся, но и не экстренно тормозим

            if (!isCurrentLaneTheLeftmost && leftLanePotentialAccel_noSafetyCheck != null && overallSafetyLeft &&
                    leftLanePotentialAccel_noSafetyCheck > ownAcceleration + ACCELERATION_THRESHOLD_GAIN * 0.25) { // Небольшой, но заметный выигрыш
                wantsToChangeLaneLeft = true;
            } else if (!isCurrentLaneTheRightmost && rightLanePotentialAccel_noSafetyCheck != null && overallSafetyRight &&
                    rightLanePotentialAccel_noSafetyCheck + LANE_CHANGE_RIGHT_BIAS_ACCEL > ownAcceleration + ACCELERATION_THRESHOLD_GAIN * 0.25) {
                wantsToChangeLaneRight = true;
            }
        }

        // if (CAR_DEBUG_LOGGING && (wantsToChangeLaneLeft || wantsToChangeLaneRight)) {
        //     System.out.printf("Car %d Eval LC: ownA=%.2f leftPotA=%.2f (safeL=%b) rightPotA=%.2f (safeR=%b) | wantsL=%b wantsR=%b%n",
        //         id, ownAcceleration,
        //         leftLanePotentialAccel_noSafetyCheck !=null ? leftLanePotentialAccel_noSafetyCheck : -99, overallSafetyLeft,
        //         rightLanePotentialAccel_noSafetyCheck !=null ? rightLanePotentialAccel_noSafetyCheck : -99, overallSafetyRight,
        //         wantsToChangeLaneLeft, wantsToChangeLaneRight);
        // }
    }

    public void startLaneChange(int targetLocalLane) {
        if (!isChangingLane && this.currentLaneIndex != targetLocalLane) {
            this.isChangingLane = true;
            this.targetLaneForChange = targetLocalLane;
            this.laneChangeProgress = 0.0;
            // if (CAR_DEBUG_LOGGING) System.out.println("Car " + id + " STARTING lane change from local " + this.currentLaneIndex + " to " + targetLocalLane);
        }
    }

    public boolean isChangingLane() { return isChangingLane; }
    public double getLaneChangeProgress() { return laneChangeProgress; }
    public int getCurrentLaneIndex() { return currentLaneIndex; }
    public int getTargetLaneForChange() { return targetLaneForChange; }
    public boolean wantsToChangeLaneLeft() { return wantsToChangeLaneLeft; }
    public boolean wantsToChangeLaneRight() { return wantsToChangeLaneRight; }
    public void resetLaneChangeDesire() { wantsToChangeLaneLeft = false; wantsToChangeLaneRight = false;}

    public long getId() { return id; }
    public double getPosition() { return position; }
    public double getCurrentSpeed() { return currentSpeed; }
    public double getMaxSpeed() { return maxSpeed; }
    public double getDesiredSpeed() { return desiredSpeed; }
    public int getDirection() { return direction; }
    public double getAccelerationParam() { return accelerationParam; }
    public double getBaseDecelerationParam() { return baseDecelerationParam; }

    @Override
    public String toString() {
        return String.format("Car{id=%d, dir=%d, locLane=%d, pos=%.1f, spd=%.1f (%.1f km/h), desiredSpd=%.1f km/h, changing=%b(%.1f%% to %d)}",
                id, direction, currentLaneIndex, position, currentSpeed, currentSpeed * 3.6, desiredSpeed * 3.6,
                isChangingLane, laneChangeProgress*100, targetLaneForChange);
    }
}
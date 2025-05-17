package com.trafficsimulation.model;

import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;

public class Car {

    private static final AtomicLong idCounter = new AtomicLong(0);
    private static final Random random = new Random();

    public static final double SAFE_TIME_HEADWAY = 1.6;
    public static final double MIN_GAP = 2.0;
    public static final int ACCELERATION_EXPONENT = 4;
    public static final double APPROX_CAR_LENGTH = 4.5;

    public static final double POLITENESS_FACTOR = 0.2;
    public static final double BASE_ACCEL_GAIN_THRESHOLD_OVERTAKE = 0.5;
    public static final double BASE_ACCEL_GAIN_THRESHOLD_REGULAR = 0.2;

    public static final double LANE_CHANGE_MANEUVER_COST = 0.1;
    public static final double OPTIMAL_LANE_BIAS_ACCEL = 0.3;
    public static final double RIGHTMOST_LANE_BIAS_ACCEL = 0.1;

    public static final double SAFE_DECELERATION_FOR_OTHERS = 3.0;
    private static final double LANE_CHANGE_DURATION = 2.0;
    private static final double LANE_CHANGE_COOLDOWN = 2.5;
    private static final double DECISION_MAKING_INTERVAL = 0.5;

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
    private double timeSinceLastLaneChangeDecision = 0.0;
    private double timeSinceChangeCompleted = LANE_CHANGE_COOLDOWN;

    private boolean committedToChangeLeft = false;
    private boolean committedToChangeRight = false;
    private int committedTargetLane = -1;

    private static final boolean CAR_DEBUG_LOGGING = false;

    public Car(
            double initialPosition, double initialSpeed, double maxSpeed,
            double accelerationParam, double baseDecelerationParam,
            int localLaneIndex, int direction
    ) {
        this.id = idCounter.incrementAndGet();
        this.position = initialPosition;
        this.currentSpeed = Math.max(0, initialSpeed);
        this.maxSpeed = Math.max(0, maxSpeed);
        this.desiredSpeed = this.maxSpeed;
        this.accelerationParam = Math.max(0.5, accelerationParam);
        this.baseDecelerationParam = Math.max(1.0, baseDecelerationParam);
        this.currentLaneIndex = localLaneIndex;
        this.direction = direction;
        this.timeSinceChangeCompleted = LANE_CHANGE_COOLDOWN;
        this.timeSinceLastLaneChangeDecision = DECISION_MAKING_INTERVAL;
    }

    public void updateDesiredSpeed(double externalSpeedLimitMs) {
        this.desiredSpeed = Math.min(this.maxSpeed, externalSpeedLimitMs);
        this.desiredSpeed = Math.max(0, this.desiredSpeed);
    }

    public void update(double deltaTime, Car leadCar, double distanceToLeadBumperToBumper,
                       double effectiveSpeedLimit, TrafficLightState nextLightState, double distanceToLightAbs) {
        if (deltaTime <= 0) return;

        timeSinceChangeCompleted += deltaTime;
        timeSinceLastLaneChangeDecision += deltaTime;

        if (isChangingLane) {
            laneChangeProgress += deltaTime / LANE_CHANGE_DURATION;
            if (laneChangeProgress >= 1.0) {
                isChangingLane = false;
                laneChangeProgress = 0.0;
                this.currentLaneIndex = this.targetLaneForChange;
                this.targetLaneForChange = -1;
                this.committedToChangeLeft = false;
                this.committedToChangeRight = false;
                this.committedTargetLane = -1;
                timeSinceChangeCompleted = 0.0;
                if (CAR_DEBUG_LOGGING) System.out.printf("Car %d FINISHED LC to %d%n", id, currentLaneIndex);
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
                this.currentSpeed = 0; lightInteractionTerm = -100;
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
            if (this.direction == 0) { this.position += deltaPos; } else { this.position -= deltaPos; }
        } else { this.currentSpeed = 0; }
    }

    public boolean canConsiderLaneChange() {
        return !isChangingLane && timeSinceChangeCompleted >= LANE_CHANGE_COOLDOWN && timeSinceLastLaneChangeDecision >= DECISION_MAKING_INTERVAL;
    }

    public void decideLaneChange(double ownCurrentAcceleration,
                                 Double leftLanePotentialAccel, Double rightLanePotentialAccel,
                                 boolean isCurrentLaneTheRightmost, boolean isCurrentLaneTheLeftmost,
                                 boolean overallSafetyLeft, boolean overallSafetyRight,
                                 boolean leftChangeHurtsOthersTooMuch, boolean rightChangeHurtsOthersTooMuch,
                                 int lanesPerDirection, double roadTypeDefaultSpeedMs, double roadTypeMaxSpeedMs) {

        if (!canConsiderLaneChange()) {
            resetCommitments();
            return;
        }
        resetCommitments();
        timeSinceLastLaneChangeDecision = 0.0;

        int optimalLaneIndex = 0;
        if (lanesPerDirection > 1) {
            if (this.desiredSpeed >= roadTypeMaxSpeedMs * 0.85 && !isCurrentLaneTheLeftmost) {
                optimalLaneIndex = lanesPerDirection - 1;
            }
            else if (this.desiredSpeed > roadTypeDefaultSpeedMs * 1.1 && currentLaneIndex < lanesPerDirection - 1) {
                optimalLaneIndex = Math.min(currentLaneIndex + 1 + random.nextInt(Math.max(1, lanesPerDirection - (currentLaneIndex+1) )/2 +1 ), lanesPerDirection -1);
            }
            else if (this.desiredSpeed < roadTypeDefaultSpeedMs * 0.9 && !isCurrentLaneTheRightmost) {
                optimalLaneIndex = 0;
            } else {
                optimalLaneIndex = currentLaneIndex;
            }
        }
        if (CAR_DEBUG_LOGGING) System.out.printf("Car %d: OptimalLane=%d (curr=%d, desiredSpd=%.1f, roadDefSpd=%.1f)%n", id, optimalLaneIndex, currentLaneIndex, desiredSpeed*3.6, roadTypeDefaultSpeedMs*3.6);

        double bestGain = -Double.MAX_VALUE;
        int bestTargetLane = -1;

        double currentAccelGainThresholdOvertake = BASE_ACCEL_GAIN_THRESHOLD_OVERTAKE;
        double currentAccelGainThresholdRegular = BASE_ACCEL_GAIN_THRESHOLD_REGULAR;
        if (currentSpeed < desiredSpeed * 0.5 && ownCurrentAcceleration < 0) {
            currentAccelGainThresholdOvertake *= 0.5;
            currentAccelGainThresholdRegular *= 0.5;
        }

        if (!isCurrentLaneTheLeftmost && leftLanePotentialAccel != null && overallSafetyLeft && !leftChangeHurtsOthersTooMuch) {
            double gainLeft = leftLanePotentialAccel - ownCurrentAcceleration - LANE_CHANGE_MANEUVER_COST;
            if (currentLaneIndex + 1 == optimalLaneIndex) gainLeft += OPTIMAL_LANE_BIAS_ACCEL;

            if (gainLeft > currentAccelGainThresholdOvertake) {
                if (gainLeft > bestGain) {
                    bestGain = gainLeft;
                    bestTargetLane = currentLaneIndex + 1;
                }
            }
        }

        if (!isCurrentLaneTheRightmost && rightLanePotentialAccel != null && overallSafetyRight && !rightChangeHurtsOthersTooMuch) {
            double gainRight = rightLanePotentialAccel - ownCurrentAcceleration - LANE_CHANGE_MANEUVER_COST;
            if (currentLaneIndex - 1 == optimalLaneIndex) {
                gainRight += OPTIMAL_LANE_BIAS_ACCEL;
            } else if (currentLaneIndex - 1 == 0) {
                gainRight += RIGHTMOST_LANE_BIAS_ACCEL;
            }
            if (gainRight > currentAccelGainThresholdRegular) {
                if (gainRight > bestGain) {
                    bestGain = gainRight;
                    bestTargetLane = currentLaneIndex - 1;
                }
            }
        }

        if (bestTargetLane != -1) {
            if (bestTargetLane > currentLaneIndex) {
                this.committedToChangeLeft = true;
            } else {
                this.committedToChangeRight = true;
            }
            this.committedTargetLane = bestTargetLane;
            if (CAR_DEBUG_LOGGING) System.out.printf("Car %d COMMITTED to change to local %d (from %d). BestGain: %.2f%n", id, bestTargetLane, currentLaneIndex, bestGain);
        }
    }

    public void startLaneChangeIfCommitted(int targetLocalLane) {
        if ((committedToChangeLeft && targetLocalLane > currentLaneIndex) ||
                (committedToChangeRight && targetLocalLane < currentLaneIndex)) {
            if (this.committedTargetLane == targetLocalLane) {
                startLaneChange(targetLocalLane);
            } else {
                if (CAR_DEBUG_LOGGING) System.err.printf("Car %d: Mismatch in committed target (%d) and approved target (%d)%n", id, committedTargetLane, targetLocalLane);
                resetCommitments();
            }
        }
    }

    private void startLaneChange(int targetLocalLane) {
        if (!isChangingLane && this.currentLaneIndex != targetLocalLane) {
            this.isChangingLane = true;
            this.targetLaneForChange = targetLocalLane;
            this.laneChangeProgress = 0.0;
            if (CAR_DEBUG_LOGGING) System.out.println("Car " + id + " STARTING lane change from local " + this.currentLaneIndex + " to " + targetLocalLane);
        }
    }

    public void resetCommitments() { // <--- МОДИФИКАТОР ДОСТУПА ИЗМЕНЕН НА PUBLIC
        this.committedToChangeLeft = false;
        this.committedToChangeRight = false;
        this.committedTargetLane = -1;
    }

    public boolean isCommittedToChangeLeft() { return committedToChangeLeft; }
    public boolean isCommittedToChangeRight() { return committedToChangeRight; }
    public int getCommittedTargetLane() { return committedTargetLane; }

    public boolean isChangingLane() { return isChangingLane; }
    public double getLaneChangeProgress() { return laneChangeProgress; }
    public int getCurrentLaneIndex() { return currentLaneIndex; }
    public int getTargetLaneForChange() { return targetLaneForChange; }
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
        return String.format("Car{id=%d, dir=%d, locLane=%d, pos=%.1f, spd=%.1f (%.1f km/h), desiredSpd=%.1f km/h, changing=%b(%.1f%% to %d), commitL=%b, commitR=%b, commitT=%d}",
                id, direction, currentLaneIndex, position, currentSpeed, currentSpeed * 3.6, desiredSpeed * 3.6,
                isChangingLane, laneChangeProgress*100, targetLaneForChange, committedToChangeLeft, committedToChangeRight, committedTargetLane);
    }
}
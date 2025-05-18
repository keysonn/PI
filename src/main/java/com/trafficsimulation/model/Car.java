package com.trafficsimulation.model;

import java.util.concurrent.atomic.AtomicLong;
import java.util.Random;

public class Car {

    private static final AtomicLong idCounter = new AtomicLong(0);
    private static final Random random = new Random();

    public enum DriverType {
        CAUTIOUS(0.88, 1.25, 0.85),
        NORMAL(0.97, 1.0, 1.0),
        AGGRESSIVE(1.03, 0.75, 1.15);

        final double desiredSpeedMultiplier;
        final double idmTimeHeadwayMultiplier;
        final double idmAccelParamMultiplier;

        DriverType(double dsm, double thm, double apm) {
            this.desiredSpeedMultiplier = dsm;
            this.idmTimeHeadwayMultiplier = thm;
            this.idmAccelParamMultiplier = apm;
        }
    }

    public static final double BASE_SAFE_TIME_HEADWAY = 1.8;
    public static final double BASE_ACCELERATION_PARAM = 1.7;
    public static final double BASE_DECELERATION_PARAM = 2.8;

    public static final double MIN_GAP = 2.0;
    public static final int ACCELERATION_EXPONENT = 4;
    public static final double APPROX_CAR_LENGTH = 4.5;

    public static final double POLITENESS_FACTOR = 0.2;
    public static final double BASE_ACCEL_GAIN_THRESHOLD_OVERTAKE = 0.5;
    public static final double BASE_ACCEL_GAIN_THRESHOLD_REGULAR = 0.2;
    public static final double LANE_CHANGE_MANEUVER_COST = 0.1;
    public static final double OPTIMAL_LANE_BIAS_ACCEL = 0.3;
    public static final double RIGHTMOST_LANE_BIAS_ACCEL = 0.15;
    public static final double SAFE_DECELERATION_FOR_OTHERS = 3.0;
    private static final double LANE_CHANGE_DURATION = 2.0;
    private static final double LANE_CHANGE_COOLDOWN = 2.5;
    private static final double DECISION_MAKING_INTERVAL = 0.5;

    private final long id;
    private double position;
    private double currentSpeed;
    private double desiredSpeed;
    private final double maxSpeed;

    private final double actualAccelerationParam;
    private final double actualBaseDecelerationParam;
    private final double actualSafeTimeHeadway;

    private int currentLaneIndex;
    private final int direction;
    private final DriverType driverType;

    private boolean isChangingLane = false;
    private int targetLaneForChange = -1;
    private double laneChangeProgress = 0.0;
    private double timeSinceLastLaneChangeDecision = DECISION_MAKING_INTERVAL;
    private double timeSinceChangeCompleted = LANE_CHANGE_COOLDOWN;

    private boolean committedToChangeLeft = false;
    private boolean committedToChangeRight = false;
    private int committedTargetLane = -1;

    private boolean isBraking = false;
    private static final double BRAKING_THRESHOLD = -0.5;


    public Car(double initialPosition, double initialSpeed, double personalMaxSpeedMs,
               DriverType driverType,
               int localLaneIndex, int direction) {
        this.id = idCounter.incrementAndGet();
        this.position = initialPosition;
        this.currentSpeed = Math.max(0, initialSpeed);
        this.maxSpeed = Math.max(0, personalMaxSpeedMs);
        this.driverType = driverType;
        this.currentLaneIndex = localLaneIndex;
        this.direction = direction;

        this.actualSafeTimeHeadway = BASE_SAFE_TIME_HEADWAY * driverType.idmTimeHeadwayMultiplier;
        this.actualAccelerationParam = BASE_ACCELERATION_PARAM * driverType.idmAccelParamMultiplier;
        this.actualBaseDecelerationParam = BASE_DECELERATION_PARAM;
        this.desiredSpeed = this.maxSpeed;
        this.timeSinceChangeCompleted = LANE_CHANGE_COOLDOWN;
        this.timeSinceLastLaneChangeDecision = DECISION_MAKING_INTERVAL;
    }

    public void updateDesiredSpeed(double externalSpeedLimitMs) {
        double capSpeed = Math.min(this.maxSpeed, externalSpeedLimitMs);
        this.desiredSpeed = capSpeed * this.driverType.desiredSpeedMultiplier;
        this.desiredSpeed = Math.min(this.desiredSpeed, capSpeed);
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
            }
        }

        updateDesiredSpeed(effectiveSpeedLimit);

        double deltaV = (leadCar != null) ? this.currentSpeed - leadCar.getCurrentSpeed() : 0;
        double s_star = MIN_GAP + Math.max(0, this.currentSpeed * actualSafeTimeHeadway +
                (this.currentSpeed * deltaV) / (2 * Math.sqrt(actualAccelerationParam * actualBaseDecelerationParam)));

        double freeRoadTerm = actualAccelerationParam * (1 - Math.pow(this.currentSpeed / Math.max(0.1, this.desiredSpeed), ACCELERATION_EXPONENT));
        double interactionTerm = 0.0;
        if (leadCar != null && distanceToLeadBumperToBumper < 200) {
            interactionTerm = -actualAccelerationParam * Math.pow(s_star / Math.max(0.1, distanceToLeadBumperToBumper), 2);
        }

        double lightInteractionTerm = 0.0;
        if (nextLightState == TrafficLightState.RED && distanceToLightAbs < 100 && distanceToLightAbs > 0.01) {
            double s_star_light = MIN_GAP + this.currentSpeed * actualSafeTimeHeadway;
            lightInteractionTerm = -actualAccelerationParam * Math.pow(s_star_light / Math.max(0.1, distanceToLightAbs), 2);
            if (this.currentSpeed > 0.5 && distanceToLightAbs < MIN_GAP * 1.5 && distanceToLightAbs < this.currentSpeed * actualSafeTimeHeadway * 0.7) {
                lightInteractionTerm = Math.min(lightInteractionTerm, -actualBaseDecelerationParam * 1.2);
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
        finalAcceleration = Math.max(-actualBaseDecelerationParam * 2.5, Math.min(finalAcceleration, actualAccelerationParam));

        isBraking = (finalAcceleration < BRAKING_THRESHOLD);

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
        } else { this.currentSpeed = 0; isBraking = false; }
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
            if (driverType == DriverType.AGGRESSIVE && this.desiredSpeed > roadTypeDefaultSpeedMs * 1.05 && !isCurrentLaneTheLeftmost) {
                optimalLaneIndex = lanesPerDirection - 1;
            }
            else if (driverType == DriverType.CAUTIOUS) {
                optimalLaneIndex = 0;
            }
            else {
                if (this.desiredSpeed >= roadTypeMaxSpeedMs * 0.80 && !isCurrentLaneTheLeftmost) {
                    optimalLaneIndex = lanesPerDirection - 1;
                } else if (this.desiredSpeed > roadTypeDefaultSpeedMs * 1.05 && currentLaneIndex < lanesPerDirection - 1) {
                    if (lanesPerDirection > 2) optimalLaneIndex = Math.min(currentLaneIndex + 1 + random.nextInt(Math.max(1, lanesPerDirection - (currentLaneIndex + 1))), lanesPerDirection - 1); // Было /2 + 1, изменил для большего разброса влево
                    else optimalLaneIndex = lanesPerDirection - 1;
                } else {
                    optimalLaneIndex = 0;
                }
            }
        }
        double bestGain = -Double.MAX_VALUE;
        int bestTargetLane = -1;
        double currentAccelGainThresholdOvertake = BASE_ACCEL_GAIN_THRESHOLD_OVERTAKE;
        double currentAccelGainThresholdRegular = BASE_ACCEL_GAIN_THRESHOLD_REGULAR;
        if (driverType == DriverType.AGGRESSIVE) {
            currentAccelGainThresholdOvertake *= 0.7; currentAccelGainThresholdRegular *= 0.7;
        } else if (driverType == DriverType.CAUTIOUS) {
            currentAccelGainThresholdOvertake *= 1.5; currentAccelGainThresholdRegular *= 1.5;
        }
        if (currentSpeed < desiredSpeed * 0.5 && ownCurrentAcceleration < 0) {
            currentAccelGainThresholdOvertake *= 0.5; currentAccelGainThresholdRegular *= 0.5;
        }
        if (!isCurrentLaneTheLeftmost && leftLanePotentialAccel != null && overallSafetyLeft && !leftChangeHurtsOthersTooMuch) {
            double gainLeft = leftLanePotentialAccel - ownCurrentAcceleration - LANE_CHANGE_MANEUVER_COST;
            if (currentLaneIndex + 1 == optimalLaneIndex) gainLeft += OPTIMAL_LANE_BIAS_ACCEL;
            double effectiveOvertakeThreshold = (driverType == DriverType.AGGRESSIVE && gainLeft > 0) ? currentAccelGainThresholdOvertake * 0.8 : currentAccelGainThresholdOvertake;
            if (gainLeft > effectiveOvertakeThreshold) {
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
            double effectiveRegularThreshold = (driverType == DriverType.CAUTIOUS && gainRight > -0.1) ? currentAccelGainThresholdRegular * 0.5 : currentAccelGainThresholdRegular;
            if (gainRight > effectiveRegularThreshold) {
                if (gainRight > bestGain) {
                    bestGain = gainRight;
                    bestTargetLane = currentLaneIndex - 1;
                }
            }
        }
        if (bestTargetLane != -1) {
            if (bestTargetLane > currentLaneIndex) this.committedToChangeLeft = true;
            else this.committedToChangeRight = true;
            this.committedTargetLane = bestTargetLane;
        }
    }

    public void startLaneChangeIfCommitted(int targetLocalLane) {
        if ((committedToChangeLeft && targetLocalLane > currentLaneIndex) ||
                (committedToChangeRight && targetLocalLane < currentLaneIndex)) {
            if (this.committedTargetLane == targetLocalLane) {
                startLaneChange(targetLocalLane);
            } else {
                resetCommitments();
            }
        }
    }

    private void startLaneChange(int targetLocalLane) {
        if (!isChangingLane && this.currentLaneIndex != targetLocalLane) {
            this.isChangingLane = true;
            this.targetLaneForChange = targetLocalLane;
            this.laneChangeProgress = 0.0;
        }
    }

    public void resetCommitments() {
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
    public double getActualAccelerationParam() { return actualAccelerationParam; }
    public double getActualBaseDecelerationParam() { return actualBaseDecelerationParam; }
    public double getActualSafeTimeHeadway() { return actualSafeTimeHeadway; }
    public DriverType getDriverType() { return driverType; }
    public boolean isBraking() { return isBraking; }

    @Override
    public String toString() {
        return String.format("Car{id=%d, %s, dir=%d, locLane=%d, pos=%.1f, spd=%.1f (ds=%.1f), brake=%b, commitT=%d}",
                id, driverType, direction, currentLaneIndex, position, currentSpeed * 3.6, desiredSpeed*3.6,
                isBraking, committedTargetLane);
    }
}
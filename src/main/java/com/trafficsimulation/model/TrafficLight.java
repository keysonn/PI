package com.trafficsimulation.model;

import java.util.concurrent.atomic.AtomicLong;

public class TrafficLight {
    private static final AtomicLong idCounter = new AtomicLong(0);
    private final long id;
    private double position;
    private double redDuration;
    private double greenDuration;
    private TrafficLightState initialState;
    private TrafficLightState currentState;
    private double remainingTimeInState;
    private int targetDirection;
    private boolean externallyControlled = false;

    public TrafficLight(double position, double redDuration, double greenDuration, TrafficLightState initialState, int targetDirection) {
        this.id = idCounter.incrementAndGet();
        this.position = position;
        this.redDuration = Math.max(20, Math.min(redDuration, 100));
        this.greenDuration = Math.max(20, Math.min(greenDuration, 100));
        this.initialState = initialState;
        this.targetDirection = targetDirection;
        resetToInitialState();
    }

    public void update(double deltaTime) {
        if (externallyControlled) {
            if (remainingTimeInState > 0) {
                remainingTimeInState -= deltaTime;
                if (remainingTimeInState < 0) remainingTimeInState = 0;
            }
            return;
        }

        remainingTimeInState -= deltaTime;
        if (remainingTimeInState <= 0) {
            if (currentState == TrafficLightState.GREEN) {
                currentState = TrafficLightState.RED;
                remainingTimeInState = redDuration + remainingTimeInState;
            } else {
                currentState = TrafficLightState.GREEN;
                remainingTimeInState = greenDuration + remainingTimeInState;
            }
            if (remainingTimeInState < 0) remainingTimeInState = 0;
        }
    }

    public void setCurrentState(TrafficLightState newState, boolean resetTimerForThisState) {
        this.currentState = newState;
        if (resetTimerForThisState) {
            this.remainingTimeInState = (newState == TrafficLightState.GREEN) ? greenDuration : redDuration;
        }
    }

    public void resetTimer(double duration) {
        this.remainingTimeInState = duration;
    }

    public void resetToInitialState() {
        this.currentState = this.initialState;
        if (this.currentState == TrafficLightState.GREEN) {
            this.remainingTimeInState = greenDuration;
        } else {
            this.remainingTimeInState = redDuration;
        }
    }

    public long getId() { return id; }
    public double getPosition() { return position; }
    public TrafficLightState getCurrentState() { return currentState; }
    public double getRemainingTime() { return Math.max(0, remainingTimeInState); }
    public int getTargetDirection() { return targetDirection; }
    public boolean isExternallyControlled() { return externallyControlled; }
    public void setExternallyControlled(boolean externallyControlled) { this.externallyControlled = externallyControlled; }

    @Override
    public String toString() {
        return "TrafficLight{" + id + " pos=" + String.format("%.1f",position) + ", state=" + currentState + ", remT=" + String.format("%.1f",remainingTimeInState) + '}';
    }
}
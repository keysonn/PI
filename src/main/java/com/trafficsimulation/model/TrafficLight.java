package com.trafficsimulation.model;

import java.util.concurrent.atomic.AtomicLong;

public class TrafficLight {
    private static final AtomicLong idCounter = new AtomicLong(0);
    private final long id;
    private double position; // meters
    private double redDuration; // seconds
    private double greenDuration; // seconds
    private TrafficLightState initialState; // Начальное состояние при сбросе
    private TrafficLightState currentState;
    private double remainingTimeInState; // seconds
    private int targetDirection; // 0 for dir0 (L->R on screen bottom), 1 for dir1 (R->L on screen top), -1 for both (tunnel system lights)
    private boolean externallyControlled = false; // true for tunnel lights

    public TrafficLight(double position, double redDuration, double greenDuration, TrafficLightState initialState, int targetDirection) {
        this.id = idCounter.incrementAndGet();
        this.position = position;
        this.redDuration = Math.max(20, Math.min(redDuration, 100)); // Ensure bounds
        this.greenDuration = Math.max(20, Math.min(greenDuration, 100)); // Ensure bounds
        this.initialState = initialState;
        this.targetDirection = targetDirection;
        resetToInitialState(); // Устанавливаем начальное состояние и таймер
    }

    public void update(double deltaTime) {
        if (externallyControlled) {
            // Если управляется извне, просто уменьшаем таймер для отображения,
            // фактическая смена состояния будет через setCurrentState.
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
                remainingTimeInState = redDuration + remainingTimeInState; // Добавляем остаток времени
            } else {
                currentState = TrafficLightState.GREEN;
                remainingTimeInState = greenDuration + remainingTimeInState; // Добавляем остаток времени
            }
            if (remainingTimeInState < 0) remainingTimeInState = 0; // Убедимся, что не отрицательное
        }
    }

    public void setCurrentState(TrafficLightState newState, boolean resetTimerForThisState) {
        this.currentState = newState;
        if (resetTimerForThisState) {
            this.remainingTimeInState = (newState == TrafficLightState.GREEN) ? greenDuration : redDuration;
        }
    }

    // Новый метод для сброса таймера к заданной длительности
    public void resetTimer(double duration) {
        this.remainingTimeInState = duration;
    }

    // Новый метод для сброса светофора в его начальное состояние
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
    public double getRemainingTime() { return Math.max(0, remainingTimeInState); } // Не возвращаем отрицательное время
    public int getTargetDirection() { return targetDirection; }
    public boolean isExternallyControlled() { return externallyControlled; }
    public void setExternallyControlled(boolean externallyControlled) { this.externallyControlled = externallyControlled; }

    @Override
    public String toString() {
        return "TrafficLight{" + id + " pos=" + String.format("%.1f",position) + ", state=" + currentState + ", remT=" + String.format("%.1f",remainingTimeInState) + '}';
    }
}
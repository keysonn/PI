package com.trafficsimulation.model;

public class TrafficLight {

    private static int idCounter = 0;

    private final int id;
    private final double position;
    private final double redDuration;
    private final double greenDuration;

    private TrafficLightState currentState;
    private double timeInCurrentState;

    public TrafficLight(double position, double redDuration, double greenDuration, TrafficLightState initialState) {
        this.id = idCounter++;
        this.position = position;
        this.redDuration = Math.max(20.0, Math.min(redDuration, 100.0));
        this.greenDuration = Math.max(20.0, Math.min(greenDuration, 100.0));

        // Просто присваиваем initialState, так как он может быть только RED или GREEN
        this.currentState = initialState;
        // Если бы мы хотели гарантировать, что initialState не null или не что-то неожиданное,
        // можно было бы добавить:
        // if (initialState == null || (initialState != TrafficLightState.RED && initialState != TrafficLightState.GREEN)) {
        //     this.currentState = TrafficLightState.RED; // Дефолтное значение
        // } else {
        //     this.currentState = initialState;
        // }
        // Но с enum это обычно не требуется, если только initialState не может быть null.

        this.timeInCurrentState = 0.0;
        System.out.println("Создан TrafficLight ID=" + this.id + " на позиции " + this.position + " с нач. состоянием " + this.currentState);
    }

    public void update(double deltaTime) {
        timeInCurrentState += deltaTime;

        switch (currentState) {
            case GREEN:
                if (timeInCurrentState >= greenDuration) {
                    currentState = TrafficLightState.RED;
                    timeInCurrentState = 0;
                }
                break;
            case RED:
                if (timeInCurrentState >= redDuration) {
                    currentState = TrafficLightState.GREEN;
                    timeInCurrentState = 0;
                }
                break;
        }
    }

    public double getRemainingTime() {
        if (currentState == TrafficLightState.GREEN) {
            return Math.max(0, greenDuration - timeInCurrentState);
        } else if (currentState == TrafficLightState.RED) {
            return Math.max(0, redDuration - timeInCurrentState);
        }
        return 0;
    }

    public int getId() { return id; }
    public double getPosition() { return position; }
    public TrafficLightState getCurrentState() { return currentState; }

    @Override
    public String toString() {
        return String.format("Light{id=%d, pos=%.1f, state=%s, timeInState=%.1f, remains=%.1f}",
                id, position, currentState, timeInCurrentState, getRemainingTime());
    }
}
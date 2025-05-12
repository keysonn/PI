package com.trafficsimulation.model;

public class TrafficLight {

    private static int idCounter = 0;

    private final int id;
    private final double position;
    private final double redDuration;
    private final double greenDuration;
    private TrafficLightState currentState;
    private double timeInCurrentState;
    private final int targetDirection; // 0 для направления 0, 1 для направления 1

    public TrafficLight(double position, double redDuration, double greenDuration, int targetDirection) {
        this.id = idCounter++;
        this.position = position;
        this.redDuration = Math.max(20.0, Math.min(redDuration, 100.0));
        this.greenDuration = Math.max(20.0, Math.min(greenDuration, 100.0));
        this.currentState = TrafficLightState.GREEN; // По умолчанию зеленый
        this.targetDirection = targetDirection;
        this.timeInCurrentState = 0.0;
        System.out.println("Создан TrafficLight ID=" + this.id + " на поз. " + String.format("%.1f",this.position) +
                " для напр. " + this.targetDirection + " с сост. " + this.currentState);
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
                if (timeInCurrentState >= greenDuration) { // Ошибка была здесь, должно быть redDuration
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

    public int getTargetDirection() {
        return targetDirection;
    }

    public int getId() { return id; }
    public double getPosition() { return position; }
    public TrafficLightState getCurrentState() { return currentState; }

    @Override
    public String toString() {
        return String.format("Light{id=%d, dir=%d, pos=%.1f, state=%s, remains=%.1f}",
                id, targetDirection, position, currentState, getRemainingTime());
    }
}
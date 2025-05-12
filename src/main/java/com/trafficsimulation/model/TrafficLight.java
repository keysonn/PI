package com.trafficsimulation.model;

public class TrafficLight {

    private static int idCounter = 0;

    private final int id;
    private final double position;
    private final double redDuration;
    private final double greenDuration;
    private TrafficLightState currentState;
    private double timeInCurrentState;
    private final int targetDirection; // 0 для направления 0 (слева-направо), 1 для направления 1 (справа-налево)

    // Конструктор с initialState и targetDirection
    public TrafficLight(double position, double redDuration, double greenDuration,
                        TrafficLightState initialState, int targetDirection) {
        this.id = idCounter++;
        this.position = position;
        this.redDuration = Math.max(20.0, Math.min(redDuration, 100.0)); // ТЗ 1.6-1.7
        this.greenDuration = Math.max(20.0, Math.min(greenDuration, 100.0)); // ТЗ 1.8-1.9

        // Убедимся, что initialState не null и корректен (RED или GREEN)
        if (initialState == null || (initialState != TrafficLightState.RED && initialState != TrafficLightState.GREEN) ) {
            this.currentState = TrafficLightState.RED; // Безопасное значение по умолчанию
        } else {
            this.currentState = initialState;
        }

        this.targetDirection = targetDirection;
        this.timeInCurrentState = 0.0;
        System.out.println("Создан TrafficLight ID=" + this.id + " на поз. " + String.format("%.1f",this.position) +
                " для напр. " + this.targetDirection + " с нач. сост. " + this.currentState);
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
                if (timeInCurrentState >= redDuration) { // Исправлено с greenDuration на redDuration
                    currentState = TrafficLightState.GREEN;
                    timeInCurrentState = 0;
                }
                break;
            // YELLOW удален
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
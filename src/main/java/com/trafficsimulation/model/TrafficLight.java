package com.trafficsimulation.model;

public class TrafficLight {

    private static int idCounter = 0;

    private final int id;
    private final double position;
    private final double redDuration;
    private final double greenDuration;
    private TrafficLightState currentState;
    private double timeInCurrentState;
    private final int targetDirection; // 0 для направления ->, 1 для направления <-, -1 для обоих (если применимо)

    private boolean externallyControlled = false;

    public TrafficLight(double position, double redDuration, double greenDuration,
                        TrafficLightState initialState, int targetDirection) {
        this.id = idCounter++;
        this.position = position;
        // Валидация длительностей фаз из ТЗ (20-100 секунд)
        this.redDuration = Math.max(20.0, Math.min(redDuration, 100.0));
        this.greenDuration = Math.max(20.0, Math.min(greenDuration, 100.0));

        if (initialState == null || (initialState != TrafficLightState.RED && initialState != TrafficLightState.GREEN)) {
            this.currentState = TrafficLightState.RED; // Безопасное начальное состояние
        } else {
            this.currentState = initialState;
        }

        this.targetDirection = targetDirection;
        this.timeInCurrentState = 0.0;
        // System.out.println("Создан TrafficLight ID=" + this.id + " на поз. " + String.format("%.1f",this.position) +
        //                    " для напр. " + this.targetDirection + " с сост. " + this.currentState);
    }

    public void update(double deltaTime) {
        if (externallyControlled) {
            // Если светофор управляется извне, его собственный таймер и логика переключения не работают.
            // Но время в текущем состоянии все равно отслеживается для getRemainingTime.
            timeInCurrentState += deltaTime;
            return;
        }

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

    public void setCurrentState(TrafficLightState newState, boolean resetTimer) {
        if (this.currentState != newState || resetTimer) {
            this.currentState = newState;
            if (resetTimer) {
                this.timeInCurrentState = 0.0;
            }
        }
    }

    public void setExternallyControlled(boolean controlled) {
        this.externallyControlled = controlled;
    }

    // Геттер для флага внешнего управления (ИЗМЕНЕНИЕ: ДОБАВЛЕН МЕТОД)
    public boolean isExternallyControlled() {
        return externallyControlled;
    }

    public double getRemainingTime() {
        if (currentState == TrafficLightState.GREEN) {
            return Math.max(0, greenDuration - timeInCurrentState);
        } else if (currentState == TrafficLightState.RED) {
            return Math.max(0, redDuration - timeInCurrentState);
        }
        return 0; // Не должно произойти при корректных состояниях
    }

    public int getTargetDirection() { return targetDirection; }
    public int getId() { return id; }
    public double getPosition() { return position; }
    public TrafficLightState getCurrentState() { return currentState; }

    @Override
    public String toString() {
        return String.format("Light{id=%d, dir=%d, pos=%.1f, state=%s, extCtrl=%b, timeInState=%.1f, remains=%.1f}",
                id, targetDirection, position, currentState, externallyControlled, timeInCurrentState, getRemainingTime());
    }
}
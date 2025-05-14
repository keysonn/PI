package com.trafficsimulation.model;

public class TrafficLight {

    private static int idCounter = 0;

    private final int id;
    private final double position;
    private final double redDuration;
    private final double greenDuration;
    private TrafficLightState currentState;
    private double timeInCurrentState;
    private final int targetDirection;

    // Флаг, указывающий, управляется ли этот светофор внешним контроллером (например, тоннеля)
    private boolean externallyControlled = false;

    public TrafficLight(double position, double redDuration, double greenDuration,
                        TrafficLightState initialState, int targetDirection) {
        this.id = idCounter++;
        this.position = position;
        this.redDuration = Math.max(20.0, Math.min(redDuration, 100.0));
        this.greenDuration = Math.max(20.0, Math.min(greenDuration, 100.0));

        if (initialState == null || (initialState != TrafficLightState.RED && initialState != TrafficLightState.GREEN) ) {
            this.currentState = TrafficLightState.RED;
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
            // Если светофор управляется извне (например, контроллером тоннеля),
            // его собственный таймер и логика переключения не работают.
            // Время в текущем состоянии все равно может быть полезно отслеживать для getRemainingTime.
            timeInCurrentState += deltaTime;
            // Если timeInCurrentState сбрасывается извне при смене состояния,
            // то getRemainingTime будет работать корректно.
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

    // Сеттер для внешнего управления состоянием (например, контроллером тоннеля)
    public void setCurrentState(TrafficLightState newState, boolean resetTimer) {
        if (this.currentState != newState || resetTimer) {
            this.currentState = newState;
            if (resetTimer) {
                this.timeInCurrentState = 0.0;
            }
        }
    }

    // Флаг для контроллера тоннеля
    public void setExternallyControlled(boolean controlled) {
        this.externallyControlled = controlled;
    }


    public double getRemainingTime() {
        // Этот метод теперь должен корректно работать и для управляемых извне светофоров,
        // если timeInCurrentState правильно обновляется или сбрасывается при смене состояния.
        // Для управляемых извне светофоров, длительность фазы берется из параметров тоннеля,
        // а не из собственных red/greenDuration, если они разные.
        // Пока оставим как есть, предполагая, что red/greenDuration установлены правильно при создании.
        if (currentState == TrafficLightState.GREEN) {
            return Math.max(0, greenDuration - timeInCurrentState);
        } else if (currentState == TrafficLightState.RED) {
            return Math.max(0, redDuration - timeInCurrentState);
        }
        return 0;
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
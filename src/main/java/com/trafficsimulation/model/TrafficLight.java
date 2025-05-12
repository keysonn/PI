package com.trafficsimulation.model;

/**
 * Представляет светофор на определенной позиции дороги.
 */
public class TrafficLight {

    private static int idCounter = 0; // Счетчик для ID светофоров

    private final int id;
    private final double position;          // Позиция на дороге (метры)
    private final double redDuration;       // Длительность красного (секунды)
    private final double greenDuration;     // Длительность зеленого (секунды)
    private final double yellowDuration = 2.0; // Фиксированная длительность желтого

    private TrafficLightState currentState; // Текущий цвет
    private double timeInCurrentState;      // Сколько времени прошло в текущем цвете

    /**
     * Конструктор светофора.
     * @param position Позиция на дороге (метры).
     * @param redDuration Длительность красного (с, будет ограничена 20-100).
     * @param greenDuration Длительность зеленого (с, будет ограничена 20-100).
     * @param initialState Начальное состояние (цвет).
     */
    public TrafficLight(double position, double redDuration, double greenDuration, TrafficLightState initialState) {
        this.id = idCounter++;
        this.position = position;
        // Ограничиваем длительность фаз согласно ТЗ (Приложение, п. 1.6 - 1.9)
        this.redDuration = Math.max(20.0, Math.min(redDuration, 100.0));
        this.greenDuration = Math.max(20.0, Math.min(greenDuration, 100.0));
        this.currentState = initialState;
        this.timeInCurrentState = 0.0; // Начинаем отсчет времени в начальном состоянии
        System.out.println("Создан TrafficLight ID=" + this.id + " на позиции " + this.position);
    }

    /**
     * Обновляет состояние светофора (переключает цвета).
     * @param deltaTime Время, прошедшее с прошлого обновления (секунды).
     */
    public void update(double deltaTime) {
        timeInCurrentState += deltaTime;
        boolean switched = false; // Флаг, что произошло переключение

        // Логика переключения состояний
        switch (currentState) {
            case GREEN:
                if (timeInCurrentState >= greenDuration) {
                    currentState = TrafficLightState.YELLOW;
                    timeInCurrentState = 0; // Сброс таймера для нового состояния
                    switched = true;
                }
                break;
            case YELLOW:
                if (timeInCurrentState >= yellowDuration) {
                    currentState = TrafficLightState.RED;
                    timeInCurrentState = 0;
                    switched = true;
                }
                break;
            case RED:
                if (timeInCurrentState >= redDuration) {
                    currentState = TrafficLightState.GREEN;
                    timeInCurrentState = 0;
                    switched = true;
                }
                break;
        }
        //if (switched) { System.out.println("Light " + id + " switched to " + currentState); } // Для отладки
    }

    // --- Геттеры ---
    public int getId() { return id; }
    public double getPosition() { return position; }
    public TrafficLightState getCurrentState() { return currentState; }

    @Override
    public String toString() {
        return String.format("Light{id=%d, pos=%.1f, state=%s, time=%.1f}",
                id, position, currentState, timeInCurrentState);
    }
}
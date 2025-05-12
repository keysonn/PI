package com.trafficsimulation.model;

import java.util.concurrent.atomic.AtomicLong; // Для генерации уникальных ID машин

/**
 * Представляет один автомобиль в симуляции.
 */
public class Car {

    // Статический генератор ID, чтобы у каждой машины был уникальный номер
    private static final AtomicLong idCounter = new AtomicLong(0);

    private final long id;           // Уникальный ID этой машины
    private double position;         // Положение на дороге (в метрах от начала участка)
    private double currentSpeed;     // Текущая скорость (важно выбрать единицы, например, м/с)
    private double targetSpeed;      // Желаемая скорость (определяется водителем, знаками, светофорами)
    private double maxSpeed;         // Максимальная техническая скорость машины
    private double acceleration;     // Ускорение (м/с^2)
    private double deceleration;     // Замедление/торможение (положительное значение, м/с^2)
    private int laneIndex;           // Номер полосы (0, 1, 2...)

    /**
     * Конструктор для создания нового автомобиля.
     * @param initialPosition Начальное положение на дороге (метры).
     * @param initialSpeed Начальная скорость (м/с).
     * @param maxSpeed Максимальная скорость (м/с).
     * @param acceleration Ускорение (м/с^2).
     * @param deceleration Замедление (м/с^2).
     * @param laneIndex Номер полосы.
     */
    public Car(double initialPosition, double initialSpeed, double maxSpeed, double acceleration, double deceleration, int laneIndex) {
        this.id = idCounter.incrementAndGet(); // Генерируем и присваиваем ID
        this.position = initialPosition;
        this.currentSpeed = initialSpeed;
        this.maxSpeed = maxSpeed;
        this.targetSpeed = initialSpeed; // Начнем с текущей, потом разберемся
        this.acceleration = acceleration;
        this.deceleration = deceleration;
        this.laneIndex = laneIndex;
        System.out.println("Создан Car ID=" + this.id + " на полосе " + this.laneIndex);
    }

    /**
     * Основной метод обновления состояния машины за промежуток времени.
     * @param deltaTime Прошедшее время в секундах.
     * @param leadCar Машина впереди (или null, если нет).
     * @param distanceToLead Расстояние до машины впереди.
     * @param effectiveSpeedLimit Действующее ограничение скорости (от знаков).
     * @param nextLightState Состояние следующего светофора (или null).
     * @param distanceToLight Расстояние до следующего светофора.
     */
    public void update(double deltaTime, Car leadCar, double distanceToLead, double effectiveSpeedLimit, TrafficLightState nextLightState, double distanceToLight) {
        // !!! Логика движения будет сложной и добавится позже !!!
        // Пока реализуем простейшее движение с постоянной скоростью

        // Простое перемещение: новая позиция = старая + скорость * время
        this.position += this.currentSpeed * deltaTime;
    }

    // --- Геттеры (методы для получения данных о машине) ---
    public long getId() { return id; }
    public double getPosition() { return position; }
    public double getCurrentSpeed() { return currentSpeed; }
    public int getLaneIndex() { return laneIndex; }
    public double getMaxSpeed() { return maxSpeed; }

    // --- Сеттеры (методы для изменения данных, если нужно извне) ---
    // Обычно сеттеры нужны реже, стараемся менять состояние через метод update()
    public void setTargetSpeed(double targetSpeed) {
        this.targetSpeed = Math.max(0, Math.min(targetSpeed, this.maxSpeed));
    }


    @Override
    public String toString() {
        // Для удобного вывода в консоль при отладке
        return String.format("Car{id=%d, pos=%.1f, spd=%.1f, lane=%d}",
                id, position, currentSpeed * 3.6, laneIndex); // Выводим скорость в км/ч
    }
}
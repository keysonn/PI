package com.trafficsimulation.model;

import java.util.concurrent.atomic.AtomicLong;

public class Car {

    private static final AtomicLong idCounter = new AtomicLong(0);

    private final long id;
    private double position;
    private double currentSpeed;
    private double targetSpeed;
    private double maxSpeed;
    private double acceleration;
    private double deceleration;
    private int laneIndex;       // Общий индекс полосы от 0 до (totalLanes - 1)
    private final int direction; // Направление движения (например, 0 для слева-направо, 1 для справа-налево)

    /**
     * Конструктор для создания нового автомобиля.
     * @param initialPosition Начальное положение на дороге (метры).
     * @param initialSpeed Начальная скорость (м/с). Должна быть > 0.
     * @param maxSpeed Максимальная скорость (м/с).
     * @param acceleration Ускорение (м/с^2).
     * @param deceleration Замедление (м/s^2).
     * @param laneIndex Номер полосы.
     * @param direction Направление движения (0 или 1).
     */
    public Car(double initialPosition, double initialSpeed, double maxSpeed, double acceleration, double deceleration, int laneIndex, int direction) {
        this.id = idCounter.incrementAndGet();
        this.position = initialPosition;
        this.currentSpeed = Math.abs(initialSpeed); // Скорость всегда положительна по модулю
        this.maxSpeed = Math.abs(maxSpeed);
        this.targetSpeed = this.currentSpeed;
        this.acceleration = acceleration;
        this.deceleration = deceleration;
        this.laneIndex = laneIndex;
        this.direction = direction; // 0 - слева направо, 1 - справа налево
        System.out.println("Создан Car ID=" + this.id + " на полосе " + this.laneIndex + " в направлении " + this.direction);
    }

    public void update(double deltaTime, Car leadCar, double distanceToLead, double effectiveSpeedLimit, TrafficLightState nextLightState, double distanceToLight) {
        // Логика ускорения/торможения (очень упрощенная)
        double desiredSpeed = Math.min(this.maxSpeed, effectiveSpeedLimit);

        // Реакция на светофор (простейшая)
        if (nextLightState == TrafficLightState.RED || nextLightState == TrafficLightState.YELLOW) {
            if (distanceToLight < 50 && distanceToLight > 0) { // Примерное расстояние для начала торможения
                // Плавное торможение до светофора (очень грубо)
                double requiredDeceleration = (this.currentSpeed * this.currentSpeed) / (2 * Math.max(1.0, distanceToLight));
                if (requiredDeceleration > this.deceleration / 2) { // Если нужно сильно тормозить
                    desiredSpeed = 0;
                } else if (distanceToLight < 10) {
                    desiredSpeed = 0;
                }
            }
        }

        // Реакция на машину впереди (простейшая)
        if (leadCar != null && distanceToLead < 30 && distanceToLead > 0) { // 30м - безопасная дистанция
            desiredSpeed = Math.min(desiredSpeed, leadCar.getCurrentSpeed() * 0.95); // Ехать чуть медленнее или с той же скоростью
            if (distanceToLead < 10) { // Критическое сближение
                desiredSpeed = Math.min(desiredSpeed, leadCar.getCurrentSpeed() * 0.8); // Сильнее сбросить скорость
                if(leadCar.getCurrentSpeed() < 1.0) desiredSpeed = 0; // Если передний стоит, мы тоже останавливаемся
            }
        }


        if (this.currentSpeed < desiredSpeed) {
            this.currentSpeed += this.acceleration * deltaTime;
            this.currentSpeed = Math.min(this.currentSpeed, desiredSpeed);
        } else if (this.currentSpeed > desiredSpeed) {
            this.currentSpeed -= this.deceleration * deltaTime;
            this.currentSpeed = Math.max(this.currentSpeed, desiredSpeed); // Не тормозить ниже желаемой (если она не 0)
            if (desiredSpeed == 0) this.currentSpeed = Math.max(0, this.currentSpeed); // Убедиться, что не ушли в минус
        }
        this.currentSpeed = Math.max(0, this.currentSpeed); // Скорость не может быть отрицательной

        // Обновление позиции
        if (this.direction == 0) { // Слева направо
            this.position += this.currentSpeed * deltaTime;
        } else { // Справа налево
            this.position -= this.currentSpeed * deltaTime; // Двигаемся в сторону уменьшения координаты
        }
    }

    public long getId() { return id; }
    public double getPosition() { return position; }
    public double getCurrentSpeed() { return currentSpeed; }
    public int getLaneIndex() { return laneIndex; }
    public double getMaxSpeed() { return maxSpeed; }
    public int getDirection() { return direction; } // Геттер для направления

    public void setTargetSpeed(double targetSpeed) {
        this.targetSpeed = Math.max(0, Math.min(targetSpeed, this.maxSpeed));
    }

    @Override
    public String toString() {
        return String.format("Car{id=%d, dir=%d, pos=%.1f, spd=%.1f, lane=%d}",
                id, direction, position, currentSpeed * 3.6, laneIndex);
    }
}
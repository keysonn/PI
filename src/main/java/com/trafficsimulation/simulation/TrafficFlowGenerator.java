package com.trafficsimulation.simulation;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road; // Нужен для определения полосы

import java.util.Random; // Для случайных потоков

/**
 * Отвечает за генерацию новых автомобилей для симуляции
 * в соответствии с заданными параметрами потока.
 */
public class TrafficFlowGenerator {

    private final SimulationParameters params; // Параметры для генерации
    private final Road road;                   // Дорога, на которую добавляем машины
    private final Random random;               // Генератор случайных чисел

    private double timeSinceLastCar = 0.0;     // Счетчик времени с последней машины (для детерминированного потока)

    /**
     * Конструктор генератора.
     * @param params Параметры симуляции.
     * @param road Дорога, для которой генерируется поток.
     */
    public TrafficFlowGenerator(SimulationParameters params, Road road) {
        this.params = params;
        this.road = road;
        this.random = new Random(); // Инициализируем генератор случайных чисел
        System.out.println("TrafficFlowGenerator создан.");
    }

    /**
     * Пытается сгенерировать новую машину на основе прошедшего времени.
     * @param deltaTime Время, прошедшее с последнего вызова (секунды).
     * @return Новый объект Car, если пора его создавать, иначе null.
     */
    public Car generateCar(double deltaTime) {
        if (params.isRandomFlow()) {
            // --- Логика для СЛУЧАЙНОГО потока ---
            // TODO: Реализовать генерацию по законам распределения
            // Вероятность появления машины за шаг deltaTime может зависеть от интенсивности
            // Например, для Пуассоновского потока с интенсивностью lambda (машин/сек):
            // double probability = lambda * deltaTime;
            // if (random.nextDouble() < probability) {
            //     return createNewCar();
            // }
            return null; // Пока не реализовано
        } else {
            // --- Логика для ДЕТЕРМИНИРОВАННОГО потока ---
            timeSinceLastCar += deltaTime;
            double requiredInterval = params.getDeterministicIntervalSeconds();
            if (requiredInterval <= 0) { // Защита от деления на ноль или бесконечного потока
                System.err.println("Интервал детерминированного потока должен быть > 0");
                return null;
            }
            if (timeSinceLastCar >= requiredInterval) {
                timeSinceLastCar -= requiredInterval; // Вычитаем интервал (или обнуляем, если хотим точнее)
                return createNewCar();
            } else {
                return null; // Еще не время
            }
        }
    }

    /**
     * Вспомогательный метод для создания объекта Car с параметрами по умолчанию
     * или на основе настроек.
     * @return Новый объект Car.
     */
    private Car createNewCar() {
        // Определяем параметры для новой машины
        double initialPosition = 0.0; // Машина появляется в начале дороги
        int laneIndex = random.nextInt(road.getNumberOfLanes()); // Случайная полоса (упрощенно!)
        // TODO: Для двух направлений нужно будет выбирать полосу более аккуратно

        double initialSpeed;
        double maxSpeed;
        double acceleration = 1.5 + random.nextDouble() * 1.0; // Пример: Ускорение 1.5-2.5 м/с^2
        double deceleration = 3.0 + random.nextDouble() * 1.5; // Пример: Торможение 3.0-4.5 м/с^2

        if (params.isRandomFlow()) {
            // TODO: Генерировать скорость и maxSpeed случайно по закону
            initialSpeed = (40 + random.nextDouble() * 20) / 3.6; // Пример: 40-60 км/ч -> м/с
            maxSpeed = (80 + random.nextDouble() * 40) / 3.6;     // Пример: 80-120 км/ч -> м/с
        } else {
            // Детерминированный поток
            initialSpeed = params.getDeterministicSpeedKmh() / 3.6; // Заданная скорость (переводим в м/с)
            maxSpeed = initialSpeed * (1.2 + random.nextDouble() * 0.3); // Макс. скорость чуть выше начальной
        }

        // Создаем машину
        return new Car(initialPosition, initialSpeed, maxSpeed, acceleration, deceleration, laneIndex);
    }

    // Метод для обновления параметров, если они изменятся в GUI
    public void updateParameters(SimulationParameters newParams) {
        // TODO: Обновить внутренние параметры генератора, если это необходимо
        // Например, если используются предрасчитанные значения
        System.out.println("Параметры генератора потока обновлены (пока не реализовано)");
    }
}
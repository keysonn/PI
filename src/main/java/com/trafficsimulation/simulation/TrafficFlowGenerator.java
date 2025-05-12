package com.trafficsimulation.simulation;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road; // Нужен для определения полосы

import java.util.Random;

public class TrafficFlowGenerator {

    private final SimulationParameters params;
    private final Road road; // Дорога, на которую добавляем машины
    private final Random random;

    private double timeToNextRandomCarDir0 = 0.0; // Счетчик для направления 0 (слева направо)
    private double timeToNextRandomCarDir1 = 0.0; // Счетчик для направления 1 (справа налево)
    private double timeSinceLastDeterministicCar = 0.0; // Общий для детерминированного, если он будет двунаправленным

    public TrafficFlowGenerator(SimulationParameters params, Road road) {
        this.params = params;
        this.road = road;
        this.random = new Random(System.currentTimeMillis());
        if (params.isRandomFlow()) {
            this.timeToNextRandomCarDir0 = generateNextRandomInterval();
            if (params.getNumberOfDirections() == 2) {
                this.timeToNextRandomCarDir1 = generateNextRandomInterval(); // Отдельный интервал для второго направления
            }
        }
        System.out.println("TrafficFlowGenerator создан.");
    }

    // Изменим generateCar, чтобы он возвращал массив машин (или null)
    // т.к. за один шаг может сгенерироваться машина для каждого направления
    public Car[] generateCars(double deltaTime) { // Возвращает массив, может быть пустым или с 1-2 машинами
        Car carDir0 = null;
        Car carDir1 = null;

        if (params.isRandomFlow()) {
            timeToNextRandomCarDir0 -= deltaTime;
            if (timeToNextRandomCarDir0 <= 0) {
                carDir0 = createNewCarWithRandomParams(0); // Направление 0
                timeToNextRandomCarDir0 = generateNextRandomInterval() + timeToNextRandomCarDir0;
            }

            if (params.getNumberOfDirections() == 2) {
                timeToNextRandomCarDir1 -= deltaTime;
                if (timeToNextRandomCarDir1 <= 0) {
                    carDir1 = createNewCarWithRandomParams(1); // Направление 1
                    timeToNextRandomCarDir1 = generateNextRandomInterval() + timeToNextRandomCarDir1;
                }
            }
        } else { // Детерминированный поток
            timeSinceLastDeterministicCar += deltaTime;
            double requiredInterval = params.getDeterministicIntervalSeconds();
            if (requiredInterval > 0 && timeSinceLastDeterministicCar >= requiredInterval) {
                timeSinceLastDeterministicCar -= requiredInterval;
                carDir0 = createNewCarDeterministicParams(0); // Генерируем для первого направления
                if (params.getNumberOfDirections() == 2) {
                    // Для детерминированного потока в двух направлениях можно генерировать одновременно
                    // или со сдвигом. Пока генерируем одновременно на разных полосах.
                    carDir1 = createNewCarDeterministicParams(1);
                }
            }
        }

        if (carDir0 != null && carDir1 != null) return new Car[]{carDir0, carDir1};
        if (carDir0 != null) return new Car[]{carDir0};
        if (carDir1 != null) return new Car[]{carDir1};
        return null;
    }


    private double generateNextRandomInterval() {
        // ... (код этого метода не меняется) ...
        switch (params.getTimeDistributionLaw()) {
            case UNIFORM:
                return params.getTimeUniformMinSec() +
                        (params.getTimeUniformMaxSec() - params.getTimeUniformMinSec()) * random.nextDouble();
            case NORMAL:
                double interval = params.getTimeNormalMeanSec() +
                        Math.sqrt(params.getTimeNormalVarianceSec()) * random.nextGaussian();
                return Math.max(0.1, interval);
            case EXPONENTIAL:
                if (params.getTimeExponentialIntensityPerSec() <= 0) return Double.POSITIVE_INFINITY;
                double meanInterval = 1.0 / params.getTimeExponentialIntensityPerSec();
                return -meanInterval * Math.log(1.0 - random.nextDouble());
            default:
                System.err.println("Неизвестный закон распределения времени: " + params.getTimeDistributionLaw());
                return 10.0; // Fallback
        }
    }

    private double generateRandomInitialSpeedKmh() {
        // ... (код этого метода не меняется) ...
        double speedKmh;
        switch (params.getSpeedDistributionLaw()) {
            case UNIFORM:
                speedKmh = params.getSpeedUniformMinKmh() +
                        (params.getSpeedUniformMaxKmh() - params.getSpeedUniformMinKmh()) * random.nextDouble();
                break;
            case NORMAL:
                speedKmh = params.getSpeedNormalMeanKmh() +
                        Math.sqrt(params.getSpeedNormalVarianceKmh()) * random.nextGaussian();
                break;
            case EXPONENTIAL:
                if (params.getSpeedExponentialIntensityPerKmh() <= 0) return params.getDeterministicSpeedKmh();
                double meanSpeedForExp = 1.0 / params.getSpeedExponentialIntensityPerKmh();
                speedKmh = -meanSpeedForExp * Math.log(1.0 - random.nextDouble());
                speedKmh = Math.max(10.0, Math.min(speedKmh, 150.0));
                break;
            default:
                System.err.println("Неизвестный закон распределения скорости: " + params.getSpeedDistributionLaw());
                speedKmh = params.getDeterministicSpeedKmh();
                break;
        }
        return Math.max(10.0, Math.min(speedKmh, 150.0));
    }

    private Car createNewCarWithRandomParams(int direction) {
        double initialPosition;
        int baseLane; // Начальная полоса в рамках своего направления (0, 1, ...)
        int totalLanesInDirection = params.getLanesPerDirection();

        if (direction == 0) { // Слева направо
            initialPosition = 0.0;
            baseLane = random.nextInt(totalLanesInDirection); // Полоса 0, 1, ... (lanesPerDirection - 1)
        } else { // Справа налево
            initialPosition = road.getLength(); // Начинает с конца дороги
            baseLane = totalLanesInDirection + random.nextInt(totalLanesInDirection); // Полоса lanesPerDirection, ...
        }

        double initialSpeedKmh = generateRandomInitialSpeedKmh();
        double initialSpeedMs = initialSpeedKmh / 3.6;
        double maxSpeedMs = initialSpeedMs * (1.2 + random.nextDouble() * 0.3);
        maxSpeedMs = Math.max(initialSpeedMs, maxSpeedMs);
        double acceleration = 1.5 + random.nextDouble() * 1.5;
        double deceleration = 3.0 + random.nextDouble() * 2.0;

        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, acceleration, deceleration, baseLane, direction);
    }

    private Car createNewCarDeterministicParams(int direction) {
        double initialPosition;
        int baseLane;
        int totalLanesInDirection = params.getLanesPerDirection();

        if (direction == 0) {
            initialPosition = 0.0;
            baseLane = random.nextInt(totalLanesInDirection);
        } else {
            initialPosition = road.getLength();
            baseLane = totalLanesInDirection + random.nextInt(totalLanesInDirection);
        }

        double initialSpeedMs = params.getDeterministicSpeedKmh() / 3.6;
        double maxSpeedMs = initialSpeedMs * (1.2 + random.nextDouble() * 0.3);
        maxSpeedMs = Math.max(initialSpeedMs, maxSpeedMs);
        double acceleration = 1.5 + random.nextDouble() * 1.5;
        double deceleration = 3.0 + random.nextDouble() * 2.0;

        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, acceleration, deceleration, baseLane, direction);
    }

    public void updateParameters(SimulationParameters newParams) {
        // this.params = newParams; // Параметры обновляются через SimulationEngine
        System.out.println("TrafficFlowGenerator.updateParameters вызван (сброс/инициализация счетчиков).");
        timeSinceLastDeterministicCar = 0.0;
        // При смене параметров или типа потока, нужно корректно инициализировать счетчики времени до случайной машины
        if (params.isRandomFlow()) {
            this.timeToNextRandomCarDir0 = generateNextRandomInterval();
            if (params.getNumberOfDirections() == 2) {
                this.timeToNextRandomCarDir1 = generateNextRandomInterval();
            } else {
                this.timeToNextRandomCarDir1 = Double.POSITIVE_INFINITY; // Не генерировать для второго направления
            }
        } else {
            this.timeToNextRandomCarDir0 = Double.POSITIVE_INFINITY;
            this.timeToNextRandomCarDir1 = Double.POSITIVE_INFINITY;
        }
    }
}
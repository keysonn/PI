package com.trafficsimulation.simulation;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;

import java.util.List; // Для получения списка машин
import java.util.Random;

public class TrafficFlowGenerator {

    private final SimulationParameters params;
    private final Road road;
    private final Random random;

    private double timeToNextRandomCarDir0 = 0.0;
    private double timeToNextRandomCarDir1 = 0.0;
    private double timeSinceLastDeterministicCar = 0.0;

    // Минимальный ЧИСТЫЙ зазор до ближайшей машины в точке спавна, чтобы новая машина появилась
    private static final double MIN_SPAWN_CLEARANCE_M = 7.0; // метры (длина машины + немного буфера)
    // Максимальное количество попыток найти свободную полосу (для детерминированного потока)
    private static final int MAX_SPAWN_LANE_ATTEMPTS = 3;


    public TrafficFlowGenerator(SimulationParameters params, Road road) {
        this.params = params;
        this.road = road;
        this.random = new Random(System.currentTimeMillis());
        resetGenerationTimers(); // Инициализируем таймеры
        System.out.println("TrafficFlowGenerator создан.");
    }

    private void resetGenerationTimers() {
        timeSinceLastDeterministicCar = 0.0;
        if (params.isRandomFlow()) {
            this.timeToNextRandomCarDir0 = generateNextRandomInterval();
            if (params.getNumberOfDirections() == 2) {
                this.timeToNextRandomCarDir1 = generateNextRandomInterval();
            } else {
                this.timeToNextRandomCarDir1 = Double.POSITIVE_INFINITY;
            }
        } else {
            this.timeToNextRandomCarDir0 = Double.POSITIVE_INFINITY;
            this.timeToNextRandomCarDir1 = Double.POSITIVE_INFINITY;
        }
    }


    public Car[] generateCars(double deltaTime) {
        Car carDir0 = null;
        Car carDir1 = null;
        List<Car> existingCars = road.getCars();

        if (params.isRandomFlow()) {
            timeToNextRandomCarDir0 -= deltaTime;
            if (timeToNextRandomCarDir0 <= 0) {
                int targetLaneDir0 = random.nextInt(params.getLanesPerDirection());
                if (isSpawnPointClear(0, targetLaneDir0, existingCars)) {
                    carDir0 = createNewCarWithRandomParams(0, targetLaneDir0);
                    timeToNextRandomCarDir0 = generateNextRandomInterval() + timeToNextRandomCarDir0; // Добавляем остаток
                } else {
                    timeToNextRandomCarDir0 = Math.max(0.2, timeToNextRandomCarDir0 + 0.2); // Отложить немного
                }
            }

            if (params.getNumberOfDirections() == 2) {
                timeToNextRandomCarDir1 -= deltaTime;
                if (timeToNextRandomCarDir1 <= 0) {
                    int targetLaneDir1 = params.getLanesPerDirection() + random.nextInt(params.getLanesPerDirection());
                    if (isSpawnPointClear(1, targetLaneDir1, existingCars)) {
                        carDir1 = createNewCarWithRandomParams(1, targetLaneDir1);
                        timeToNextRandomCarDir1 = generateNextRandomInterval() + timeToNextRandomCarDir1;
                    } else {
                        timeToNextRandomCarDir1 = Math.max(0.2, timeToNextRandomCarDir1 + 0.2);
                    }
                }
            }
        } else { // Детерминированный поток
            timeSinceLastDeterministicCar += deltaTime;
            double requiredInterval = params.getDeterministicIntervalSeconds();
            if (requiredInterval > 0 && timeSinceLastDeterministicCar >= requiredInterval) {
                boolean carCreated = false;
                // Пытаемся сгенерировать для направления 0
                for (int attempt = 0; attempt < MAX_SPAWN_LANE_ATTEMPTS; attempt++) {
                    int targetLaneDir0Det = random.nextInt(params.getLanesPerDirection());
                    if (isSpawnPointClear(0, targetLaneDir0Det, existingCars)) {
                        carDir0 = createNewCarDeterministicParams(0, targetLaneDir0Det);
                        carCreated = true;
                        break;
                    }
                }

                if (params.getNumberOfDirections() == 2) {
                    // Пытаемся сгенерировать для направления 1
                    for (int attempt = 0; attempt < MAX_SPAWN_LANE_ATTEMPTS; attempt++) {
                        int targetLaneDir1Det = params.getLanesPerDirection() + random.nextInt(params.getLanesPerDirection());
                        if (isSpawnPointClear(1, targetLaneDir1Det, existingCars)) {
                            carDir1 = createNewCarDeterministicParams(1, targetLaneDir1Det);
                            carCreated = true; // Учитываем, если хотя бы одна создана
                            break;
                        }
                    }
                }
                if (carCreated) { // Сбрасываем таймер, если хотя бы одна машина была успешно создана
                    timeSinceLastDeterministicCar -= requiredInterval;
                } else {
                    // Если не смогли создать ни одной машины (все полосы заняты),
                    // можно либо отложить (увеличив timeSinceLastDeterministicCar меньше чем на requiredInterval),
                    // либо просто ждать следующего интервала. Пока ждем следующего.
                    // Чтобы избежать слишком частого срабатывания, если интервал маленький,
                    // можно добавить небольшую задержку к timeSinceLastDeterministicCar, если не создали.
                    // timeSinceLastDeterministicCar = requiredInterval - 0.1; // Например, попробовать снова чуть позже
                }
            }
        }

        if (carDir0 != null && carDir1 != null) return new Car[]{carDir0, carDir1};
        if (carDir0 != null) return new Car[]{carDir0};
        if (carDir1 != null) return new Car[]{carDir1};
        return null;
    }

    private boolean isSpawnPointClear(int direction, int targetLane, List<Car> existingCars) {
        if (existingCars == null || road == null) return true;

        double spawnPosition = (direction == 0) ? 0.0 : road.getLength();
        double checkZoneStart, checkZoneEnd;

        // Определяем небольшую зону вокруг точки спавна для проверки
        if (direction == 0) { // Машина появляется на 0 и едет вправо
            checkZoneStart = spawnPosition - MIN_SPAWN_CLEARANCE_M / 2; // Немного "за" точкой спавна (не должно быть)
            checkZoneEnd = spawnPosition + MIN_SPAWN_CLEARANCE_M;     // Зона перед машиной
        } else { // Машина появляется на road.getLength() и едет влево
            checkZoneStart = spawnPosition - MIN_SPAWN_CLEARANCE_M;     // Зона перед машиной (левее точки спавна)
            checkZoneEnd = spawnPosition + MIN_SPAWN_CLEARANCE_M / 2; // Немного "за" точкой спавна
        }

        for (Car car : existingCars) {
            if (car.getLaneIndex() == targetLane && car.getDirection() == direction) {
                // Проверяем, не находится ли какая-либо часть существующей машины в зоне спавна
                // (упрощенно: если ее позиция (передний бампер) попадает в расширенную зону)
                // Это грубая проверка, т.к. не учитывает длину машины car.
                // Более точная: (car.getPosition() - CAR_LENGTH) < checkZoneEnd && car.getPosition() > checkZoneStart
                // Пока используем только car.getPosition()
                if (car.getPosition() >= checkZoneStart && car.getPosition() <= checkZoneEnd) {
                    // Более точная проверка для направления 0: не должно быть машины близко к 0
                    if (direction == 0 && car.getPosition() < MIN_SPAWN_CLEARANCE_M) return false;
                    // Более точная проверка для направления 1: не должно быть машины близко к road.getLength()
                    if (direction == 1 && (road.getLength() - car.getPosition()) < MIN_SPAWN_CLEARANCE_M) return false;
                }
            }
        }
        return true;
    }

    private Car createNewCarWithRandomParams(int direction, int targetLaneIndex) {
        double initialPosition = (direction == 0) ? 0.0 : road.getLength();
        double initialSpeedKmh = generateRandomInitialSpeedKmh();
        double initialSpeedMs = initialSpeedKmh / 3.6;
        double maxSpeedMs = initialSpeedMs * (1.2 + random.nextDouble() * 0.3);
        maxSpeedMs = Math.max(initialSpeedMs, Math.min(maxSpeedMs, 150.0/3.6));
        double accelP = 1.8 + random.nextDouble() * 1.2;
        double decelP = 2.5 + random.nextDouble() * 1.5;
        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, accelP, decelP, targetLaneIndex, direction);
    }

    private Car createNewCarDeterministicParams(int direction, int targetLaneIndex) {
        double initialPosition = (direction == 0) ? 0.0 : road.getLength();
        double initialSpeedMs = params.getDeterministicSpeedKmh() / 3.6;
        double maxSpeedMs = initialSpeedMs * (1.2 + random.nextDouble() * 0.3);
        maxSpeedMs = Math.max(initialSpeedMs, Math.min(maxSpeedMs, 150.0/3.6));
        double accelP = 1.8 + random.nextDouble() * 1.2;
        double decelP = 2.5 + random.nextDouble() * 1.5;
        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, accelP, decelP, targetLaneIndex, direction);
    }

    private double generateNextRandomInterval() {
        switch (params.getTimeDistributionLaw()) {
            case UNIFORM:
                return params.getTimeUniformMinSec() + (params.getTimeUniformMaxSec() - params.getTimeUniformMinSec()) * random.nextDouble();
            case NORMAL:
                double interval = params.getTimeNormalMeanSec() + Math.sqrt(params.getTimeNormalVarianceSec()) * random.nextGaussian();
                return Math.max(0.2, interval); // Минимальный интервал чуть больше
            case EXPONENTIAL:
                if (params.getTimeExponentialIntensityPerSec() <= 0) return Double.POSITIVE_INFINITY;
                double meanInterval = 1.0 / params.getTimeExponentialIntensityPerSec();
                return Math.max(0.2, -meanInterval * Math.log(1.0 - random.nextDouble())); // Минимальный интервал
            default:
                return 10.0;
        }
    }

    private double generateRandomInitialSpeedKmh() {
        double speedKmh;
        switch (params.getSpeedDistributionLaw()) {
            case UNIFORM:
                speedKmh = params.getSpeedUniformMinKmh() + (params.getSpeedUniformMaxKmh() - params.getSpeedUniformMinKmh()) * random.nextDouble();
                break;
            case NORMAL:
                speedKmh = params.getSpeedNormalMeanKmh() + Math.sqrt(params.getSpeedNormalVarianceKmh()) * random.nextGaussian();
                break;
            case EXPONENTIAL:
                if (params.getSpeedExponentialIntensityPerKmh() <= 0) return params.getDeterministicSpeedKmh();
                double meanSpeedForExp = 1.0 / params.getSpeedExponentialIntensityPerKmh();
                speedKmh = -meanSpeedForExp * Math.log(1.0 - random.nextDouble());
                speedKmh = Math.max(10.0, Math.min(speedKmh, 150.0));
                break;
            default:
                speedKmh = params.getDeterministicSpeedKmh();
                break;
        }
        return Math.max(10.0, Math.min(speedKmh, 150.0));
    }

    public void updateParameters(SimulationParameters newParams) {
        // this.params = newParams; // Не присваиваем напрямую, т.к. params - это ссылка на объект из SimulationEngine
        System.out.println("TrafficFlowGenerator.updateParameters -> сброс таймеров генерации.");
        resetGenerationTimers();
    }
}
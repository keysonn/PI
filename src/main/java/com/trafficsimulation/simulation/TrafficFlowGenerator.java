package com.trafficsimulation.simulation;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;

import java.util.List;
import java.util.Random;

public class TrafficFlowGenerator {

    private final SimulationParameters params;
    private final Road road;
    private final Random random;

    private double timeToNextRandomCarDir0 = 0.0;
    private double timeToNextRandomCarDir1 = 0.0;
    private double timeSinceLastDeterministicCar = 0.0;

    private static final double MIN_SPAWN_CLEARANCE_M = 7.0;
    private static final int MAX_SPAWN_LANE_ATTEMPTS = 3;

    // Флаг для включения/выключения детального логгирования генерации
    private static final boolean DETAILED_LOGGING = false;


    public TrafficFlowGenerator(SimulationParameters params, Road road) {
        this.params = params;
        this.road = road;
        this.random = new Random(System.currentTimeMillis());
        resetGenerationTimers();
        if (DETAILED_LOGGING) {
            System.out.println("TrafficFlowGenerator создан. Начальные таймеры сброшены.");
            System.out.println("  Dir0 next car in: " + String.format("%.2f", timeToNextRandomCarDir0));
            System.out.println("  Dir1 next car in: " + String.format("%.2f", timeToNextRandomCarDir1));
        }
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
                    double nextInterval = generateNextRandomInterval();
                    timeToNextRandomCarDir0 = nextInterval + timeToNextRandomCarDir0; // Добавляем остаток
                    if (DETAILED_LOGGING && carDir0 != null) System.out.println("Random Car Dir0 Spawned. Next in: " + String.format("%.2f",timeToNextRandomCarDir0) + "s (interval: " + String.format("%.2f",nextInterval) + "s)");
                } else {
                    timeToNextRandomCarDir0 = Math.max(0.2, timeToNextRandomCarDir0 + 0.2);
                    if (DETAILED_LOGGING) System.out.println("Spawn Dir0 clear check failed. Deferring. Next try in: " + String.format("%.2f",timeToNextRandomCarDir0) + "s");
                }
            }

            if (params.getNumberOfDirections() == 2) {
                timeToNextRandomCarDir1 -= deltaTime;
                if (timeToNextRandomCarDir1 <= 0) {
                    int targetLaneDir1 = params.getLanesPerDirection() + random.nextInt(params.getLanesPerDirection());
                    if (isSpawnPointClear(1, targetLaneDir1, existingCars)) {
                        carDir1 = createNewCarWithRandomParams(1, targetLaneDir1);
                        double nextInterval = generateNextRandomInterval();
                        timeToNextRandomCarDir1 = nextInterval + timeToNextRandomCarDir1;
                        if (DETAILED_LOGGING && carDir1 != null) System.out.println("Random Car Dir1 Spawned. Next in: " + String.format("%.2f",timeToNextRandomCarDir1) + "s (interval: " + String.format("%.2f",nextInterval) + "s)");
                    } else {
                        timeToNextRandomCarDir1 = Math.max(0.2, timeToNextRandomCarDir1 + 0.2);
                        if (DETAILED_LOGGING) System.out.println("Spawn Dir1 clear check failed. Deferring. Next try in: " + String.format("%.2f",timeToNextRandomCarDir1) + "s");
                    }
                }
            }
        } else { // Детерминированный поток
            timeSinceLastDeterministicCar += deltaTime;
            double requiredInterval = params.getDeterministicIntervalSeconds();
            if (requiredInterval > 0 && timeSinceLastDeterministicCar >= requiredInterval) {
                boolean carCreatedThisTick = false;
                int attemptsDir0 = 0;
                while(carDir0 == null && attemptsDir0 < MAX_SPAWN_LANE_ATTEMPTS) {
                    int targetLaneDir0Det = random.nextInt(params.getLanesPerDirection());
                    if (isSpawnPointClear(0, targetLaneDir0Det, existingCars)) {
                        carDir0 = createNewCarDeterministicParams(0, targetLaneDir0Det);
                        if (DETAILED_LOGGING && carDir0 != null) System.out.println("Deterministic Car Dir0 Spawned.");
                        carCreatedThisTick = true;
                    }
                    attemptsDir0++;
                }

                if (params.getNumberOfDirections() == 2) {
                    int attemptsDir1 = 0;
                    while(carDir1 == null && attemptsDir1 < MAX_SPAWN_LANE_ATTEMPTS) {
                        int targetLaneDir1Det = params.getLanesPerDirection() + random.nextInt(params.getLanesPerDirection());
                        if (isSpawnPointClear(1, targetLaneDir1Det, existingCars)) {
                            carDir1 = createNewCarDeterministicParams(1, targetLaneDir1Det);
                            if (DETAILED_LOGGING && carDir1 != null) System.out.println("Deterministic Car Dir1 Spawned.");
                            carCreatedThisTick = true;
                        }
                        attemptsDir1++;
                    }
                }
                if (carCreatedThisTick) {
                    timeSinceLastDeterministicCar -= requiredInterval;
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
        for (Car car : existingCars) {
            if (car.getLaneIndex() == targetLane && car.getDirection() == direction) {
                if (direction == 0 && car.getPosition() < MIN_SPAWN_CLEARANCE_M) return false;
                if (direction == 1 && (road.getLength() - car.getPosition()) < MIN_SPAWN_CLEARANCE_M) return false;
            }
        }
        return true;
    }

    private double generateNextRandomInterval() {
        double interval = 10.0; // Fallback interval
        DistributionLaw law = params.getTimeDistributionLaw();
        switch (law) {
            case UNIFORM:
                interval = params.getTimeUniformMinSec() +
                        (params.getTimeUniformMaxSec() - params.getTimeUniformMinSec()) * random.nextDouble();
                break;
            case NORMAL:
                // Дисперсия - это СКО (сигма) в квадрате. Нам нужна сигма.
                double sigmaTime = Math.sqrt(params.getTimeNormalVarianceSec());
                interval = params.getTimeNormalMeanSec() + sigmaTime * random.nextGaussian();
                break;
            case EXPONENTIAL:
                double intensityTime = params.getTimeExponentialIntensityPerSec();
                if (intensityTime <= 0) return Double.POSITIVE_INFINITY; // Интенсивность должна быть > 0
                // МО для экспоненциального = 1 / интенсивность
                double meanExpInterval = 1.0 / intensityTime;
                interval = -meanExpInterval * Math.log(1.0 - random.nextDouble()); // random.nextDouble() в (0,1)
                break;
            default:
                System.err.println("Неизвестный закон распределения времени: " + law);
                break;
        }
        interval = Math.max(0.2, interval); // Минимальный интервал, чтобы избежать слишком частого спавна
        if (DETAILED_LOGGING) System.out.printf("Generated next time interval: %.2fs (Law: %s)\n", interval, law);
        return interval;
    }

    private double generateRandomInitialSpeedKmh() {
        double speedKmh = params.getDeterministicSpeedKmh(); // Fallback speed
        DistributionLaw law = params.getSpeedDistributionLaw();
        switch (law) {
            case UNIFORM:
                speedKmh = params.getSpeedUniformMinKmh() +
                        (params.getSpeedUniformMaxKmh() - params.getSpeedUniformMinKmh()) * random.nextDouble();
                break;
            case NORMAL:
                double sigmaSpeed = Math.sqrt(params.getSpeedNormalVarianceKmh());
                speedKmh = params.getSpeedNormalMeanKmh() + sigmaSpeed * random.nextGaussian();
                break;
            case EXPONENTIAL:
                // Экспоненциальное для скорости - интерпретируем "интенсивность" как параметр формы lambda,
                // и тогда МО = 1/lambda. Это не совсем стандартно для скорости.
                double intensitySpeed = params.getSpeedExponentialIntensityPerKmh();
                if (intensitySpeed <= 0) { // Интенсивность должна быть > 0
                    speedKmh = params.getDeterministicSpeedKmh(); // Fallback
                    break;
                }
                double meanExpSpeed = 1.0 / intensitySpeed; // Если бы это была интенсивность появления
                speedKmh = -meanExpSpeed * Math.log(1.0 - random.nextDouble());
                break;
            default:
                System.err.println("Неизвестный закон распределения скорости: " + law);
                break;
        }
        // Ограничение скорости разумными пределами, например, 10-150 км/ч
        // Также ТЗ Приложение 1.27, 1.28 для МО скорости (20-130 км/ч)
        speedKmh = Math.max(10.0, Math.min(speedKmh, 150.0));
        if (DETAILED_LOGGING) System.out.printf("Generated initial speed: %.2f km/h (Law: %s)\n", speedKmh, law);
        return speedKmh;
    }

    private Car createNewCarWithRandomParams(int direction, int targetLaneIndex) {
        double initialPosition = (direction == 0) ? 0.0 : road.getLength();
        double initialSpeedKmh = generateRandomInitialSpeedKmh();
        double initialSpeedMs = initialSpeedKmh / 3.6;
        double maxSpeedMs = initialSpeedMs * (1.1 + random.nextDouble() * 0.4); // Макс. скорость на 10-50% выше начальной
        maxSpeedMs = Math.max(initialSpeedMs, Math.min(maxSpeedMs, 160.0/3.6)); // Ограничение сверху 160 км/ч

        // Параметры из Car.java (можно сделать их тоже случайными в каких-то пределах)
        double accelP = 1.8 + random.nextDouble() * 1.2; // 1.8 - 3.0 м/с^2
        double decelP = 2.5 + random.nextDouble() * 1.5; // 2.5 - 4.0 м/с^2

        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, accelP, decelP, targetLaneIndex, direction);
    }

    private Car createNewCarDeterministicParams(int direction, int targetLaneIndex) {
        double initialPosition = (direction == 0) ? 0.0 : road.getLength();
        double initialSpeedMs = params.getDeterministicSpeedKmh() / 3.6;
        double maxSpeedMs = initialSpeedMs * (1.1 + random.nextDouble() * 0.4);
        maxSpeedMs = Math.max(initialSpeedMs, Math.min(maxSpeedMs, 160.0/3.6));

        double accelP = 1.8 + random.nextDouble() * 1.2;
        double decelP = 2.5 + random.nextDouble() * 1.5;

        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, accelP, decelP, targetLaneIndex, direction);
    }

    public void updateParameters(SimulationParameters newParams) {
        // this.params = newParams; // Не нужно, т.к. params - это ссылка на объект из SimulationEngine
        System.out.println("TrafficFlowGenerator.updateParameters -> сброс таймеров генерации.");
        resetGenerationTimers(); // Переинициализируем таймеры на основе текущих (обновленных) params
    }
}
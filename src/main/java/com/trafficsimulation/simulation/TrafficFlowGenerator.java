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
    private static final boolean DETAILED_LOGGING = true;


    public TrafficFlowGenerator(SimulationParameters params, Road road) {
        this.params = params;
        this.road = road;
        this.random = new Random(System.currentTimeMillis());
        resetGenerationTimers();
        if (DETAILED_LOGGING) {
            System.out.println("TrafficFlowGenerator создан. Начальные таймеры сброшены.");
            System.out.println("  isRandomTimeFlow: " + params.isRandomTimeFlow() + ", isRandomSpeedFlow: " + params.isRandomSpeedFlow());
            System.out.println("  Dir0 next car in: " + String.format("%.2f", timeToNextRandomCarDir0));
            System.out.println("  Dir1 next car in: " + String.format("%.2f", timeToNextRandomCarDir1));
        }
    }

    private void resetGenerationTimers() {
        timeSinceLastDeterministicCar = 0.0;
        if (params.isRandomTimeFlow()) {
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

        if (params.isRandomTimeFlow()) { // Используем isRandomTimeFlow
            timeToNextRandomCarDir0 -= deltaTime;
            if (timeToNextRandomCarDir0 <= 0) {
                int targetLaneDir0 = random.nextInt(params.getLanesPerDirection());
                if (isSpawnPointClear(0, targetLaneDir0, existingCars)) {
                    carDir0 = createNewCar(0, targetLaneDir0);
                    double nextInterval = generateNextRandomInterval();
                    timeToNextRandomCarDir0 = nextInterval + timeToNextRandomCarDir0;
                    if (DETAILED_LOGGING && carDir0 != null) System.out.println("RandomTime Car Dir0 Spawned. Next in: " + String.format("%.2f",timeToNextRandomCarDir0) + "s (interval: " + String.format("%.2f",nextInterval) + "s)");
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
                        carDir1 = createNewCar(1, targetLaneDir1);
                        double nextInterval = generateNextRandomInterval();
                        timeToNextRandomCarDir1 = nextInterval + timeToNextRandomCarDir1;
                        if (DETAILED_LOGGING && carDir1 != null) System.out.println("RandomTime Car Dir1 Spawned. Next in: " + String.format("%.2f",timeToNextRandomCarDir1) + "s (interval: " + String.format("%.2f",nextInterval) + "s)");
                    } else {
                        timeToNextRandomCarDir1 = Math.max(0.2, timeToNextRandomCarDir1 + 0.2);
                        if (DETAILED_LOGGING) System.out.println("Spawn Dir1 clear check failed. Deferring. Next try in: " + String.format("%.2f",timeToNextRandomCarDir1) + "s");
                    }
                }
            }
        } else { // Детерминированное ВРЕМЯ
            timeSinceLastDeterministicCar += deltaTime;
            double requiredInterval = params.getDeterministicIntervalSeconds();
            if (requiredInterval > 0 && timeSinceLastDeterministicCar >= requiredInterval) {
                boolean carCreatedThisTick = false;
                int attemptsDir0 = 0;
                while(carDir0 == null && attemptsDir0 < MAX_SPAWN_LANE_ATTEMPTS) {
                    int targetLaneDir0Det = random.nextInt(params.getLanesPerDirection());
                    if (isSpawnPointClear(0, targetLaneDir0Det, existingCars)) {
                        carDir0 = createNewCar(0, targetLaneDir0Det);
                        if (DETAILED_LOGGING && carDir0 != null) System.out.println("DeterministicTime Car Dir0 Spawned.");
                        carCreatedThisTick = true;
                    }
                    attemptsDir0++;
                }
                if (params.getNumberOfDirections() == 2) {
                    int attemptsDir1 = 0;
                    while(carDir1 == null && attemptsDir1 < MAX_SPAWN_LANE_ATTEMPTS) {
                        int targetLaneDir1Det = params.getLanesPerDirection() + random.nextInt(params.getLanesPerDirection());
                        if (isSpawnPointClear(1, targetLaneDir1Det, existingCars)) {
                            carDir1 = createNewCar(1, targetLaneDir1Det);
                            if (DETAILED_LOGGING && carDir1 != null) System.out.println("DeterministicTime Car Dir1 Spawned.");
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
        double interval = 10.0;
        DistributionLaw law = params.getTimeDistributionLaw();
        switch (law) {
            case UNIFORM:
                interval = params.getTimeUniformMinSec() +
                        (params.getTimeUniformMaxSec() - params.getTimeUniformMinSec()) * random.nextDouble();
                break;
            case NORMAL:
                double sigmaTime = Math.sqrt(Math.max(0, params.getTimeNormalVarianceSec()));
                interval = params.getTimeNormalMeanSec() + sigmaTime * random.nextGaussian();
                break;
            case EXPONENTIAL:
                double intensityTime = params.getTimeExponentialIntensityPerSec();
                if (intensityTime <= 0.00001) return Double.POSITIVE_INFINITY;
                double meanExpInterval = 1.0 / intensityTime;
                interval = -meanExpInterval * Math.log(Math.max(0.00001, 1.0 - random.nextDouble()));
                break;
            default:
                System.err.println("TrafficFlowGenerator: Неизвестный закон распределения времени: " + law + ". Используется fallback интервал.");
                break;
        }
        interval = Math.max(0.2, interval);
        if (DETAILED_LOGGING) System.out.printf("Generated next time interval: %.2fs (Law: %s)\n", interval, law);
        return interval;
    }

    private double generateRandomInitialSpeedKmh() {
        double speedKmh = params.getDeterministicSpeedKmh();
        DistributionLaw law = params.getSpeedDistributionLaw();
        switch (law) {
            case UNIFORM:
                speedKmh = params.getSpeedUniformMinKmh() +
                        (params.getSpeedUniformMaxKmh() - params.getSpeedUniformMinKmh()) * random.nextDouble();
                break;
            case NORMAL:
                double sigmaSpeed = Math.sqrt(Math.max(0, params.getSpeedNormalVarianceKmh()));
                speedKmh = params.getSpeedNormalMeanKmh() + sigmaSpeed * random.nextGaussian();
                break;
            case EXPONENTIAL:
                double intensitySpeed = params.getSpeedExponentialIntensityPerKmh();
                if (intensitySpeed <= 0.00001) {
                    speedKmh = params.getDeterministicSpeedKmh();
                    break;
                }
                double meanExpSpeed = 1.0 / intensitySpeed;
                speedKmh = -meanExpSpeed * Math.log(Math.max(0.00001, 1.0 - random.nextDouble()));
                break;
            default:
                System.err.println("TrafficFlowGenerator: Неизвестный закон распределения скорости: " + law + ". Используется fallback скорость.");
                break;
        }
        speedKmh = Math.max(20.0, Math.min(speedKmh, 130.0)); // Ограничение по ТЗ для МО скорости
        if (DETAILED_LOGGING) System.out.printf("  Generated initial random speed: %.2f km/h (Law: %s)\n", speedKmh, law);
        return speedKmh;
    }

    private Car createNewCar(int direction, int targetLaneIndex) {
        double initialPosition = (direction == 0) ? 0.0 : road.getLength();
        double initialSpeedKmh;

        if (params.isRandomSpeedFlow()) { // Используем isRandomSpeedFlow
            initialSpeedKmh = generateRandomInitialSpeedKmh();
        } else {
            initialSpeedKmh = params.getDeterministicSpeedKmh();
            if (DETAILED_LOGGING) System.out.printf("  Car (dir %d, lane %d) generated DETERMINISTIC speed: %.2f km/h%n", direction, targetLaneIndex, initialSpeedKmh);
        }

        double initialSpeedMs = initialSpeedKmh / 3.6;
        double maxSpeedMs = initialSpeedMs * (1.1 + random.nextDouble() * 0.3);
        maxSpeedMs = Math.max(initialSpeedMs, Math.min(maxSpeedMs, 160.0/3.6));
        double accelP = 1.8 + random.nextDouble() * 1.2;
        double decelP = 2.5 + random.nextDouble() * 1.5;

        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, accelP, decelP, targetLaneIndex, direction);
    }

    public void updateParameters(SimulationParameters newParams) {
        // this.params = newParams; // Не нужно, params - это ссылка
        System.out.println("TrafficFlowGenerator.updateParameters -> сброс таймеров генерации.");
        resetGenerationTimers();
    }
}
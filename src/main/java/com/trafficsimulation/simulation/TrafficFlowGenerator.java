package com.trafficsimulation.simulation;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadType;

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
            System.out.println("TrafficFlowGenerator создан. Тип дороги: " + road.getType() +
                    ", Направления: " + params.getNumberOfDirections() +
                    ", Полос на направление: " + params.getLanesPerDirection());
        }
    }

    private void resetGenerationTimers() {
        timeSinceLastDeterministicCar = 0.0;
        if (params.isRandomTimeFlow()) {
            this.timeToNextRandomCarDir0 = generateNextRandomInterval();
            if (params.getNumberOfDirections() == 2 && road.getType() != RoadType.TUNNEL) {
                this.timeToNextRandomCarDir1 = generateNextRandomInterval();
                if (DETAILED_LOGGING) System.out.println("  TrafficFlowGenerator: Random Timer for Dir1 initialized to: " + String.format("%.2f", timeToNextRandomCarDir1));
            } else {
                this.timeToNextRandomCarDir1 = Double.POSITIVE_INFINITY;
                if (DETAILED_LOGGING) System.out.println("  TrafficFlowGenerator: Random Timer for Dir1 set to INFINITY (Dirs: " + params.getNumberOfDirections() + ", Type: " + road.getType() +")");
            }
        } else {
            this.timeToNextRandomCarDir0 = Double.POSITIVE_INFINITY;
            this.timeToNextRandomCarDir1 = Double.POSITIVE_INFINITY;
            if (DETAILED_LOGGING) System.out.println("  TrafficFlowGenerator: Deterministic time flow. Random timers set to INFINITY.");
        }
    }

    public Car[] generateCars(double deltaTime, TunnelControlState tunnelState) {
        Car carDir0 = null;
        Car carDir1 = null;
        List<Car> existingCars = road.getCars();

        boolean allowSpawnDir0 = true;
        boolean allowSpawnDir1 = (road.getNumberOfDirections() == 2);

        if (road.getType() == RoadType.TUNNEL && tunnelState != null) {
            allowSpawnDir0 = (tunnelState == TunnelControlState.DIR0_GREEN);
            allowSpawnDir1 = (tunnelState == TunnelControlState.DIR1_GREEN);
        }

        if (params.isRandomTimeFlow()) {
            if (allowSpawnDir0) {
                timeToNextRandomCarDir0 -= deltaTime;
                if (timeToNextRandomCarDir0 <= 0) {
                    int targetLaneDir0 = random.nextInt(params.getLanesPerDirection());
                    if (isSpawnPointClear(0, targetLaneDir0, existingCars)) {
                        carDir0 = createNewCar(0, targetLaneDir0);
                        double nextInterval = generateNextRandomInterval();
                        timeToNextRandomCarDir0 = nextInterval + timeToNextRandomCarDir0;
                        // if (DETAILED_LOGGING && carDir0 != null) System.out.println("RandomTime Car Dir0 Spawned (Lane " + targetLaneDir0 +"). Next in: " + String.format("%.2f",timeToNextRandomCarDir0) + "s (Interval: "+ String.format("%.2f",nextInterval) +")");
                    } else {
                        timeToNextRandomCarDir0 = Math.max(0.1, timeToNextRandomCarDir0 + 0.1);
                    }
                }
            }

            if (allowSpawnDir1) {
                timeToNextRandomCarDir1 -= deltaTime;
                if (timeToNextRandomCarDir1 <= 0) {
                    int localLaneIndex = random.nextInt(params.getLanesPerDirection());
                    int globalLaneIndexDir1 = params.getLanesPerDirection() + localLaneIndex;
                    if (isSpawnPointClear(1, globalLaneIndexDir1, existingCars)) {
                        carDir1 = createNewCar(1, globalLaneIndexDir1);
                        double nextInterval = generateNextRandomInterval();
                        timeToNextRandomCarDir1 = nextInterval + timeToNextRandomCarDir1;
                        // if (DETAILED_LOGGING && carDir1 != null) System.out.println("RandomTime Car Dir1 Spawned (Lane " + globalLaneIndexDir1 + "). Next in: " + String.format("%.2f",timeToNextRandomCarDir1) + "s (Interval: "+ String.format("%.2f",nextInterval) +")");
                    } else {
                        timeToNextRandomCarDir1 = Math.max(0.1, timeToNextRandomCarDir1 + 0.1);
                    }
                }
            }
        } else { // Детерминированное ВРЕМЯ появления
            timeSinceLastDeterministicCar += deltaTime;
            double requiredInterval = params.getDeterministicIntervalSeconds();

            if (requiredInterval > 0 && timeSinceLastDeterministicCar >= requiredInterval) {
                boolean carCreatedThisTick = false;
                if (allowSpawnDir0) {
                    for (int attempt = 0; attempt < MAX_SPAWN_LANE_ATTEMPTS && carDir0 == null; attempt++) {
                        int targetLaneDir0Det = random.nextInt(params.getLanesPerDirection());
                        if (isSpawnPointClear(0, targetLaneDir0Det, existingCars)) {
                            carDir0 = createNewCar(0, targetLaneDir0Det);
                            // Logged in createNewCar
                            if (carDir0 != null) carCreatedThisTick = true;
                            break;
                        }
                    }
                }
                // ИЗМЕНЕНИЕ: Добавлено подробное логирование для Dir 1
                if (allowSpawnDir1) {
                    if (DETAILED_LOGGING) System.out.println("Attempting to generate deterministic car for Dir 1. Road Dirs: " + road.getNumberOfDirections() + ", LanesPerDir: " + params.getLanesPerDirection());
                    for (int attempt = 0; attempt < MAX_SPAWN_LANE_ATTEMPTS && carDir1 == null; attempt++) {
                        int localLaneIndex = random.nextInt(params.getLanesPerDirection());
                        // Глобальный индекс полосы для второго направления:
                        // полосы первого направления [0 ... lanesPerDir-1]
                        // полосы второго направления [lanesPerDir ... 2*lanesPerDir-1]
                        int targetLaneDir1Det = params.getLanesPerDirection() + localLaneIndex;
                        if (DETAILED_LOGGING) System.out.println("  Dir 1, Attempt " + (attempt + 1) + ": Trying global lane " + targetLaneDir1Det + " (local " + localLaneIndex + ")");

                        if (targetLaneDir1Det >= road.getNumberOfLanes()){ // Защита от выхода за пределы общего числа полос
                            if (DETAILED_LOGGING) System.err.println("    ERROR: Target global lane " + targetLaneDir1Det + " is out of bounds (Total lanes: " + road.getNumberOfLanes() + "). Skipping attempt.");
                            continue;
                        }

                        if (isSpawnPointClear(1, targetLaneDir1Det, existingCars)) {
                            if (DETAILED_LOGGING) System.out.println("    Spawn point clear for Dir 1, Global Lane " + targetLaneDir1Det);
                            carDir1 = createNewCar(1, targetLaneDir1Det);
                            if (carDir1 != null) {
                                if (DETAILED_LOGGING) System.out.println("    SUCCESS: DeterministicTime Car Dir1 Spawned. ID: " + carDir1.getId() + ", Global Lane " + targetLaneDir1Det + ", Pos: " + String.format("%.1f", carDir1.getPosition()));
                                carCreatedThisTick = true;
                                break;
                            } else if (DETAILED_LOGGING) {
                                System.err.println("    ERROR: createNewCar for Dir 1 returned NULL for global lane " + targetLaneDir1Det + "!");
                            }
                        } else if (DETAILED_LOGGING) {
                            System.out.println("    Spawn point NOT clear for Dir 1, Global Lane " + targetLaneDir1Det);
                        }
                    }
                }

                if (carCreatedThisTick) {
                    timeSinceLastDeterministicCar -= requiredInterval;
                } else if (allowSpawnDir0 || allowSpawnDir1) {
                    timeSinceLastDeterministicCar = Math.max(0, requiredInterval - 0.1);
                }
            }
        }

        if (carDir0 != null && carDir1 != null) return new Car[]{carDir0, carDir1};
        if (carDir0 != null) return new Car[]{carDir0};
        if (carDir1 != null) return new Car[]{carDir1};
        return null;
    }

    private boolean isSpawnPointClear(int direction, int targetGlobalLaneIndex, List<Car> existingCars) {
        if (existingCars == null || road == null) return true;
        for (Car car : existingCars) {
            if (car.getLaneIndex() == targetGlobalLaneIndex && car.getDirection() == direction) {
                if (direction == 0) {
                    if (car.getPosition() < MIN_SPAWN_CLEARANCE_M) {
                        return false;
                    }
                } else {
                    if ((road.getLength() - car.getPosition()) < MIN_SPAWN_CLEARANCE_M) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private double generateNextRandomInterval() {
        double interval = 10.0;
        DistributionLaw law = params.getTimeDistributionLaw();
        if (law == null) law = DistributionLaw.NORMAL;
        switch (law) {
            case UNIFORM:
                interval = params.getTimeUniformMinSec() +
                        (params.getTimeUniformMaxSec() - params.getTimeUniformMinSec()) * random.nextDouble();
                break;
            case NORMAL:
                double sigmaTime = Math.sqrt(Math.max(0.001, params.getTimeNormalVarianceSec()));
                interval = params.getTimeNormalMeanSec() + sigmaTime * random.nextGaussian();
                break;
            case EXPONENTIAL:
                double intensityTime = params.getTimeExponentialIntensityPerSec();
                if (intensityTime <= 1e-5) return Double.POSITIVE_INFINITY;
                double meanExpInterval = 1.0 / intensityTime;
                interval = -meanExpInterval * Math.log(Math.max(1e-9, 1.0 - random.nextDouble()));
                break;
        }
        return Math.max(0.1, interval);
    }

    private double generateInitialSpeedKmhBasedOnSettings() {
        double speedKmh;
        if (params.isRandomSpeedFlow()) {
            DistributionLaw law = params.getSpeedDistributionLaw();
            if (law == null) law = DistributionLaw.NORMAL;
            switch (law) {
                case UNIFORM:
                    speedKmh = params.getSpeedUniformMinKmh() +
                            (params.getSpeedUniformMaxKmh() - params.getSpeedUniformMinKmh()) * random.nextDouble();
                    break;
                case NORMAL:
                    double sigmaSpeed = Math.sqrt(Math.max(0.001, params.getSpeedNormalVarianceKmh()));
                    speedKmh = params.getSpeedNormalMeanKmh() + sigmaSpeed * random.nextGaussian();
                    break;
                case EXPONENTIAL:
                    double intensitySpeed = params.getSpeedExponentialIntensityPerKmh();
                    if (intensitySpeed <= 1e-5) {
                        speedKmh = params.getDeterministicSpeedKmh();
                    } else {
                        double meanExpSpeed = 1.0 / intensitySpeed;
                        speedKmh = -meanExpSpeed * Math.log(Math.max(1e-9, 1.0 - random.nextDouble()));
                    }
                    break;
                default:
                    speedKmh = params.getDeterministicSpeedKmh();
                    break;
            }
        } else {
            speedKmh = params.getDeterministicSpeedKmh();
        }
        return Math.max(10.0, Math.min(speedKmh, 150.0));
    }

    private Car createNewCar(int direction, int targetGlobalLaneIndex) {
        double initialPosition = (direction == 0) ? 0.0 : road.getLength();
        RoadType currentRoadType = road.getType();
        double generatedSpeedKmh = generateInitialSpeedKmhBasedOnSettings();
        double minRoadTypeSpeedKmh = currentRoadType.getMinSpeedLimitKmh();
        double maxRoadTypeSpeedKmh = currentRoadType.getMaxSpeedLimitKmh();
        double initialSpeedKmh = Math.max(minRoadTypeSpeedKmh, Math.min(generatedSpeedKmh, maxRoadTypeSpeedKmh));

        if (DETAILED_LOGGING) {
            System.out.printf(">>> CREATE CAR: Dir: %d, GlobalLane: %d, RoadType: %s, RawSpeed:%.1f, RoadLimits[%.1f-%.1f], FinalInitialSpeed:%.1f km/h, InitPos: %.1f%n",
                    direction, targetGlobalLaneIndex, currentRoadType, generatedSpeedKmh, minRoadTypeSpeedKmh, maxRoadTypeSpeedKmh, initialSpeedKmh, initialPosition);
        }
        double initialSpeedMs = initialSpeedKmh / 3.6;
        double maxCarSpeedKmh = Math.min(initialSpeedKmh * (1.05 + random.nextDouble() * 0.20), maxRoadTypeSpeedKmh);
        maxCarSpeedKmh = Math.max(initialSpeedKmh, maxCarSpeedKmh);
        maxCarSpeedKmh = Math.min(maxCarSpeedKmh, currentRoadType.getMaxSpeedLimitKmh());
        double maxSpeedMs = maxCarSpeedKmh / 3.6;
        double accelParam = 1.5 + random.nextDouble() * 1.0;
        double decelParam = 2.0 + random.nextDouble() * 1.5;
        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, accelParam, decelParam, targetGlobalLaneIndex, direction);
    }

    public void updateParameters(SimulationParameters newParams) {
        if (DETAILED_LOGGING) System.out.println("TrafficFlowGenerator.updateParameters -> сброс таймеров генерации. Новый тип дороги: " + (road != null ? road.getType() : "null") +
                ", Dirs: " + params.getNumberOfDirections() + ", LanesPerDir: " + params.getLanesPerDirection());
        resetGenerationTimers();
    }
}
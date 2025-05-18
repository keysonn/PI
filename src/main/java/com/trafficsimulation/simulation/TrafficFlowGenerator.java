package com.trafficsimulation.simulation;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.model.Car.DriverType;

import java.util.List;
import java.util.Random;

public class TrafficFlowGenerator {

    private final SimulationParameters params;
    private final Road road;
    private final Random random;

    private double timeToNextCarScreenTop = 0.0;
    private double timeToNextCarScreenBottom = 0.0;
    private double timeSinceLastDeterministicCar = 0.0;
    private boolean deterministicSpawnOnScreenTopNext = true;

    private static final double MIN_SPAWN_CLEARANCE_M = 7.0;

    public TrafficFlowGenerator(SimulationParameters params, Road road) {
        this.params = params;
        this.road = road;
        this.random = new Random(System.currentTimeMillis());
        resetGenerationTimers();
    }

    public void resetGenerationTimers() {
        timeSinceLastDeterministicCar = 0.0;
        deterministicSpawnOnScreenTopNext = true;

        if (params.isRandomTimeFlow()) {
            this.timeToNextCarScreenTop = generateNextRandomInterval();
            if (params.getNumberOfDirections() == 2 || params.getNumberOfDirections() == 1) {
                this.timeToNextCarScreenBottom = generateNextRandomInterval();
                if (params.getNumberOfDirections() == 1 && road.getType() != RoadType.TUNNEL) {
                    this.timeToNextCarScreenTop = Double.POSITIVE_INFINITY;
                }
            } else {
                this.timeToNextCarScreenBottom = Double.POSITIVE_INFINITY;
            }
        } else {
            this.timeToNextCarScreenTop = Double.POSITIVE_INFINITY;
            this.timeToNextCarScreenBottom = Double.POSITIVE_INFINITY;
        }
    }

    public Car[] generateCars(double deltaTime, TunnelControlState tunnelState) {
        Car carForScreenTop = null;
        Car carForScreenBottom = null;
        List<Car> existingCars = road.getCars();
        boolean allowSpawnForScreenTop;
        boolean allowSpawnForScreenBottom;
        if (road.getType() == RoadType.TUNNEL && tunnelState != null) {
            allowSpawnForScreenBottom = (tunnelState == TunnelControlState.DIR0_GREEN);
            allowSpawnForScreenTop = (tunnelState == TunnelControlState.DIR1_GREEN);
        } else if (road.getNumberOfDirections() == 1) {
            allowSpawnForScreenTop = false;
            allowSpawnForScreenBottom = true;
        } else {
            allowSpawnForScreenTop = true;
            allowSpawnForScreenBottom = true;
        }

        if (params.isRandomTimeFlow()) {
            if (allowSpawnForScreenTop) {
                timeToNextCarScreenTop -= deltaTime;
                if (timeToNextCarScreenTop <= 0) {
                    DriverType driverType = getRandomDriverType();
                    double initialSpeedKmh = generateInitialSpeedKmhFromSettings(driverType);
                    int initialLocalLane1 = determineInitialLocalLane(1, initialSpeedKmh, driverType);
                    if (isSpawnPointClear(1, initialLocalLane1, existingCars)) {
                        carForScreenTop = createNewCar(1, initialLocalLane1, initialSpeedKmh, driverType);
                        timeToNextCarScreenTop = generateNextRandomInterval() + timeToNextCarScreenTop;
                    } else {
                        timeToNextCarScreenTop = Math.max(0.1, timeToNextCarScreenTop + 0.1);
                    }
                }
            }
            if (allowSpawnForScreenBottom) {
                timeToNextCarScreenBottom -= deltaTime;
                if (timeToNextCarScreenBottom <= 0) {
                    DriverType driverType = getRandomDriverType();
                    double initialSpeedKmh = generateInitialSpeedKmhFromSettings(driverType);
                    int initialLocalLane0 = determineInitialLocalLane(0, initialSpeedKmh, driverType);
                    if (isSpawnPointClear(0, initialLocalLane0, existingCars)) {
                        carForScreenBottom = createNewCar(0, initialLocalLane0, initialSpeedKmh, driverType);
                        timeToNextCarScreenBottom = generateNextRandomInterval() + timeToNextCarScreenBottom;
                    } else {
                        timeToNextCarScreenBottom = Math.max(0.1, timeToNextCarScreenBottom + 0.1);
                    }
                }
            }
        } else {
            timeSinceLastDeterministicCar += deltaTime;
            double requiredInterval = params.getDeterministicIntervalSeconds();
            if (requiredInterval > 0 && timeSinceLastDeterministicCar >= requiredInterval) {
                boolean carCreatedThisTick = false;
                DriverType driverType = getRandomDriverType();
                double baseInitialSpeedKmh = params.isRandomSpeedFlow() ? -1 : params.getDeterministicSpeedKmh();
                if (road.getNumberOfDirections() == 2) {
                    if (deterministicSpawnOnScreenTopNext && allowSpawnForScreenTop) {
                        double speedForThisCar = (baseInitialSpeedKmh == -1) ? generateInitialSpeedKmhFromSettings(driverType) : baseInitialSpeedKmh;
                        int initialLocalLane1 = determineInitialLocalLane(1, speedForThisCar, driverType);
                        if (isSpawnPointClear(1, initialLocalLane1, existingCars)) {
                            carForScreenTop = createNewCar(1, initialLocalLane1, speedForThisCar, driverType);
                            if (carForScreenTop != null) {
                                carCreatedThisTick = true;
                                deterministicSpawnOnScreenTopNext = false;
                            }
                        }
                    } else if (!deterministicSpawnOnScreenTopNext && allowSpawnForScreenBottom) {
                        double speedForThisCar = (baseInitialSpeedKmh == -1) ? generateInitialSpeedKmhFromSettings(driverType) : baseInitialSpeedKmh;
                        int initialLocalLane0 = determineInitialLocalLane(0, speedForThisCar, driverType);
                        if (isSpawnPointClear(0, initialLocalLane0, existingCars)) {
                            carForScreenBottom = createNewCar(0, initialLocalLane0, speedForThisCar, driverType);
                            if (carForScreenBottom != null) {
                                carCreatedThisTick = true;
                                deterministicSpawnOnScreenTopNext = true;
                            }
                        }
                    } else {
                        if (allowSpawnForScreenBottom && deterministicSpawnOnScreenTopNext) {
                            double speedForThisCar = (baseInitialSpeedKmh == -1) ? generateInitialSpeedKmhFromSettings(driverType) : baseInitialSpeedKmh;
                            int initialLocalLane0 = determineInitialLocalLane(0, speedForThisCar, driverType);
                            if (isSpawnPointClear(0, initialLocalLane0, existingCars)) {
                                carForScreenBottom = createNewCar(0, initialLocalLane0, speedForThisCar, driverType);
                                if (carForScreenBottom != null) carCreatedThisTick = true;
                            }
                        } else if (allowSpawnForScreenTop && !deterministicSpawnOnScreenTopNext) {
                            double speedForThisCar = (baseInitialSpeedKmh == -1) ? generateInitialSpeedKmhFromSettings(driverType) : baseInitialSpeedKmh;
                            int initialLocalLane1 = determineInitialLocalLane(1, speedForThisCar, driverType);
                            if (isSpawnPointClear(1, initialLocalLane1, existingCars)) {
                                carForScreenTop = createNewCar(1, initialLocalLane1, speedForThisCar, driverType);
                                if (carForScreenTop != null) carCreatedThisTick = true;
                            }
                        }
                    }
                } else if (road.getNumberOfDirections() == 1) {
                    if (allowSpawnForScreenBottom) {
                        double speedForThisCar = (baseInitialSpeedKmh == -1) ? generateInitialSpeedKmhFromSettings(driverType) : baseInitialSpeedKmh;
                        int initialLocalLane0 = determineInitialLocalLane(0, speedForThisCar, driverType);
                        if (isSpawnPointClear(0, initialLocalLane0, existingCars)) {
                            carForScreenBottom = createNewCar(0, initialLocalLane0, speedForThisCar, driverType);
                            if (carForScreenBottom != null) carCreatedThisTick = true;
                        }
                    }
                }
                if (carCreatedThisTick) {
                    timeSinceLastDeterministicCar -= requiredInterval;
                } else if (allowSpawnForScreenTop || allowSpawnForScreenBottom) {
                    timeSinceLastDeterministicCar = Math.max(0, requiredInterval - 0.1);
                }
            }
        }
        if (carForScreenTop != null && carForScreenBottom != null) return new Car[]{carForScreenTop, carForScreenBottom};
        if (carForScreenTop != null) return new Car[]{carForScreenTop};
        if (carForScreenBottom != null) return new Car[]{carForScreenBottom};
        return null;
    }

    private DriverType getRandomDriverType() {
        double randVal = random.nextDouble();
        if (randVal < 0.15) return DriverType.CAUTIOUS;
        else if (randVal < 0.85) return DriverType.NORMAL;
        else return DriverType.AGGRESSIVE;
    }

    private int determineInitialLocalLane(int modelDirection, double initialGeneratedSpeedKmh, DriverType driverType) {
        int lanesPerDir = params.getLanesPerDirection();
        if (lanesPerDir <= 1) {
            return 0;
        }
        RoadType roadType = road.getType();
        double roadDefaultSpeedKmh = roadType.getDefaultSpeedLimitKmh();
        double roadMaxSpeedKmh = roadType.getMaxSpeedLimitKmh();
        if (driverType == DriverType.AGGRESSIVE && initialGeneratedSpeedKmh > roadDefaultSpeedKmh * 1.1) {
            return lanesPerDir - 1;
        }
        if (driverType == DriverType.CAUTIOUS) {
            return 0;
        }
        if (initialGeneratedSpeedKmh >= roadMaxSpeedKmh * 0.88) {
            return lanesPerDir - 1;
        } else if (initialGeneratedSpeedKmh > roadDefaultSpeedKmh * 1.10 && lanesPerDir > 2) {
            return 1 + random.nextInt(lanesPerDir - 1);
        } else if (initialGeneratedSpeedKmh > roadDefaultSpeedKmh * 1.02 && lanesPerDir > 1) {
            return Math.min(1, lanesPerDir - 1);
        } else {
            return 0;
        }
    }

    private boolean isSpawnPointClear(int modelDirection, int targetLocalLaneIndex, List<Car> existingCars) {
        if (existingCars == null || road == null) return true;
        for (Car car : existingCars) {
            if (car.getDirection() == modelDirection && car.getCurrentLaneIndex() == targetLocalLaneIndex) {
                if (modelDirection == 0) {
                    if (car.getPosition() < MIN_SPAWN_CLEARANCE_M) return false;
                } else {
                    if ((road.getLength() - car.getPosition()) < MIN_SPAWN_CLEARANCE_M) return false;
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
                interval = params.getTimeUniformMinSec() + (params.getTimeUniformMaxSec() - params.getTimeUniformMinSec()) * random.nextDouble();
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

    private double generateInitialSpeedKmhFromSettings(DriverType driverType) {
        double speedKmh;
        if (params.isRandomSpeedFlow()) {
            DistributionLaw law = params.getSpeedDistributionLaw();
            if (law == null) law = DistributionLaw.NORMAL;
            switch (law) {
                case UNIFORM:
                    speedKmh = params.getSpeedUniformMinKmh() + (params.getSpeedUniformMaxKmh() - params.getSpeedUniformMinKmh()) * random.nextDouble();
                    break;
                case NORMAL:
                    double sigmaSpeed = Math.sqrt(Math.max(0.001, params.getSpeedNormalVarianceKmh()));
                    speedKmh = params.getSpeedNormalMeanKmh() + sigmaSpeed * random.nextGaussian();
                    break;
                case EXPONENTIAL:
                    double intensitySpeed = params.getSpeedExponentialIntensityPerKmh();
                    if (intensitySpeed <= 1e-5) speedKmh = params.getDeterministicSpeedKmh();
                    else {
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

    private Car createNewCar(int modelDirection, int targetLocalLaneIndex, double initialGeneratedSpeedKmh, DriverType driverType) {
        double initialPosition = (modelDirection == 0) ? 0.0 : road.getLength();
        RoadType currentRoadType = road.getType();
        double roadMinKmh = currentRoadType.getMinSpeedLimitKmh();
        double roadMaxKmh = currentRoadType.getMaxSpeedLimitKmh();
        double roadDefaultKmh = currentRoadType.getDefaultSpeedLimitKmh();
        double personalMaxSpeedKmh;
        switch (driverType) {
            case CAUTIOUS:
                personalMaxSpeedKmh = roadMinKmh + (roadDefaultKmh - roadMinKmh) * (0.1 + random.nextDouble() * 0.5);
                break;
            case AGGRESSIVE:
                personalMaxSpeedKmh = roadDefaultKmh + (roadMaxKmh - roadDefaultKmh) * (0.7 + random.nextDouble() * 0.3);
                break;
            case NORMAL:
            default:
                personalMaxSpeedKmh = roadDefaultKmh * (0.90 + random.nextDouble() * 0.25);
                break;
        }
        personalMaxSpeedKmh = Math.max(roadMinKmh, Math.min(personalMaxSpeedKmh, roadMaxKmh));
        double initialSpeedKmh = Math.min(initialGeneratedSpeedKmh, personalMaxSpeedKmh);
        initialSpeedKmh = Math.max(roadMinKmh, Math.min(initialSpeedKmh, roadMaxKmh));

        double initialSpeedMs = initialSpeedKmh / 3.6;
        double personalMaxSpeedMs = personalMaxSpeedKmh / 3.6;
        return new Car(initialPosition, initialSpeedMs, personalMaxSpeedMs, driverType, targetLocalLaneIndex, modelDirection);
    }

    public void updateParameters(SimulationParameters newParams) {
        resetGenerationTimers();
    }
}
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
    private static final boolean DETAILED_LOGGING = false;


    public TrafficFlowGenerator(SimulationParameters params, Road road) {
        this.params = params;
        this.road = road;
        this.random = new Random(System.currentTimeMillis());
        resetGenerationTimers();
        // ... (лог без изменений)
    }

    private void resetGenerationTimers() {
        // ... (без изменений) ...
        timeSinceLastDeterministicCar = 0.0;
        deterministicSpawnOnScreenTopNext = true;

        if (params.isRandomTimeFlow()) {
            this.timeToNextCarScreenTop = generateNextRandomInterval();
            if (DETAILED_LOGGING) System.out.println("  TFG: Random Timer for ScreenTop (model_dir 1) initialized to: " + String.format("%.2f", timeToNextCarScreenTop));

            if ((params.getNumberOfDirections() == 2 && road.getType() != RoadType.TUNNEL) ||
                    (params.getNumberOfDirections() == 1 && road.getType() != RoadType.TUNNEL)) {
                this.timeToNextCarScreenBottom = generateNextRandomInterval();
                if (DETAILED_LOGGING) System.out.println("  TFG: Random Timer for ScreenBottom (model_dir 0) initialized to: " + String.format("%.2f", timeToNextCarScreenBottom));
                if (params.getNumberOfDirections() == 1) {
                    this.timeToNextCarScreenTop = Double.POSITIVE_INFINITY;
                    if (DETAILED_LOGGING) System.out.println("    (Single direction road, ScreenTop timer set to INF)");
                }
            } else {
                this.timeToNextCarScreenBottom = Double.POSITIVE_INFINITY;
                if (DETAILED_LOGGING && params.getNumberOfDirections() == 2) System.out.println("  TFG: ScreenBottom (model_dir 0) timer for Tunnel set to INF (handled by tunnel logic).");
            }
        } else {
            this.timeToNextCarScreenTop = Double.POSITIVE_INFINITY;
            this.timeToNextCarScreenBottom = Double.POSITIVE_INFINITY;
            if (DETAILED_LOGGING) System.out.println("  TFG: Deterministic time flow. Random timers set to INFINITY.");
        }
    }

    public Car[] generateCars(double deltaTime, TunnelControlState tunnelState) {
        // ... (логика разрешения спавна и таймеров без изменений) ...
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
                    int initialLocalLane1 = determineInitialLocalLane(1, initialSpeedKmh, driverType); // Передаем driverType
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
                    int initialLocalLane0 = determineInitialLocalLane(0, initialSpeedKmh, driverType); // Передаем driverType
                    if (isSpawnPointClear(0, initialLocalLane0, existingCars)) {
                        carForScreenBottom = createNewCar(0, initialLocalLane0, initialSpeedKmh, driverType);
                        timeToNextCarScreenBottom = generateNextRandomInterval() + timeToNextCarScreenBottom;
                    } else {
                        timeToNextCarScreenBottom = Math.max(0.1, timeToNextCarScreenBottom + 0.1);
                    }
                }
            }
        } else { // Детерминированный поток времени
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
        // ... (возврат массива машин без изменений) ...
        if (carForScreenTop != null && carForScreenBottom != null) return new Car[]{carForScreenTop, carForScreenBottom};
        if (carForScreenTop != null) return new Car[]{carForScreenTop};
        if (carForScreenBottom != null) return new Car[]{carForScreenBottom};
        return null;
    }

    private DriverType getRandomDriverType() {
        // ... (без изменений) ...
        double randVal = random.nextDouble();
        if (randVal < 0.15) return DriverType.CAUTIOUS;
        else if (randVal < 0.85) return DriverType.NORMAL;
        else return DriverType.AGGRESSIVE;
    }

    // Определяет начальную локальную полосу на основе сгенерированной начальной скорости И ТИПА ВОДИТЕЛЯ
    private int determineInitialLocalLane(int modelDirection, double initialGeneratedSpeedKmh, DriverType driverType) {
        int lanesPerDir = params.getLanesPerDirection();
        if (lanesPerDir <= 1) {
            return 0;
        }

        RoadType roadType = road.getType();
        double roadMinSpeedKmh = roadType.getMinSpeedLimitKmh();
        double roadDefaultSpeedKmh = roadType.getDefaultSpeedLimitKmh();
        double roadMaxSpeedKmh = roadType.getMaxSpeedLimitKmh();

        // Локальный индекс: 0 = правая, lanesPerDir-1 = левая

        // Агрессивные водители с высокой скоростью предпочитают левые полосы
        if (driverType == DriverType.AGGRESSIVE && initialGeneratedSpeedKmh > roadDefaultSpeedKmh * 1.1) {
            return lanesPerDir - 1; // Самая левая
        }
        // Осторожные водители всегда предпочитают правую, если только их скорость не вынуждает (что маловероятно при генерации)
        if (driverType == DriverType.CAUTIOUS) {
            return 0; // Самая правая
        }

        // Нормальные водители (и агрессивные со средней/низкой скоростью)
        if (initialGeneratedSpeedKmh >= roadMaxSpeedKmh * 0.85) { // Близко к максимуму дороги -> левая
            return lanesPerDir - 1;
        } else if (initialGeneratedSpeedKmh > roadDefaultSpeedKmh * 1.05) { // Быстрее типичной
            if (lanesPerDir == 2) return 1; // Левая из двух
            if (lanesPerDir > 2) return 1 + random.nextInt(lanesPerDir - 1); // Одна из левых, не самая правая
        } else if (initialGeneratedSpeedKmh < roadDefaultSpeedKmh * 0.9) { // Медленнее типичной
            return 0; // Правая
        }
        // Для скоростей около типичной, или если выше не сработало
        return random.nextInt(Math.max(1, lanesPerDir / 2 + (lanesPerDir % 2))); // Случайная из правых/центральных
    }

    private boolean isSpawnPointClear(int modelDirection, int targetLocalLaneIndex, List<Car> existingCars) {
        // ... (без изменений) ...
        if (existingCars == null || road == null) return true;
        for (Car car : existingCars) {
            if (car.getDirection() == modelDirection && car.getCurrentLaneIndex() == targetLocalLaneIndex) {
                if (modelDirection == 0) { // L->R, спавн x=0
                    if (car.getPosition() < MIN_SPAWN_CLEARANCE_M) return false;
                } else { // R->L, спавн x=road.getLength()
                    if ((road.getLength() - car.getPosition()) < MIN_SPAWN_CLEARANCE_M) return false;
                }
            }
        }
        return true;
    }

    private double generateNextRandomInterval() {
        // ... (без изменений) ...
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
        // ... (без изменений) ...
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

    // Создает машину с учетом типа водителя и персонального максимума скорости
    private Car createNewCar(int modelDirection, int targetLocalLaneIndex, double initialGeneratedSpeedKmh, DriverType driverType) {
        double initialPosition = (modelDirection == 0) ? 0.0 : road.getLength();
        RoadType currentRoadType = road.getType();

        double roadMinKmh = currentRoadType.getMinSpeedLimitKmh();
        double roadMaxKmh = currentRoadType.getMaxSpeedLimitKmh();
        double roadDefaultKmh = currentRoadType.getDefaultSpeedLimitKmh();

        // 1. Определяем ПЕРСОНАЛЬНЫЙ максимум скорости для этой машины (км/ч)
        double personalMaxSpeedKmh;
        switch (driverType) {
            case CAUTIOUS:
                // Их максимум ниже среднего, ближе к минимуму дороги
                personalMaxSpeedKmh = roadMinKmh + (roadDefaultKmh - roadMinKmh) * (0.1 + random.nextDouble() * 0.5); // от 10% до 60% диапазона [min, default]
                break;
            case AGGRESSIVE:
                // Их максимум выше среднего, ближе к максимуму дороги
                personalMaxSpeedKmh = roadDefaultKmh + (roadMaxKmh - roadDefaultKmh) * (0.7 + random.nextDouble() * 0.3); // от 70% до 100% диапазона [default, max]
                break;
            case NORMAL:
            default:
                // Разброс вокруг default скорости, но в пределах min/max дороги
                personalMaxSpeedKmh = roadDefaultKmh * (0.90 + random.nextDouble() * 0.25); // от 90% до 115% от default
                break;
        }
        // Окончательно ограничиваем персональный максимум реальными лимитами дороги
        personalMaxSpeedKmh = Math.max(roadMinKmh, Math.min(personalMaxSpeedKmh, roadMaxKmh));

        // 2. Начальная скорость на основе сгенерированной из настроек потока,
        //    но не выше персонального максимума и лимитов дороги
        double initialSpeedKmh = Math.min(initialGeneratedSpeedKmh, personalMaxSpeedKmh);
        initialSpeedKmh = Math.max(roadMinKmh, Math.min(initialSpeedKmh, roadMaxKmh));


        if (DETAILED_LOGGING) {
            String screenDir = (modelDirection == 1) ? "ScreenTop (R->L, model_dir 1)" : "ScreenBottom (L->R, model_dir 0)";
            System.out.printf(">>> CREATE CAR for %s: Type: %s, InitLocalLane: %d, InitPos: %.1f, InitialSpeed:%.1f km/h (PersonalMax: %.1f, FlowGenSpeed: %.1f)%n",
                    screenDir, driverType, targetLocalLaneIndex, initialPosition, initialSpeedKmh, personalMaxSpeedKmh, initialGeneratedSpeedKmh);
        }

        double initialSpeedMs = initialSpeedKmh / 3.6;
        double personalMaxSpeedMs = personalMaxSpeedKmh / 3.6;

        return new Car(initialPosition, initialSpeedMs, personalMaxSpeedMs, driverType, targetLocalLaneIndex, modelDirection);
    }

    public void updateParameters(SimulationParameters newParams) {
        if (DETAILED_LOGGING) System.out.println("TrafficFlowGenerator.updateParameters -> сброс таймеров генерации.");
        resetGenerationTimers();
    }
}
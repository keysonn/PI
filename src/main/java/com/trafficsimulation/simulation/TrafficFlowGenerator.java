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

    private double timeToNextCarScreenTop = 0.0;
    private double timeToNextCarScreenBottom = 0.0;
    private double timeSinceLastDeterministicCar = 0.0;
    private boolean deterministicSpawnOnScreenTopNext = true;

    private static final double MIN_SPAWN_CLEARANCE_M = 7.0;
    private static final boolean DETAILED_LOGGING = false; // Выключим для производительности


    public TrafficFlowGenerator(SimulationParameters params, Road road) {
        this.params = params;
        this.road = road;
        this.random = new Random(System.currentTimeMillis());
        resetGenerationTimers();
    }

    private void resetGenerationTimers() {
        // ... (без изменений, как в предыдущей версии) ...
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
        // ... (логика разрешения спавна и таймеров без изменений, как в предыдущей версии) ...
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

        // int targetLocalLaneIndex = 0; // Старая логика - всегда на правую

        if (params.isRandomTimeFlow()) {
            if (allowSpawnForScreenTop) {
                timeToNextCarScreenTop -= deltaTime;
                if (timeToNextCarScreenTop <= 0) {
                    // Выбираем начальную полосу для model_dir = 1 (ScreenTop)
                    int initialLocalLane1 = determineInitialLocalLane(1);
                    if (isSpawnPointClear(1, initialLocalLane1, existingCars)) {
                        carForScreenTop = createNewCar(1, initialLocalLane1);
                        timeToNextCarScreenTop = generateNextRandomInterval() + timeToNextCarScreenTop;
                    } else {
                        timeToNextCarScreenTop = Math.max(0.1, timeToNextCarScreenTop + 0.1);
                    }
                }
            }

            if (allowSpawnForScreenBottom) {
                timeToNextCarScreenBottom -= deltaTime;
                if (timeToNextCarScreenBottom <= 0) {
                    // Выбираем начальную полосу для model_dir = 0 (ScreenBottom)
                    int initialLocalLane0 = determineInitialLocalLane(0);
                    if (isSpawnPointClear(0, initialLocalLane0, existingCars)) {
                        carForScreenBottom = createNewCar(0, initialLocalLane0);
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
                if (road.getNumberOfDirections() == 2) {
                    if (deterministicSpawnOnScreenTopNext && allowSpawnForScreenTop) {
                        int initialLocalLane1 = determineInitialLocalLane(1);
                        if (isSpawnPointClear(1, initialLocalLane1, existingCars)) {
                            carForScreenTop = createNewCar(1, initialLocalLane1);
                            if (carForScreenTop != null) {
                                carCreatedThisTick = true;
                                deterministicSpawnOnScreenTopNext = false;
                            }
                        }
                    } else if (!deterministicSpawnOnScreenTopNext && allowSpawnForScreenBottom) {
                        int initialLocalLane0 = determineInitialLocalLane(0);
                        if (isSpawnPointClear(0, initialLocalLane0, existingCars)) {
                            carForScreenBottom = createNewCar(0, initialLocalLane0);
                            if (carForScreenBottom != null) {
                                carCreatedThisTick = true;
                                deterministicSpawnOnScreenTopNext = true;
                            }
                        }
                    } else {
                        if (allowSpawnForScreenBottom && deterministicSpawnOnScreenTopNext) {
                            int initialLocalLane0 = determineInitialLocalLane(0);
                            if (isSpawnPointClear(0, initialLocalLane0, existingCars)) {
                                carForScreenBottom = createNewCar(0, initialLocalLane0);
                                if (carForScreenBottom != null) {
                                    carCreatedThisTick = true;
                                    // deterministicSpawnOnScreenTopNext не меняем, т.к. это была "компенсация"
                                }
                            }
                        } else if (allowSpawnForScreenTop && !deterministicSpawnOnScreenTopNext) {
                            int initialLocalLane1 = determineInitialLocalLane(1);
                            if (isSpawnPointClear(1, initialLocalLane1, existingCars)) {
                                carForScreenTop = createNewCar(1, initialLocalLane1);
                                if (carForScreenTop != null) {
                                    carCreatedThisTick = true;
                                    // deterministicSpawnOnScreenTopNext не меняем
                                }
                            }
                        }
                    }
                } else if (road.getNumberOfDirections() == 1) {
                    if (allowSpawnForScreenBottom) {
                        int initialLocalLane0 = determineInitialLocalLane(0);
                        if (isSpawnPointClear(0, initialLocalLane0, existingCars)) {
                            carForScreenBottom = createNewCar(0, initialLocalLane0);
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

    // Новый метод для определения начальной локальной полосы
    private int determineInitialLocalLane(int modelDirection) {
        int lanesPerDir = params.getLanesPerDirection();
        if (lanesPerDir <= 1) {
            return 0; // Если одна полоса, всегда 0
        }

        double generatedSpeedKmh = generateInitialSpeedKmhBasedOnSettings(); // Генерируем скорость, чтобы решить
        RoadType roadType = road.getType();
        double roadDefaultSpeedKmh = roadType.getDefaultSpeedLimitKmh();
        double roadMaxSpeedKmh = roadType.getMaxSpeedLimitKmh();

        // Локальный индекс: 0 = правая, lanesPerDir-1 = левая
        if (generatedSpeedKmh >= roadMaxSpeedKmh * 0.9) { // Очень быстрые -> на левую
            return lanesPerDir - 1;
        } else if (generatedSpeedKmh > roadDefaultSpeedKmh * 1.1 && lanesPerDir > 2) { // Быстрее среднего и есть >2 полос -> средняя/левая
            // Для 3 полос: 0 (прав), 1 (сред), 2 (лев). Вернем 1 или 2.
            // Для 4 полос: 0 (прав), 1 (сред-прав), 2 (сред-лев), 3 (лев). Вернем 2 или 3.
            return random.nextInt(lanesPerDir - 1) + 1; // Случайная из левых, кроме самой правой
        } else if (generatedSpeedKmh > roadDefaultSpeedKmh * 1.05 && lanesPerDir > 1) { // Чуть быстрее среднего и есть левая
            return Math.min(1, lanesPerDir - 1); // Вторая справа (локальная 1), если есть
        } else { // Медленные или средние
            return 0; // Крайняя правая
        }
    }

    private boolean isSpawnPointClear(int modelDirection, int targetLocalLaneIndex, List<Car> existingCars) {
        // ... (без изменений) ...
        if (existingCars == null || road == null) return true;
        for (Car car : existingCars) {
            if (car.getDirection() == modelDirection && car.getCurrentLaneIndex() == targetLocalLaneIndex) {
                if (modelDirection == 0) {
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
        // ... (без изменений) ...
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
        // ... (без изменений) ...
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

    private Car createNewCar(int modelDirection, int targetLocalLaneIndex) {
        // ... (без изменений, кроме, возможно, логирования, если нужно видеть выбранную начальную полосу) ...
        double initialPosition = (modelDirection == 0) ? 0.0 : road.getLength();
        RoadType currentRoadType = road.getType();
        // Скорость генерируется один раз и используется для определения полосы, а затем для машины
        double generatedSpeedKmh = generateInitialSpeedKmhBasedOnSettings();

        double minRoadTypeSpeedKmh = currentRoadType.getMinSpeedLimitKmh();
        double maxRoadTypeSpeedKmh = currentRoadType.getMaxSpeedLimitKmh();
        double initialSpeedKmh = Math.max(minRoadTypeSpeedKmh, Math.min(generatedSpeedKmh, maxRoadTypeSpeedKmh));

        if (DETAILED_LOGGING) {
            String screenDir = (modelDirection == 1) ? "ScreenTop (R->L, model_dir 1)" : "ScreenBottom (L->R, model_dir 0)";
            System.out.printf(">>> CREATE CAR for %s: DeterminedInitialLocalLane: %d, InitPos: %.1f, FinalInitialSpeed:%.1f km/h (GeneratedSpeed: %.1f)%n",
                    screenDir, targetLocalLaneIndex, initialPosition, initialSpeedKmh, generatedSpeedKmh);
        }

        double initialSpeedMs = initialSpeedKmh / 3.6;
        double maxCarSpeedKmh = Math.min(initialSpeedKmh * (1.05 + random.nextDouble() * 0.20), maxRoadTypeSpeedKmh);
        maxCarSpeedKmh = Math.max(initialSpeedKmh, maxCarSpeedKmh);
        maxCarSpeedKmh = Math.min(maxCarSpeedKmh, currentRoadType.getMaxSpeedLimitKmh());
        double maxSpeedMs = maxCarSpeedKmh / 3.6;
        double accelParam = 1.5 + random.nextDouble() * 1.0;
        double decelParam = 2.0 + random.nextDouble() * 1.5;

        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, accelParam, decelParam, targetLocalLaneIndex, modelDirection);
    }

    public void updateParameters(SimulationParameters newParams) {
        if (DETAILED_LOGGING) System.out.println("TrafficFlowGenerator.updateParameters -> сброс таймеров генерации.");
        resetGenerationTimers();
    }
}
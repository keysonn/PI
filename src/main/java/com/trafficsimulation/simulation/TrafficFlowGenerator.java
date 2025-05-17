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

    // Таймеры для ЭКРАННЫХ потоков
    private double timeToNextCarScreenTop = 0.0;    // Для потока, который на экране сверху (едет R->L, соответствует model_direction=1)
    private double timeToNextCarScreenBottom = 0.0; // Для потока, который на экране снизу (едет L->R, соответствует model_direction=0)
    private double timeSinceLastDeterministicCar = 0.0;

    private static final double MIN_SPAWN_CLEARANCE_M = 7.0;
    private static final boolean DETAILED_LOGGING = true;


    public TrafficFlowGenerator(SimulationParameters params, Road road) {
        this.params = params;
        this.road = road;
        this.random = new Random(System.currentTimeMillis());
        resetGenerationTimers();
        if (DETAILED_LOGGING) {
            System.out.println("TrafficFlowGenerator создан. Тип дороги: " + road.getType() +
                    ", Направления (модель): " + params.getNumberOfDirections() +
                    ", Полос на направление: " + params.getLanesPerDirection());
        }
    }

    private void resetGenerationTimers() {
        timeSinceLastDeterministicCar = 0.0;

        if (params.isRandomTimeFlow()) {
            // Поток сверху экрана (R->L) использует model_direction = 1
            this.timeToNextCarScreenTop = generateNextRandomInterval();
            if (DETAILED_LOGGING) System.out.println("  TFG: Random Timer for ScreenTop (model_dir 1) initialized to: " + String.format("%.2f", timeToNextCarScreenTop));

            // Поток снизу экрана (L->R) использует model_direction = 0
            // Генерируем для него, только если дорога двухсторонняя в модели ИЛИ это односторонняя (которая у нас model_dir 0)
            if ((params.getNumberOfDirections() == 2 && road.getType() != RoadType.TUNNEL) ||
                    (params.getNumberOfDirections() == 1 && road.getType() != RoadType.TUNNEL)) { // Односторонняя тоже использует этот таймер
                this.timeToNextCarScreenBottom = generateNextRandomInterval();
                if (DETAILED_LOGGING) System.out.println("  TFG: Random Timer for ScreenBottom (model_dir 0) initialized to: " + String.format("%.2f", timeToNextCarScreenBottom));
                if (params.getNumberOfDirections() == 1) { // Если односторонняя, то верхний таймер не нужен
                    this.timeToNextCarScreenTop = Double.POSITIVE_INFINITY;
                    if (DETAILED_LOGGING) System.out.println("    (Single direction road, ScreenTop timer set to INF)");
                }
            } else { // Тоннель или невалидная конфигурация
                this.timeToNextCarScreenBottom = Double.POSITIVE_INFINITY;
                if (DETAILED_LOGGING && params.getNumberOfDirections() == 2) System.out.println("  TFG: ScreenBottom (model_dir 0) timer for Tunnel set to INF (handled by tunnel logic).");

            }
        } else { // Детерминированный поток
            this.timeToNextCarScreenTop = Double.POSITIVE_INFINITY;
            this.timeToNextCarScreenBottom = Double.POSITIVE_INFINITY;
            if (DETAILED_LOGGING) System.out.println("  TFG: Deterministic time flow. Random timers set to INFINITY.");
        }
    }

    public Car[] generateCars(double deltaTime, TunnelControlState tunnelState) {
        Car carForScreenTop = null;    // Едет R->L на экране (model_dir=1)
        Car carForScreenBottom = null; // Едет L->R на экране (model_dir=0)
        List<Car> existingCars = road.getCars();

        // Разрешения на спавн для ЭКРАННЫХ потоков
        boolean allowSpawnForScreenTop;    // Поток сверху (R->L), model_dir=1
        boolean allowSpawnForScreenBottom; // Поток снизу (L->R), model_dir=0

        if (road.getType() == RoadType.TUNNEL && tunnelState != null) {
            // В тоннеле:
            // DIR0_GREEN (model_dir 0) -> зеленый для потока ScreenBottom (L->R)
            // DIR1_GREEN (model_dir 1) -> зеленый для потока ScreenTop (R->L)
            allowSpawnForScreenBottom = (tunnelState == TunnelControlState.DIR0_GREEN);
            allowSpawnForScreenTop = (tunnelState == TunnelControlState.DIR1_GREEN);
        } else if (road.getNumberOfDirections() == 1) {
            // Односторонняя дорога (в нашей модели всегда model_dir=0, едет L->R, рисуется снизу)
            allowSpawnForScreenTop = false;
            allowSpawnForScreenBottom = true;
        } else { // Двухсторонняя не-тоннель
            allowSpawnForScreenTop = true;
            allowSpawnForScreenBottom = true;
        }

        // Локальный индекс для генерации (0 = крайняя правая для СВОЕГО направления)
        int targetLocalLaneIndex = 0;

        if (params.isRandomTimeFlow()) {
            // --- Поток сверху экрана (model_dir=1, едет R->L) ---
            if (allowSpawnForScreenTop) {
                timeToNextCarScreenTop -= deltaTime;
                if (timeToNextCarScreenTop <= 0) {
                    if (isSpawnPointClear(1, targetLocalLaneIndex, existingCars)) { // model_dir=1
                        carForScreenTop = createNewCar(1, targetLocalLaneIndex); // model_dir=1
                        double nextInterval = generateNextRandomInterval();
                        timeToNextCarScreenTop = nextInterval + timeToNextCarScreenTop; // Сброс таймера
                        // if (DETAILED_LOGGING && carForScreenTop != null) System.out.println("RandomTime Car ScreenTop (model_dir 1, locLane " + targetLocalLaneIndex +") Spawned. Next in: " + String.format("%.2f",timeToNextCarScreenTop));
                    } else {
                        timeToNextCarScreenTop = Math.max(0.1, timeToNextCarScreenTop + 0.1); // Небольшая задержка
                    }
                }
            }

            // --- Поток снизу экрана (model_dir=0, едет L->R) ---
            if (allowSpawnForScreenBottom) {
                timeToNextCarScreenBottom -= deltaTime;
                if (timeToNextCarScreenBottom <= 0) {
                    if (isSpawnPointClear(0, targetLocalLaneIndex, existingCars)) { // model_dir=0
                        carForScreenBottom = createNewCar(0, targetLocalLaneIndex); // model_dir=0
                        double nextInterval = generateNextRandomInterval();
                        timeToNextCarScreenBottom = nextInterval + timeToNextCarScreenBottom; // Сброс таймера
                        // if (DETAILED_LOGGING && carForScreenBottom != null) System.out.println("RandomTime Car ScreenBottom (model_dir 0, locLane " + targetLocalLaneIndex +") Spawned. Next in: " + String.format("%.2f",timeToNextCarScreenBottom));
                    } else {
                        timeToNextCarScreenBottom = Math.max(0.1, timeToNextCarScreenBottom + 0.1); // Небольшая задержка
                    }
                }
            }
        } else { // Детерминированное ВРЕМЯ появления
            timeSinceLastDeterministicCar += deltaTime;
            double requiredInterval = params.getDeterministicIntervalSeconds();

            if (requiredInterval > 0 && timeSinceLastDeterministicCar >= requiredInterval) {
                boolean carCreatedOnThisTick = false;
                // Попытка для ScreenTop (model_dir=1)
                if (allowSpawnForScreenTop) {
                    if (isSpawnPointClear(1, targetLocalLaneIndex, existingCars)) {
                        carForScreenTop = createNewCar(1, targetLocalLaneIndex);
                        if (carForScreenTop != null) carCreatedOnThisTick = true;
                    }
                }
                // Попытка для ScreenBottom (model_dir=0)
                // Если двухсторонняя, пытаемся создать и для второго потока, если первый не создался ИЛИ если первый создался (но тогда сбрасываем таймер только один раз)
                // Если односторонняя, то carForScreenTop будет null и allowSpawnForScreenTop=false, так что всегда попытаемся для ScreenBottom
                if (allowSpawnForScreenBottom) {
                    if (road.getNumberOfDirections() == 2) { // Для двухсторонней
                        if (carForScreenTop == null || !carCreatedOnThisTick) { // Если для верхнего не создали, или создали, но хотим и для нижнего
                            if (isSpawnPointClear(0, targetLocalLaneIndex, existingCars)) {
                                carForScreenBottom = createNewCar(0, targetLocalLaneIndex);
                                if (carForScreenBottom != null) carCreatedOnThisTick = true; // Учитываем общее создание
                            }
                        }
                    } else { // Односторонняя (всегда ScreenBottom)
                        if (isSpawnPointClear(0, targetLocalLaneIndex, existingCars)) {
                            carForScreenBottom = createNewCar(0, targetLocalLaneIndex);
                            if (carForScreenBottom != null) carCreatedOnThisTick = true;
                        }
                    }
                }

                if (carCreatedOnThisTick) { // Если хотя бы одна машина создана
                    timeSinceLastDeterministicCar -= requiredInterval; // Сбрасываем таймер
                } else if (allowSpawnForScreenTop || allowSpawnForScreenBottom) { // Если должны были, но не смогли
                    timeSinceLastDeterministicCar = Math.max(0, requiredInterval - 0.1); // Немного откладываем
                }
            }
        }

        if (carForScreenTop != null && carForScreenBottom != null) return new Car[]{carForScreenTop, carForScreenBottom};
        if (carForScreenTop != null) return new Car[]{carForScreenTop};
        if (carForScreenBottom != null) return new Car[]{carForScreenBottom};
        return null;
    }

    // modelDirection: 0 для L->R в модели (ScreenBottom), 1 для R->L в модели (ScreenTop)
    // targetLocalLaneIndex - ЛОКАЛЬНЫЙ индекс полосы (0 = правая, .. N-1 = левая)
    private boolean isSpawnPointClear(int modelDirection, int targetLocalLaneIndex, List<Car> existingCars) {
        if (existingCars == null || road == null) return true;
        for (Car car : existingCars) {
            if (car.getDirection() == modelDirection && car.getLaneIndex() == targetLocalLaneIndex) {
                if (modelDirection == 0) { // Модельное L->R (ScreenBottom), спавн в x=0
                    if (car.getPosition() < MIN_SPAWN_CLEARANCE_M) {
                        // if (DETAILED_LOGGING) System.out.println("Spawn ScreenBottom (model_dir 0), local lane " + targetLocalLaneIndex + " BLOCKED by car " + car.getId() + " at " + String.format("%.1f",car.getPosition()));
                        return false;
                    }
                } else { // modelDirection == 1. Модельное R->L (ScreenTop), спавн в x=road.getLength()
                    if ((road.getLength() - car.getPosition()) < MIN_SPAWN_CLEARANCE_M) {
                        // if (DETAILED_LOGGING) System.out.println("Spawn ScreenTop (model_dir 1), local lane " + targetLocalLaneIndex + " BLOCKED by car " + car.getId() + " at " + String.format("%.1f",car.getPosition()));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private double generateNextRandomInterval() {
        double interval = 10.0; // Значение по умолчанию, если что-то пойдет не так
        DistributionLaw law = params.getTimeDistributionLaw();
        if (law == null) law = DistributionLaw.NORMAL;

        switch (law) {
            case UNIFORM:
                interval = params.getTimeUniformMinSec() +
                        (params.getTimeUniformMaxSec() - params.getTimeUniformMinSec()) * random.nextDouble();
                break;
            case NORMAL:
                double sigmaTime = Math.sqrt(Math.max(0.001, params.getTimeNormalVarianceSec())); // Дисперсия не может быть <0
                interval = params.getTimeNormalMeanSec() + sigmaTime * random.nextGaussian();
                break;
            case EXPONENTIAL:
                double intensityTime = params.getTimeExponentialIntensityPerSec();
                if (intensityTime <= 1e-5) return Double.POSITIVE_INFINITY;
                double meanExpInterval = 1.0 / intensityTime;
                interval = -meanExpInterval * Math.log(Math.max(1e-9, 1.0 - random.nextDouble())); // Защита от log(0) или log(1)
                break;
        }
        return Math.max(0.1, interval); // Минимальный интервал для предотвращения слишком частой генерации
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
        return Math.max(10.0, Math.min(speedKmh, 150.0)); // Общее ограничение генерируемой скорости
    }

    // modelDirection: 0 для L->R в модели (ScreenBottom), 1 для R->L в модели (ScreenTop)
    // targetLocalLaneIndex - ЛОКАЛЬНЫЙ индекс полосы (0 = правая, ... N-1 = левая)
    private Car createNewCar(int modelDirection, int targetLocalLaneIndex) {
        double initialPosition = (modelDirection == 0) ? 0.0 : road.getLength();
        RoadType currentRoadType = road.getType();
        double generatedSpeedKmh = generateInitialSpeedKmhBasedOnSettings();
        double minRoadTypeSpeedKmh = currentRoadType.getMinSpeedLimitKmh();
        double maxRoadTypeSpeedKmh = currentRoadType.getMaxSpeedLimitKmh();
        double initialSpeedKmh = Math.max(minRoadTypeSpeedKmh, Math.min(generatedSpeedKmh, maxRoadTypeSpeedKmh));

        if (DETAILED_LOGGING) {
            String screenDir = (modelDirection == 1) ? "ScreenTop (R->L, model_dir 1)" : "ScreenBottom (L->R, model_dir 0)";
            System.out.printf(">>> CREATE CAR for %s: LocalLane: %d, InitPos: %.1f, FinalInitialSpeed:%.1f km/h%n",
                    screenDir, targetLocalLaneIndex, initialPosition, initialSpeedKmh);
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
        // this.params = newParams; // Не нужно, params - это ссылка
        if (DETAILED_LOGGING) System.out.println("TrafficFlowGenerator.updateParameters -> сброс таймеров генерации.");
        resetGenerationTimers();
    }
}
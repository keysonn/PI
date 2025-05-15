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
    private static final int MAX_SPAWN_LANE_ATTEMPTS = 3; // Попытки найти свободную полосу для детерминированного потока
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
        timeSinceLastDeterministicCar = 0.0; // Сбрасываем таймер для детерминированного потока
        if (params.isRandomTimeFlow()) {
            this.timeToNextRandomCarDir0 = generateNextRandomInterval();
            // Для второго направления таймер инициализируется, только если оно есть и это не тоннель
            // (для тоннеля генерация управляется tunnelState)
            if (params.getNumberOfDirections() == 2 && road.getType() != RoadType.TUNNEL) {
                this.timeToNextRandomCarDir1 = generateNextRandomInterval();
            } else {
                this.timeToNextRandomCarDir1 = Double.POSITIVE_INFINITY; // Нет генерации для второго направления
            }
        } else {
            // Если поток времени детерминированный, таймеры случайной генерации не используются
            this.timeToNextRandomCarDir0 = Double.POSITIVE_INFINITY;
            this.timeToNextRandomCarDir1 = Double.POSITIVE_INFINITY;
        }
        if (DETAILED_LOGGING) {
            System.out.println("  TrafficFlowGenerator: Таймеры сброшены. Dir0 next car in: " + String.format("%.2f", timeToNextRandomCarDir0) +
                    ", Dir1 next car in: " + String.format("%.2f", timeToNextRandomCarDir1));
        }
    }

    public Car[] generateCars(double deltaTime, TunnelControlState tunnelState) {
        Car carDir0 = null;
        Car carDir1 = null;
        List<Car> existingCars = road.getCars(); // Получаем текущие машины для проверки клиренса

        // Определяем, разрешен ли спавн для каждого направления
        boolean allowSpawnDir0 = true;
        boolean allowSpawnDir1 = (road.getNumberOfDirections() == 2); // По умолчанию разрешен, если есть второе направление

        if (road.getType() == RoadType.TUNNEL && tunnelState != null) {
            allowSpawnDir0 = (tunnelState == TunnelControlState.DIR0_GREEN);
            allowSpawnDir1 = (tunnelState == TunnelControlState.DIR1_GREEN);
            if (DETAILED_LOGGING && (allowSpawnDir0 || allowSpawnDir1)) {
                // System.out.println("Tunnel State: " + tunnelState + ", Allow0: " + allowSpawnDir0 + ", Allow1: " + allowSpawnDir1);
            }
        }


        if (params.isRandomTimeFlow()) { // Случайное ВРЕМЯ появления
            if (allowSpawnDir0) {
                timeToNextRandomCarDir0 -= deltaTime;
                if (timeToNextRandomCarDir0 <= 0) {
                    int targetLaneDir0 = random.nextInt(params.getLanesPerDirection()); // Локальный индекс полосы 0, 1...
                    if (isSpawnPointClear(0, targetLaneDir0, existingCars)) {
                        carDir0 = createNewCar(0, targetLaneDir0);
                        double nextInterval = generateNextRandomInterval();
                        timeToNextRandomCarDir0 = nextInterval + timeToNextRandomCarDir0; // Добавляем остаток, чтобы не терять точность
                        if (DETAILED_LOGGING && carDir0 != null) System.out.println("RandomTime Car Dir0 Spawned (Lane " + targetLaneDir0 +"). Next in: " + String.format("%.2f",timeToNextRandomCarDir0) + "s (Interval: "+ String.format("%.2f",nextInterval) +")");
                    } else {
                        timeToNextRandomCarDir0 = Math.max(0.1, timeToNextRandomCarDir0 + 0.1); // Небольшая задержка, если место занято
                        // if (DETAILED_LOGGING) System.out.println("Spawn Dir0 clear check failed (Random). Deferring. Next try in: " + String.format("%.2f",timeToNextRandomCarDir0) + "s");
                    }
                }
            }

            if (allowSpawnDir1) {
                timeToNextRandomCarDir1 -= deltaTime;
                if (timeToNextRandomCarDir1 <= 0) {
                    int localLaneIndex = random.nextInt(params.getLanesPerDirection());
                    int globalLaneIndexDir1 = params.getLanesPerDirection() + localLaneIndex; // Глобальный индекс полосы
                    if (isSpawnPointClear(1, globalLaneIndexDir1, existingCars)) {
                        carDir1 = createNewCar(1, globalLaneIndexDir1);
                        double nextInterval = generateNextRandomInterval();
                        timeToNextRandomCarDir1 = nextInterval + timeToNextRandomCarDir1;
                        if (DETAILED_LOGGING && carDir1 != null) System.out.println("RandomTime Car Dir1 Spawned (Lane " + globalLaneIndexDir1 + "). Next in: " + String.format("%.2f",timeToNextRandomCarDir1) + "s (Interval: "+ String.format("%.2f",nextInterval) +")");
                    } else {
                        timeToNextRandomCarDir1 = Math.max(0.1, timeToNextRandomCarDir1 + 0.1);
                        // if (DETAILED_LOGGING) System.out.println("Spawn Dir1 clear check failed (Random). Deferring. Next try in: " + String.format("%.2f",timeToNextRandomCarDir1) + "s");
                    }
                }
            }
        } else { // Детерминированное ВРЕМЯ появления
            timeSinceLastDeterministicCar += deltaTime;
            double requiredInterval = params.getDeterministicIntervalSeconds();

            if (requiredInterval > 0 && timeSinceLastDeterministicCar >= requiredInterval) {
                boolean carCreatedThisTick = false; // Флаг, что хотя бы одна машина создана в этот тик
                if (allowSpawnDir0) {
                    for (int attempt = 0; attempt < MAX_SPAWN_LANE_ATTEMPTS && carDir0 == null; attempt++) {
                        int targetLaneDir0Det = random.nextInt(params.getLanesPerDirection());
                        if (isSpawnPointClear(0, targetLaneDir0Det, existingCars)) {
                            carDir0 = createNewCar(0, targetLaneDir0Det);
                            if (DETAILED_LOGGING && carDir0 != null) System.out.println("DeterministicTime Car Dir0 Spawned (Lane " + targetLaneDir0Det + ")");
                            carCreatedThisTick = true;
                            break; // Выходим из попыток, если успешно
                        }
                    }
                }
                if (allowSpawnDir1) {
                    for (int attempt = 0; attempt < MAX_SPAWN_LANE_ATTEMPTS && carDir1 == null; attempt++) {
                        int localLaneIndex = random.nextInt(params.getLanesPerDirection());
                        int targetLaneDir1Det = params.getLanesPerDirection() + localLaneIndex;
                        if (isSpawnPointClear(1, targetLaneDir1Det, existingCars)) {
                            carDir1 = createNewCar(1, targetLaneDir1Det);
                            if (DETAILED_LOGGING && carDir1 != null) System.out.println("DeterministicTime Car Dir1 Spawned (Lane " + targetLaneDir1Det + ")");
                            carCreatedThisTick = true;
                            break;
                        }
                    }
                }

                if (carCreatedThisTick) {
                    timeSinceLastDeterministicCar -= requiredInterval; // Сбрасываем таймер кратно интервалу
                } else if (allowSpawnDir0 || allowSpawnDir1) {
                    // Если должны были, но не смогли (все полосы заняты), не сбрасываем полностью,
                    // а даем шанс в ближайшее время.
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
        // double spawnPosition = (direction == 0) ? 0.0 : road.getLength(); // Не используется напрямую
        for (Car car : existingCars) {
            // Проверяем только машины на той же полосе и в том же направлении
            if (car.getLaneIndex() == targetGlobalLaneIndex && car.getDirection() == direction) {
                if (direction == 0) { // Машина появляется на 0, едет вправо
                    if (car.getPosition() < MIN_SPAWN_CLEARANCE_M) { // Если существующая машина близко к началу
                        return false;
                    }
                } else { // Машина появляется на road.getLength(), едет влево
                    if ((road.getLength() - car.getPosition()) < MIN_SPAWN_CLEARANCE_M) { // Если существующая машина близко к концу (с ее стороны)
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private double generateNextRandomInterval() {
        double interval = 10.0; // Дефолтное значение на случай ошибки
        DistributionLaw law = params.getTimeDistributionLaw();
        if (law == null) law = DistributionLaw.NORMAL; // Безопасное значение по умолчанию

        switch (law) {
            case UNIFORM:
                interval = params.getTimeUniformMinSec() +
                        (params.getTimeUniformMaxSec() - params.getTimeUniformMinSec()) * random.nextDouble();
                break;
            case NORMAL:
                double sigmaTime = Math.sqrt(Math.max(0.001, params.getTimeNormalVarianceSec())); // Дисперсия не < 0
                interval = params.getTimeNormalMeanSec() + sigmaTime * random.nextGaussian();
                break;
            case EXPONENTIAL:
                double intensityTime = params.getTimeExponentialIntensityPerSec();
                if (intensityTime <= 1e-5) return Double.POSITIVE_INFINITY; // Избегаем деления на ноль
                double meanExpInterval = 1.0 / intensityTime;
                interval = -meanExpInterval * Math.log(Math.max(1e-9, 1.0 - random.nextDouble())); // Избегаем log(0) и log(1)
                break;
        }
        return Math.max(0.1, interval); // Минимальный интервал, чтобы не было слишком часто
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
                        speedKmh = params.getDeterministicSpeedKmh(); // Fallback
                    } else {
                        double meanExpSpeed = 1.0 / intensitySpeed;
                        speedKmh = -meanExpSpeed * Math.log(Math.max(1e-9, 1.0 - random.nextDouble()));
                    }
                    break;
                default:
                    speedKmh = params.getDeterministicSpeedKmh(); // Fallback
                    break;
            }
        } else {
            speedKmh = params.getDeterministicSpeedKmh();
        }
        // Это предварительная скорость, она будет скорректирована по типу дороги
        return Math.max(10.0, Math.min(speedKmh, 150.0)); // Общий разумный диапазон
    }

    private Car createNewCar(int direction, int targetGlobalLaneIndex) {
        double initialPosition = (direction == 0) ? 0.0 : road.getLength();
        RoadType currentRoadType = road.getType();

        double generatedSpeedKmh = generateInitialSpeedKmhBasedOnSettings();

        // Корректируем сгенерированную скорость в соответствии с лимитами типа дороги
        double minRoadTypeSpeedKmh = currentRoadType.getMinSpeedLimitKmh();
        double maxRoadTypeSpeedKmh = currentRoadType.getMaxSpeedLimitKmh();

        double initialSpeedKmh = Math.max(minRoadTypeSpeedKmh, Math.min(generatedSpeedKmh, maxRoadTypeSpeedKmh));

        if (DETAILED_LOGGING) {
            System.out.printf("  Car (dir %d, lane %d, type %s): GeneratedRawSpeed=%.1f, RoadLimits[%.1f-%.1f], FinalInitialSpeed=%.1f km/h%n",
                    direction, targetGlobalLaneIndex, currentRoadType, generatedSpeedKmh, minRoadTypeSpeedKmh, maxRoadTypeSpeedKmh, initialSpeedKmh);
        }

        double initialSpeedMs = initialSpeedKmh / 3.6;

        // Максимальная скорость машины также должна быть в пределах лимитов дороги,
        // но может быть немного выше начальной (например, если машина хочет ускориться)
        // Убедимся, что maxCarSpeedKmh не выходит за пределы maxRoadTypeSpeedKmh
        double maxCarSpeedKmh = Math.min(initialSpeedKmh * (1.05 + random.nextDouble() * 0.20), maxRoadTypeSpeedKmh); // Разброс 5-25% от начальной
        maxCarSpeedKmh = Math.max(initialSpeedKmh, maxCarSpeedKmh); // Не меньше начальной
        maxCarSpeedKmh = Math.min(maxCarSpeedKmh, currentRoadType.getMaxSpeedLimitKmh()); // И не больше лимита дороги

        double maxSpeedMs = maxCarSpeedKmh / 3.6;

        // Параметры ускорения/замедления (можно сделать более разнообразными)
        double accelParam = 1.5 + random.nextDouble() * 1.0;  // [1.5 - 2.5] м/с^2
        double decelParam = 2.0 + random.nextDouble() * 1.5;  // [2.0 - 3.5] м/с^2

        return new Car(initialPosition, initialSpeedMs, maxSpeedMs, accelParam, decelParam, targetGlobalLaneIndex, direction);
    }

    public void updateParameters(SimulationParameters newParams) {
        // this.params = newParams; // Если params - это ссылка на тот же объект, что и в SimulationEngine, это не нужно
        if (DETAILED_LOGGING) System.out.println("TrafficFlowGenerator.updateParameters -> сброс таймеров генерации. Новый тип дороги: " + road.getType());
        resetGenerationTimers(); // Переинициализируем таймеры на основе текущих (уже обновленных) params
    }
}
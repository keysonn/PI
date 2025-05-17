package com.trafficsimulation.simulation;

import com.trafficsimulation.gui.SimulationPanel;
import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadSign;
import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimulationEngine implements Runnable {

    private SimulationParameters parameters;
    private Road road;
    private TrafficFlowGenerator flowGenerator;
    private SimulationPanel simulationPanel;

    private volatile boolean running = false;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();

    private double simulationTime = 0.0;

    private TunnelControlState tunnelControlState;
    private double tunnelPhaseTimer;
    private TrafficLight tunnelLightDir0;
    private TrafficLight tunnelLightDir1;
    private boolean isTunnelActive = false;

    private static final boolean ENGINE_DEBUG_LOGGING = true;
    private static final double LANE_CHANGE_CHECK_INTERVAL = 0.6; // Немного увеличим интервал проверки
    private double timeSinceLastLaneChangeCheck = 0.0;


    public SimulationEngine(SimulationParameters params, SimulationPanel panel) {
        this.parameters = params;
        this.simulationPanel = panel;
        initializeSimulation();
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Конструктор выполнен, initializeSimulation() вызвана.");
    }

    public void initializeSimulation() {
        // ... (код без изменений, как в предыдущем полном ответе) ...
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Начало initializeSimulation(). Параметры: " + parameters.getRoadType());
        isTunnelActive = (parameters.getRoadType() == RoadType.TUNNEL);

        if (isTunnelActive) {
            if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Тип дороги - Тоннель. Установка параметров для тоннеля.");
            parameters.setNumberOfDirections(2);
            parameters.setLanesPerDirection(1);
        }

        this.road = new Road(
                parameters.getRoadLengthKm(),
                parameters.getRoadType(),
                parameters.getLanesPerDirection(),
                parameters.getNumberOfDirections()
        );
        this.flowGenerator = new TrafficFlowGenerator(parameters, this.road);
        this.simulationTime = 0.0;
        this.timeSinceLastLaneChangeCheck = 0.0;
        this.tunnelLightDir0 = null;
        this.tunnelLightDir1 = null;

        if (this.road != null && this.road.getTrafficLights() != null && !this.road.getTrafficLights().isEmpty()) {
            if (isTunnelActive) {
                this.road.clearTrafficLights();
            }
        }
        if (this.road != null && isTunnelActive && this.road.getRoadSigns() != null) {
            this.road.clearRoadSigns();
        }

        if (isTunnelActive) {
            if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Создание светофоров для тоннеля.");
            double roadModelLength = this.road.getLength();
            double leftLightPosition = roadModelLength * 0.25;
            double rightLightPosition = roadModelLength * 0.75;

            tunnelLightDir0 = new TrafficLight(leftLightPosition, parameters.getTunnelDefaultRedDuration(), parameters.getTunnelDefaultGreenDuration(), TrafficLightState.GREEN, 0);
            tunnelLightDir0.setExternallyControlled(true);
            this.road.addTrafficLight(tunnelLightDir0);

            tunnelLightDir1 = new TrafficLight(rightLightPosition, parameters.getTunnelDefaultRedDuration(), parameters.getTunnelDefaultGreenDuration(), TrafficLightState.RED, 1);
            tunnelLightDir1.setExternallyControlled(true);
            this.road.addTrafficLight(tunnelLightDir1);

            tunnelControlState = TunnelControlState.DIR0_GREEN;
            tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();

            if (ENGINE_DEBUG_LOGGING) {
                System.out.println("SimulationEngine: Тоннель активирован.");
                System.out.println("  Длина дороги (модель): " + String.format("%.1f", roadModelLength) + "м");
                System.out.println("  Левый светофор (Dir0) на поз.: " + String.format("%.1f", leftLightPosition) + "м (ID: " + tunnelLightDir0.getId() + ")");
                System.out.println("  Правый светофор (Dir1) на поз.: " + String.format("%.1f", rightLightPosition) + "м (ID: " + tunnelLightDir1.getId() + ")");
                System.out.println("  Начальное состояние: " + tunnelControlState + ", таймер фазы: " + String.format("%.2f", tunnelPhaseTimer) + "с");
            }
        }
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Конец initializeSimulation(). Дорога: " + this.road);
        if (simulationPanel != null) {
            simulationPanel.updateSimulationState(this.road, this.simulationTime);
        }
    }

    private void updateTunnelLogic(double deltaTime) {
        // ... (код без изменений) ...
        if (!isTunnelActive || tunnelLightDir0 == null || tunnelLightDir1 == null) return;
        tunnelPhaseTimer -= deltaTime;
        tunnelLightDir0.update(deltaTime);
        tunnelLightDir1.update(deltaTime);

        if (tunnelPhaseTimer <= 0) {
            if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: Таймер фазы истек ("+String.format("%.2f", tunnelPhaseTimer)+"). Текущее состояние: " + tunnelControlState);
            switch (tunnelControlState) {
                case DIR0_GREEN:
                    tunnelControlState = TunnelControlState.DIR0_CLEARING;
                    tunnelPhaseTimer = parameters.getTunnelClearanceTime();
                    tunnelLightDir0.setCurrentState(TrafficLightState.RED, true);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: -> DIR0_CLEARING. Таймер: " + String.format("%.2f", tunnelPhaseTimer) + "с. Св0->Красный.");
                    break;
                case DIR0_CLEARING:
                    boolean carsInDir0 = areCarsInTunnel(0);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: Проверка DIR0_CLEARING. Машины напр.0 в тоннеле: " + carsInDir0 + ". Осталось времени очистки: " + String.format("%.2f", tunnelPhaseTimer));
                    if (carsInDir0 && tunnelPhaseTimer > -2.0) {
                        tunnelPhaseTimer = Math.max(tunnelPhaseTimer, 0.2);
                        break;
                    }
                    tunnelControlState = TunnelControlState.DIR1_GREEN;
                    tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
                    tunnelLightDir1.setCurrentState(TrafficLightState.GREEN, true);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: -> DIR1_GREEN. Таймер: " + String.format("%.2f", tunnelPhaseTimer) + "с. Св1->Зеленый.");
                    break;
                case DIR1_GREEN:
                    tunnelControlState = TunnelControlState.DIR1_CLEARING;
                    tunnelPhaseTimer = parameters.getTunnelClearanceTime();
                    tunnelLightDir1.setCurrentState(TrafficLightState.RED, true);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: -> DIR1_CLEARING. Таймер: " + String.format("%.2f", tunnelPhaseTimer) + "с. Св1->Красный.");
                    break;
                case DIR1_CLEARING:
                    boolean carsInDir1 = areCarsInTunnel(1);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: Проверка DIR1_CLEARING. Машины напр.1 в тоннеле: " + carsInDir1 + ". Осталось времени очистки: " + String.format("%.2f", tunnelPhaseTimer));
                    if (carsInDir1 && tunnelPhaseTimer > -2.0) {
                        tunnelPhaseTimer = Math.max(tunnelPhaseTimer, 0.2);
                        break;
                    }
                    tunnelControlState = TunnelControlState.DIR0_GREEN;
                    tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
                    tunnelLightDir0.setCurrentState(TrafficLightState.GREEN, true);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: -> DIR0_GREEN. Таймер: " + String.format("%.2f", tunnelPhaseTimer) + "с. Св0->Зеленый.");
                    break;
            }
        }
    }

    private boolean areCarsInTunnel(int direction) {
        // ... (код без изменений) ...
        if (road == null || road.getCars() == null || road.getCars().isEmpty()) return false;
        double entryPointModel, exitPointModel;
        double buffer = 5.0;
        if (direction == 0) {
            entryPointModel = road.getLength() * 0.25 - buffer;
            exitPointModel = road.getLength() * 0.75 + buffer;
            for (Car car : road.getCars()) {
                if (car.getDirection() == direction && car.getPosition() > entryPointModel && car.getPosition() < exitPointModel) {
                    return true;
                }
            }
        } else {
            entryPointModel = road.getLength() * 0.75 + buffer;
            exitPointModel = road.getLength() * 0.25 - buffer;
            for (Car car : road.getCars()) {
                if (car.getDirection() == direction && car.getPosition() < entryPointModel && car.getPosition() > exitPointModel) {
                    return true;
                }
            }
        }
        return false;
    }

    public void startSimulation() { /* ... */ if (running) return; running = true; paused = false; new Thread(this, "SimulationThread").start(); if(ENGINE_DEBUG_LOGGING) System.out.println("Поток симуляции запущен."); }
    public void stopSimulation() { /* ... */ running = false; synchronized (pauseLock) { paused = false; pauseLock.notifyAll(); } if(ENGINE_DEBUG_LOGGING) System.out.println("Симуляция остановлена."); }
    public void pauseSimulation() { /* ... */ paused = true; if(ENGINE_DEBUG_LOGGING) System.out.println("Симуляция на паузе."); }
    public void resumeSimulation() { /* ... */ synchronized (pauseLock) { paused = false; pauseLock.notifyAll(); } if(ENGINE_DEBUG_LOGGING) System.out.println("Симуляция снята с паузы."); }

    @Override
    public void run() {
        // ... (код без изменений) ...
        long lastUpdateTime = System.nanoTime();
        final double TARGET_FPS = 30.0;
        final double OPTIMAL_TIME_PER_FRAME_NANO = 1_000_000_000.0 / TARGET_FPS;
        while (running) {
            synchronized (pauseLock) {
                if (paused) {
                    try { pauseLock.wait(); if (!running) break; lastUpdateTime = System.nanoTime(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); running = false; break; }
                }
            }
            long loopStartTime = System.nanoTime();
            double deltaTimeFromLastFrame = (loopStartTime - lastUpdateTime) / 1_000_000_000.0;
            lastUpdateTime = loopStartTime;
            deltaTimeFromLastFrame = Math.min(deltaTimeFromLastFrame, 0.1);
            double simulationDeltaTime = deltaTimeFromLastFrame * parameters.getSimulationSpeedFactor();
            if (road != null) {
                step(simulationDeltaTime);
            }
            if (simulationPanel != null) {
                Road currentRoadForPanel = road;
                double currentSimTimeForPanel = simulationTime;
                SwingUtilities.invokeLater(() -> simulationPanel.updateSimulationState(currentRoadForPanel, currentSimTimeForPanel));
            }
            long loopEndTime = System.nanoTime();
            long timeTakenNano = loopEndTime - loopStartTime;
            long sleepTimeNano = (long) (OPTIMAL_TIME_PER_FRAME_NANO - timeTakenNano);
            if (sleepTimeNano > 0) {
                try {
                    Thread.sleep(sleepTimeNano / 1_000_000, (int) (sleepTimeNano % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); running = false;
                }
            } else {
                Thread.yield();
            }
        }
        if(ENGINE_DEBUG_LOGGING) System.out.println("Поток симуляции завершил работу.");
    }

    private void step(double deltaTime) {
        if (deltaTime <= 0 || road == null) return;
        simulationTime += deltaTime;
        timeSinceLastLaneChangeCheck += deltaTime;

        if (isTunnelActive) {
            updateTunnelLogic(deltaTime);
        } else {
            List<TrafficLight> lights = road.getTrafficLights();
            if (lights != null) {
                for (TrafficLight light : lights) {
                    if (!light.isExternallyControlled()) light.update(deltaTime);
                }
            }
        }

        if (flowGenerator != null) {
            TunnelControlState currentTunnelStateForGenerator = isTunnelActive ? this.tunnelControlState : null;
            Car[] newCars = flowGenerator.generateCars(deltaTime, currentTunnelStateForGenerator);
            if (newCars != null) {
                for (Car newCar : newCars) {
                    if (newCar != null) road.addCar(newCar);
                }
            }
        }

        List<Car> currentCars = new ArrayList<>(road.getCars());
        // Сортировка важна для корректного поиска лидеров/последователей, особенно при смене полос
        currentCars.sort(Comparator.comparingInt(Car::getDirection).thenComparingDouble(Car::getPosition));


        for (Car car : currentCars) {
            Car leadCar = findLeadCarOnLane(car, car.getLaneIndex(), currentCars);
            double distanceToLead = (leadCar != null) ?
                    Math.max(0.01, Math.abs(leadCar.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH) : Double.POSITIVE_INFINITY;
            double effectiveSpeedLimit = findEffectiveSpeedLimit(car);
            TrafficLight nextLight = findNextTrafficLight(car);
            double distanceToLight = Double.POSITIVE_INFINITY;
            TrafficLightState nextLightState = null;
            if (nextLight != null) {
                distanceToLight = Math.abs(nextLight.getPosition() - car.getPosition());
                boolean carPassedLight = (car.getDirection() == 0 && car.getPosition() > nextLight.getPosition() + Car.APPROX_CAR_LENGTH * 0.3) ||
                        (car.getDirection() == 1 && car.getPosition() < nextLight.getPosition() - Car.APPROX_CAR_LENGTH * 0.3);
                if (!carPassedLight) nextLightState = nextLight.getCurrentState();
                else distanceToLight = Double.POSITIVE_INFINITY;
            }
            car.update(deltaTime, leadCar, distanceToLead, effectiveSpeedLimit, nextLightState, distanceToLight);
        }

        if (timeSinceLastLaneChangeCheck >= LANE_CHANGE_CHECK_INTERVAL) {
            if (road.getType() != RoadType.TUNNEL && road.getNumberOfLanes() > 1 && road.getLanesPerDirection() > 0 ) {
                processLaneChanges(currentCars);
            }
            timeSinceLastLaneChangeCheck = 0.0;
        }

        if (road.getCars() != null) {
            double removalBuffer = Car.APPROX_CAR_LENGTH * 3.0;
            road.getCars().removeIf(car ->
                    (car.getDirection() == 0 && car.getPosition() > road.getLength() + removalBuffer) ||
                            (car.getDirection() == 1 && car.getPosition() < -removalBuffer)
            );
        }
    }

    private void processLaneChanges(List<Car> cars) {
        if (road.getLanesPerDirection() <= 1 && road.getNumberOfDirections() == 1) return; // Для однополосной односторонней нечего делать

        for (Car car : cars) {
            if (road.getType() == RoadType.TUNNEL) continue; // В тоннеле нет смены полос
            // Если это двухсторонняя дорога, но в каждом направлении только по одной полосе, тоже нет смены
            if (road.getNumberOfDirections() == 2 && road.getLanesPerDirection() == 1) continue;


            double currentAcceleration = calculatePotentialAcceleration(car, car.getLaneIndex(), cars);

            // Определяем, является ли текущая полоса самой правой для данного направления
            // (для правостороннего движения, это полоса с наибольшим локальным индексом)
            int localLaneIndex = car.getLaneIndex() - (car.getDirection() * road.getLanesPerDirection());
            boolean isRightmostLane = (localLaneIndex == road.getLanesPerDirection() - 1);


            Double leftBenefit = null;
            int leftLane = getTargetLane(car, -1); // Левая полоса (относительно текущей)
            if (leftLane != -1) {
                leftBenefit = calculatePotentialAcceleration(car, leftLane, cars);
            }

            Double rightBenefit = null;
            int rightLane = getTargetLane(car, 1); // Правая полоса (относительно текущей)
            if (rightLane != -1) {
                rightBenefit = calculatePotentialAcceleration(car, rightLane, cars);
            }

            // В СНГ и Европе обычно правостороннее движение, стремятся держаться правее, обгоняют слева
            // isLeftLaneGenerallyPreferred: true, если не на крайней левой, false если на крайней левой (для обгона)
            // Для возврата направо: если не на крайней правой.
            boolean canMoveLeft = (leftLane != -1);
            // boolean canMoveRight = (rightLane != -1); // Уже проверяется в getTargetLane

            // Логика принятия решения:
            // 1. Если можно и выгоднее направо (с учетом bias) - хотим направо.
            // 2. Если можно и выгоднее налево И (направо нельзя ИЛИ налево значительно лучше чем направо) - хотим налево.
            car.evaluateLaneChange(currentAcceleration, leftBenefit, rightBenefit, !isRightmostLane);


            int targetLane = -1;
            String changeLogDirection = "";
            if (car.wantsToChangeLaneLeft()) {
                targetLane = leftLane; // leftLane уже содержит результат getTargetLane(car, -1)
                changeLogDirection = " налево";
            } else if (car.wantsToChangeLaneRight()) {
                targetLane = rightLane; // rightLane уже содержит результат getTargetLane(car, 1)
                changeLogDirection = " направо";
            }

            if (targetLane != -1) {
                if (isLaneChangeSafe(car, targetLane, cars)) {
                    if(ENGINE_DEBUG_LOGGING) System.out.printf("Car %d (lane %d, spd %.1f km/h, dir %d) CHANGING LANE%s to %d. MyAccel: %.2f, LeftBenefit: %.2f, RightBenefit: %.2f%n",
                            car.getId(), car.getLaneIndex(), car.getCurrentSpeed()*3.6, car.getDirection(),
                            changeLogDirection, targetLane, currentAcceleration,
                            leftBenefit != null ? leftBenefit : -99.0,
                            rightBenefit != null ? rightBenefit : -99.0);
                    car.setLaneIndex(targetLane);
                } else {
                    if(ENGINE_DEBUG_LOGGING && (car.wantsToChangeLaneLeft() || car.wantsToChangeLaneRight()))
                        System.out.printf("Car %d (lane %d) wanted to change to %d%s BUT NOT SAFE.%n", car.getId(), car.getLaneIndex(), targetLane, changeLogDirection);
                }
            }
        }
    }

    private double calculatePotentialAcceleration(Car car, int targetLaneIndex, List<Car> allCars) {
        // ... (код без изменений, как в предыдущем полном ответе) ...
        Car leadCarOnTargetLane = findLeadCarOnLane(car, targetLaneIndex, allCars);
        double distanceToLead = (leadCarOnTargetLane != null) ?
                Math.max(0.01, Math.abs(leadCarOnTargetLane.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH) : Double.POSITIVE_INFINITY;
        double s_star = Car.MIN_GAP + Math.max(0, car.getCurrentSpeed() * Car.SAFE_TIME_HEADWAY +
                (car.getCurrentSpeed() * (car.getCurrentSpeed() - (leadCarOnTargetLane != null ? leadCarOnTargetLane.getCurrentSpeed() : 0))) /
                        (2 * Math.sqrt(car.getAccelerationParam() * car.getBaseDecelerationParam())));
        double freeRoadTerm = car.getAccelerationParam() * (1 - Math.pow(car.getCurrentSpeed() / Math.max(0.1, car.getDesiredSpeed()), Car.ACCELERATION_EXPONENT));
        double interactionTerm = 0.0;
        if (leadCarOnTargetLane != null && distanceToLead < 200) {
            if (distanceToLead > 0.1) {
                interactionTerm = -car.getAccelerationParam() * Math.pow(s_star / Math.max(0.1, distanceToLead), 2);
            } else {
                interactionTerm = -car.getAccelerationParam() * Math.pow(s_star / 0.1, 2);
            }
        }
        return freeRoadTerm + interactionTerm;
    }

    private boolean isLaneChangeSafe(Car car, int targetLaneIndex, List<Car> allCars) {
        // ... (код без изменений, как в предыдущем полном ответе) ...
        Car newFollower = findFollowerOnLane(car, targetLaneIndex, allCars);
        if (newFollower != null) {
            double distanceCarToNewFollower = Math.abs(car.getPosition() - newFollower.getPosition()) - Car.APPROX_CAR_LENGTH;
            if (distanceCarToNewFollower < Car.MIN_GAP * 0.8) {
                if(ENGINE_DEBUG_LOGGING) System.out.println("LC unsafe: too close to new follower " + newFollower.getId());
                return false;
            }
            double followerSpeed = newFollower.getCurrentSpeed();
            double s_star_follower = Car.MIN_GAP + Math.max(0, followerSpeed * Car.SAFE_TIME_HEADWAY +
                    (followerSpeed * (followerSpeed - car.getCurrentSpeed())) /
                            (2 * Math.sqrt(newFollower.getAccelerationParam() * newFollower.getBaseDecelerationParam())));
            double interactionFollower = -newFollower.getAccelerationParam() * Math.pow(s_star_follower / Math.max(0.1,distanceCarToNewFollower), 2);
            double freeRoadFollower = newFollower.getAccelerationParam() * (1 - Math.pow(followerSpeed / Math.max(0.1, newFollower.getDesiredSpeed()), Car.ACCELERATION_EXPONENT));
            double potentialAccelerationFollower = freeRoadFollower + interactionFollower;
            if (potentialAccelerationFollower < -Car.SAFE_DECELERATION_FOR_OTHERS) {
                if(ENGINE_DEBUG_LOGGING) System.out.println("LC unsafe for new follower " + newFollower.getId() + ". Accel: " + String.format("%.2f", potentialAccelerationFollower) + " (limit: " + -Car.SAFE_DECELERATION_FOR_OTHERS + ")");
                return false;
            }
        }
        Car newLeader = findLeadCarOnLane(car, targetLaneIndex, allCars);
        if (newLeader != null) {
            double distanceToNewLeader = Math.abs(newLeader.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH;
            if (distanceToNewLeader < Car.MIN_GAP * 1.2) { // Немного больше зазор для безопасности "подрезания"
                if(ENGINE_DEBUG_LOGGING) System.out.println("LC unsafe: too close to new leader " + newLeader.getId());
                return false;
            }
        }
        return true;
    }

    private int getTargetLane(Car car, int directionOfChange) { // directionOfChange: -1 for left, +1 for right
        // ... (код без изменений, как в предыдущем полном ответе) ...
        int currentGlobalLane = car.getLaneIndex();
        int currentCarDirection = car.getDirection();
        int lanesPerActualDirection = road.getLanesPerDirection();
        if (lanesPerActualDirection == 0) return -1;

        int localLaneIndex = currentGlobalLane - (currentCarDirection * lanesPerActualDirection);
        int targetLocalLane = localLaneIndex + directionOfChange;

        if (targetLocalLane >= 0 && targetLocalLane < lanesPerActualDirection) {
            return (currentCarDirection * lanesPerActualDirection) + targetLocalLane;
        }
        return -1;
    }

    private Car findLeadCarOnLane(Car currentCar, int targetGlobalLaneIndex, List<Car> allCars) {
        // ... (код без изменений, как в предыдущем полном ответе) ...
        Car leader = null;
        double minPositiveDistance = Double.POSITIVE_INFINITY;
        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() ||
                    otherCar.getLaneIndex() != targetGlobalLaneIndex ||
                    otherCar.getDirection() != currentCar.getDirection()) {
                continue;
            }
            double distance = (currentCar.getDirection() == 0) ?
                    (otherCar.getPosition() - currentCar.getPosition()) :
                    (currentCar.getPosition() - otherCar.getPosition());
            if (distance > 0.01 && distance < minPositiveDistance) {
                minPositiveDistance = distance;
                leader = otherCar;
            }
        }
        return leader;
    }

    private Car findFollowerOnLane(Car currentCar, int targetGlobalLaneIndex, List<Car> allCars) {
        // ... (код без изменений, как в предыдущем полном ответе) ...
        Car follower = null;
        double minPositiveDistanceBehind = Double.POSITIVE_INFINITY;
        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() ||
                    otherCar.getLaneIndex() != targetGlobalLaneIndex ||
                    otherCar.getDirection() != currentCar.getDirection()) {
                continue;
            }
            double distance = (currentCar.getDirection() == 0) ?
                    (currentCar.getPosition() - otherCar.getPosition()) :
                    (otherCar.getPosition() - currentCar.getPosition());
            if (distance > 0.01 && distance < minPositiveDistanceBehind) {
                minPositiveDistanceBehind = distance;
                follower = otherCar;
            }
        }
        return follower;
    }

    private double findEffectiveSpeedLimit(Car car) {
        // ... (код без изменений, как в предыдущем полном ответе) ...
        RoadType currentRoadType = road.getType();
        double roadTypeMaxSpeedMs = currentRoadType.getMaxSpeedLimitMs();
        double roadTypeMinSpeedMs = currentRoadType.getMinSpeedLimitMs();
        double carPersonalMaxSpeedMs = car.getMaxSpeed();
        double effectiveLimitMs = Math.min(roadTypeMaxSpeedMs, carPersonalMaxSpeedMs);
        List<RoadSign> signs = road.getRoadSigns();

        if (signs != null && !signs.isEmpty()) {
            double activeSignLimitMs = -1.0;
            if (car.getDirection() == 0) {
                for (RoadSign sign : signs) {
                    if (sign.getTargetDirection() == 1) continue;
                    if (sign.getPosition() <= car.getPosition() + 1.0) {
                        double limitFromSign = sign.getSpeedLimitValue();
                        if (limitFromSign >= 0) {
                            activeSignLimitMs = limitFromSign;
                        }
                    } else { break; }
                }
            } else {
                for (int i = signs.size() - 1; i >= 0; i--) {
                    RoadSign sign = signs.get(i);
                    if (sign.getTargetDirection() == 0) continue;
                    if (sign.getPosition() >= car.getPosition() - 1.0) {
                        double limitFromSign = sign.getSpeedLimitValue();
                        if (limitFromSign >= 0) {
                            activeSignLimitMs = limitFromSign;
                        }
                    } else { break; }
                }
            }
            if (activeSignLimitMs >= 0) {
                effectiveLimitMs = Math.min(effectiveLimitMs, activeSignLimitMs);
            }
        }
        return Math.max(roadTypeMinSpeedMs, Math.min(effectiveLimitMs, carPersonalMaxSpeedMs));
    }

    private TrafficLight findNextTrafficLight(Car car) {
        // ... (код без изменений, как в предыдущем полном ответе) ...
        List<TrafficLight> lights = road.getTrafficLights();
        if (lights == null || lights.isEmpty()) return null;
        TrafficLight nextFoundLight = null;
        double minPositiveDistance = Double.POSITIVE_INFINITY;
        for (TrafficLight light : lights) {
            if (light.getTargetDirection() != car.getDirection() && light.getTargetDirection() != -1) {
                continue;
            }
            double distanceToLight;
            if (car.getDirection() == 0) {
                distanceToLight = light.getPosition() - car.getPosition();
            } else {
                distanceToLight = car.getPosition() - light.getPosition();
            }
            if (distanceToLight > -0.5 && distanceToLight < minPositiveDistance) {
                minPositiveDistance = distanceToLight;
                nextFoundLight = light;
            }
        }
        return nextFoundLight;
    }

    public void updateParameters(SimulationParameters newParams) {
        // ... (код без изменений, как в предыдущем полном ответе) ...
        this.parameters = newParams;
        this.initializeSimulation();
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Параметры обновлены и симуляция переинициализирована.");
    }

    public Road getRoad() { return road; }
    public double getSimulationTime() { return simulationTime; }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    private int getGuiPanelWidth() {
        // ... (код без изменений, как в предыдущем полном ответе) ...
        if (simulationPanel != null) {
            return simulationPanel.getWidth();
        }
        return 800;
    }
}
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
import java.util.stream.Collectors;


public class SimulationEngine implements Runnable {

    private SimulationParameters parameters;
    private Road road;
    private TrafficFlowGenerator flowGenerator;
    private SimulationPanel simulationPanel;

    private volatile boolean running = false;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();
    private Thread simulationThread;

    private double simulationTime = 0.0;

    private TunnelControlState tunnelControlState;
    private double tunnelPhaseTimer;
    private TrafficLight tunnelLightDir0;
    private TrafficLight tunnelLightDir1;

    private static final boolean ENGINE_DEBUG_LOGGING = true; // Включим для отладки расчета
    private static final double TUNNEL_CLEARANCE_TIME_SAFETY_FACTOR = 1.15; // 15% запас времени


    public SimulationEngine(SimulationParameters params, SimulationPanel panel) {
        this.parameters = params;
        this.simulationPanel = panel;
        performFullInitialization();
    }

    public void performFullInitialization() {
        // ... (без изменений, как в предыдущей полной версии) ...
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Performing Full Initialization.");
        boolean isTunnelActive = (parameters.getRoadType() == RoadType.TUNNEL);

        if (isTunnelActive) {
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

        this.tunnelLightDir0 = null;
        this.tunnelLightDir1 = null;

        if (this.road.getTrafficLights() != null) this.road.clearTrafficLights();
        if (this.road.getRoadSigns() != null) this.road.clearRoadSigns();
        if (this.road.getCars() != null) this.road.getCars().clear();


        if (isTunnelActive) {
            double roadModelLength = this.road.getLength();
            // Используем новые позиции 0.1 и 0.9
            double leftLightPosition = roadModelLength * 0.1;
            double rightLightPosition = roadModelLength * 0.9;

            tunnelLightDir0 = new TrafficLight(leftLightPosition, parameters.getTunnelDefaultRedDuration(), parameters.getTunnelDefaultGreenDuration(), TrafficLightState.GREEN, 0);
            tunnelLightDir0.setExternallyControlled(true);
            this.road.addTrafficLight(tunnelLightDir0);

            tunnelLightDir1 = new TrafficLight(rightLightPosition, parameters.getTunnelDefaultRedDuration(), parameters.getTunnelDefaultGreenDuration(), TrafficLightState.RED, 1);
            tunnelLightDir1.setExternallyControlled(true);
            this.road.addTrafficLight(tunnelLightDir1);

            tunnelControlState = TunnelControlState.DIR0_GREEN;
            tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
            if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel activated. Initial state: DIR0_GREEN (model_dir 0 L->R). Timer: " + tunnelPhaseTimer);
        }
        if (simulationPanel != null) {
            simulationPanel.updateSimulationState(this.road, this.simulationTime);
        }
    }

    private double calculateDynamicTunnelClearanceTime() {
        if (road == null || road.getType() != RoadType.TUNNEL) {
            return 15.0; // Значение по умолчанию, если что-то не так
        }
        // Длина активной зоны тоннеля (между 0.1L и 0.9L) = 0.8 * L
        double activeZoneLength = road.getLength() * 0.8;
        double minSpeedInTunnelMs = RoadType.TUNNEL.getMinSpeedLimitMs();

        if (minSpeedInTunnelMs <= 0.1) { // Защита от деления на ноль или очень малую скорость
            minSpeedInTunnelMs = 5.0 / 3.6; // Примерно 5 км/ч, если не задано
            if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel Clearance: Min speed in tunnel was too low, using default 5km/h.");
        }

        double calculatedTime = activeZoneLength / minSpeedInTunnelMs;
        calculatedTime *= TUNNEL_CLEARANCE_TIME_SAFETY_FACTOR; // Добавляем запас

        if (ENGINE_DEBUG_LOGGING) {
            System.out.printf("Tunnel Clearance Time Calculated: ActiveZone=%.1fm, MinSpeed=%.1fm/s (%.1fkm/h), RawTime=%.1fs, WithFactor=%.1fs%n",
                    activeZoneLength, minSpeedInTunnelMs, minSpeedInTunnelMs * 3.6, activeZoneLength / minSpeedInTunnelMs, calculatedTime);
        }
        return Math.max(5.0, calculatedTime); // Минимальное время очистки - 5 секунд
    }

    private void resetSimulationStateOnly() {
        // ... (сброс машин, времени, генератора - без изменений) ...
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Resetting simulation state (cars, time).");
        this.simulationTime = 0.0;
        if (this.road != null && this.road.getCars() != null) {
            this.road.getCars().clear();
        }
        if (this.flowGenerator != null) {
            this.flowGenerator.resetGenerationTimers();
        }
        if (road != null && road.getType() == RoadType.TUNNEL && tunnelLightDir0 !=null && tunnelLightDir1 != null) {
            tunnelLightDir0.setCurrentState(TrafficLightState.GREEN, true);
            tunnelLightDir1.setCurrentState(TrafficLightState.RED, true);
            tunnelControlState = TunnelControlState.DIR0_GREEN;
            tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
        } else if (road != null && road.getTrafficLights() != null) {
            for(TrafficLight tl : road.getTrafficLights()){
                if(!tl.isExternallyControlled()){
                    tl.resetToInitialState();
                }
            }
        }
        if (simulationPanel != null) {
            simulationPanel.updateSimulationState(this.road, this.simulationTime);
        }
    }

    private void updateTunnelLogic(double deltaTime) {
        if (road.getType() != RoadType.TUNNEL || tunnelLightDir0 == null || tunnelLightDir1 == null) return;
        tunnelPhaseTimer -= deltaTime;

        tunnelLightDir0.update(deltaTime);
        tunnelLightDir1.update(deltaTime);

        if (tunnelPhaseTimer <= 0) {
            double dynamicClearanceTime = calculateDynamicTunnelClearanceTime(); // Рассчитываем здесь

            switch (tunnelControlState) {
                case DIR0_GREEN:
                    tunnelControlState = TunnelControlState.DIR0_CLEARING;
                    tunnelPhaseTimer = dynamicClearanceTime; // Используем рассчитанное время
                    tunnelLightDir0.setCurrentState(TrafficLightState.RED, true);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel: DIR0_GREEN -> DIR0_CLEARING. Clearance Timer: " + String.format("%.1f", tunnelPhaseTimer) + "s");
                    break;
                case DIR0_CLEARING:
                    // Даем чуть больше времени, если машины еще есть, но не бесконечно
                    if (!areCarsInTunnelActiveZone(0) || tunnelPhaseTimer <= -dynamicClearanceTime * 0.1) { // небольшой отрицательный буфер
                        tunnelControlState = TunnelControlState.DIR1_GREEN;
                        tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
                        tunnelLightDir1.setCurrentState(TrafficLightState.GREEN, true);
                        if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel: DIR0_CLEARING -> DIR1_GREEN. Green Timer: " + String.format("%.1f", tunnelPhaseTimer) + "s");
                    } else {
                        // Не даем таймеру уйти слишком сильно в минус, если машины долго выезжают
                        tunnelPhaseTimer = Math.max(tunnelPhaseTimer, 0.1);
                        if (ENGINE_DEBUG_LOGGING && areCarsInTunnelActiveZone(0)) System.out.println("Tunnel: DIR0_CLEARING - cars still in zone. Timer extended to: " + String.format("%.1f", tunnelPhaseTimer) + "s");
                    }
                    break;
                case DIR1_GREEN:
                    tunnelControlState = TunnelControlState.DIR1_CLEARING;
                    tunnelPhaseTimer = dynamicClearanceTime; // Используем рассчитанное время
                    tunnelLightDir1.setCurrentState(TrafficLightState.RED, true);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel: DIR1_GREEN -> DIR1_CLEARING. Clearance Timer: " + String.format("%.1f", tunnelPhaseTimer) + "s");
                    break;
                case DIR1_CLEARING:
                    if (!areCarsInTunnelActiveZone(1) || tunnelPhaseTimer <= -dynamicClearanceTime * 0.1) {
                        tunnelControlState = TunnelControlState.DIR0_GREEN;
                        tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
                        tunnelLightDir0.setCurrentState(TrafficLightState.GREEN, true);
                        if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel: DIR1_CLEARING -> DIR0_GREEN. Green Timer: " + String.format("%.1f", tunnelPhaseTimer) + "s");
                    } else {
                        tunnelPhaseTimer = Math.max(tunnelPhaseTimer, 0.1);
                        if (ENGINE_DEBUG_LOGGING && areCarsInTunnelActiveZone(1)) System.out.println("Tunnel: DIR1_CLEARING - cars still in zone. Timer extended to: " + String.format("%.1f", tunnelPhaseTimer) + "s");
                    }
                    break;
            }
        }
    }

    private boolean areCarsInTunnelActiveZone(int modelDirection) {
        if (road == null || road.getCars() == null) return false;
        // Активная зона между 0.1L и 0.9L
        double entryZoneEdge = (modelDirection == 0) ? road.getLength() * 0.1 : road.getLength() * 0.9;
        double exitZoneEdge  = (modelDirection == 0) ? road.getLength() * 0.9 : road.getLength() * 0.1;
        // Машина считается в активной зоне, если она строго между этими краями
        // (не включая машины, стоящие точно на линии светофора или только что въехавшие на 1 пиксель)

        for (Car car : road.getCars()) {
            if (car.getDirection() == modelDirection) {
                if (modelDirection == 0) { // L->R
                    // Машина должна быть правее левого светофора и левее правого светофора
                    if (car.getPosition() > entryZoneEdge + Car.APPROX_CAR_LENGTH * 0.1 &&
                            car.getPosition() < exitZoneEdge - Car.APPROX_CAR_LENGTH * 0.1) {
                        return true;
                    }
                } else { // R->L
                    // Машина должна быть левее правого светофора (большая позиция) и правее левого светофора (меньшая позиция)
                    if (car.getPosition() < entryZoneEdge - Car.APPROX_CAR_LENGTH * 0.1 &&
                            car.getPosition() > exitZoneEdge + Car.APPROX_CAR_LENGTH * 0.1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ... (startSimulation, stopSimulation, pauseSimulation, resumeSimulation, run, step и другие - БЕЗ ИЗМЕНЕНИЙ)
    // Я привожу их для полноты, они уже должны быть исправлены.
    public void startSimulation() {
        if (running && !paused) return;

        if (simulationThread != null && simulationThread.isAlive()) {
            running = false;
            try { simulationThread.join(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        if (!paused) {
            resetSimulationStateOnly();
        }

        running = true;
        paused = false;
        simulationThread = new Thread(this, "SimulationThread");
        simulationThread.start();
        if(ENGINE_DEBUG_LOGGING) System.out.println("Поток симуляции запущен/возобновлен.");
    }

    public void stopSimulation() {
        running = false;
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        resetSimulationStateOnly();
        if(ENGINE_DEBUG_LOGGING) System.out.println("Симуляция остановлена. Машины очищены, время сброшено.");
    }

    public void pauseSimulation() { if(running) paused = true; if(ENGINE_DEBUG_LOGGING) System.out.println("Симуляция на паузе."); }
    public void resumeSimulation() { if(running && paused) { synchronized (pauseLock) { paused = false; pauseLock.notifyAll(); } if(ENGINE_DEBUG_LOGGING) System.out.println("Симуляция снята с паузы.");} }

    @Override
    public void run() {
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
            if (!running) break;

            long loopStartTime = System.nanoTime();
            double deltaTimeFromLastFrame = (loopStartTime - lastUpdateTime) / 1_000_000_000.0;
            lastUpdateTime = loopStartTime;
            deltaTimeFromLastFrame = Math.min(deltaTimeFromLastFrame, 0.1);
            double simulationDeltaTime = deltaTimeFromLastFrame * parameters.getSimulationSpeedFactor();

            if (road != null) step(simulationDeltaTime);

            if (simulationPanel != null) {
                Road currentRoadForPanel = road;
                double currentSimTimeForPanel = simulationTime;
                SwingUtilities.invokeLater(() -> simulationPanel.updateSimulationState(currentRoadForPanel, currentSimTimeForPanel));
            }
            long loopEndTime = System.nanoTime();
            long timeTakenNano = loopEndTime - loopStartTime;
            long sleepTimeNano = (long) (OPTIMAL_TIME_PER_FRAME_NANO - timeTakenNano);
            if (sleepTimeNano > 0) {
                try { Thread.sleep(sleepTimeNano / 1_000_000, (int) (sleepTimeNano % 1_000_000)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); running = false; }
            } else { Thread.yield(); }
        }
        if(ENGINE_DEBUG_LOGGING) System.out.println("Поток симуляции штатно завершил работу.");
    }
    private void step(double deltaTime) {
        if (deltaTime <= 0 || road == null) return;
        simulationTime += deltaTime;

        if (road.getType() == RoadType.TUNNEL) {
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
            TunnelControlState currentTunnelState = (road.getType() == RoadType.TUNNEL) ? this.tunnelControlState : null;
            Car[] newCars = flowGenerator.generateCars(deltaTime, currentTunnelState);
            if (newCars != null) {
                for (Car newCar : newCars) {
                    if (newCar != null) road.addCar(newCar);
                }
            }
        }

        List<Car> currentCars = new ArrayList<>(road.getCars());
        currentCars.sort((car1, car2) -> {
            if (car1.getDirection() != car2.getDirection()) return Integer.compare(car1.getDirection(), car2.getDirection());
            return (car1.getDirection() == 0) ? Double.compare(car1.getPosition(), car2.getPosition()) : Double.compare(car2.getPosition(), car1.getPosition());
        });

        for (Car car : currentCars) {
            Car leadCar = findLeadCarOnLocalLane(car, car.getCurrentLaneIndex(), currentCars);
            double distanceToLead = (leadCar != null) ? Math.max(0.01, Math.abs(leadCar.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH) : Double.POSITIVE_INFINITY;
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

        if (road.getType() != RoadType.TUNNEL && road.getLanesPerDirection() > 1) {
            for (Car car : currentCars) {
                if (car.canConsiderLaneChange()) {
                    evaluateAndCommitLaneChangeForCar(car, currentCars);
                }
            }
        }

        if (road.getType() != RoadType.TUNNEL && road.getLanesPerDirection() > 1) {
            for (Car car : currentCars) {
                if (!car.isChangingLane() && (car.isCommittedToChangeLeft() || car.isCommittedToChangeRight())) {
                    int committedTargetLane = car.getCommittedTargetLane();
                    if (committedTargetLane != -1) {
                        if (isLaneChangeSafeAndNotConflicting(car, committedTargetLane, currentCars)) {
                            car.startLaneChangeIfCommitted(committedTargetLane);
                        } else {
                            if (ENGINE_DEBUG_LOGGING) System.out.printf("Car %d: Aborting committed change to %d due to safety/conflict.%n", car.getId(), committedTargetLane);
                            car.resetCommitments();
                        }
                    }
                }
            }
        }

        if (road.getCars() != null) {
            double removalBuffer = Car.APPROX_CAR_LENGTH * 3.0;
            road.getCars().removeIf(car ->
                    (car.getDirection() == 0 && car.getPosition() > road.getLength() + removalBuffer) ||
                            (car.getDirection() == 1 && car.getPosition() < -removalBuffer)
            );
        }
    }

    private void evaluateAndCommitLaneChangeForCar(Car car, List<Car> allCars) {
        int currentLocalLane = car.getCurrentLaneIndex();
        double currentObservedAcceleration = calculatePotentialAccelerationOnLocalLane(car, currentLocalLane, allCars);
        boolean isRightmost = (currentLocalLane == 0);
        boolean isLeftmost = (currentLocalLane == road.getLanesPerDirection() - 1);
        Double leftBenefit = null;
        int targetLocalLeftLane = -1;
        boolean overallSafetyLeft = false;
        boolean leftChangeHurtsFollower = true;
        if (!isLeftmost) {
            targetLocalLeftLane = currentLocalLane + 1;
            leftBenefit = calculatePotentialAccelerationOnLocalLane(car, targetLocalLeftLane, allCars);
            overallSafetyLeft = isSafeGapForLeader(car, targetLocalLeftLane, allCars);
            if (overallSafetyLeft) {
                leftChangeHurtsFollower = checkFollowerDecelerationTooHigh(car, targetLocalLeftLane, allCars);
            }
        }
        Double rightBenefit = null;
        int targetLocalRightLane = -1;
        boolean overallSafetyRight = false;
        boolean rightChangeHurtsFollower = true;
        if (!isRightmost) {
            targetLocalRightLane = currentLocalLane - 1;
            rightBenefit = calculatePotentialAccelerationOnLocalLane(car, targetLocalRightLane, allCars);
            overallSafetyRight = isSafeGapForLeader(car, targetLocalRightLane, allCars);
            if (overallSafetyRight) {
                rightChangeHurtsFollower = checkFollowerDecelerationTooHigh(car, targetLocalRightLane, allCars);
            }
        }
        car.decideLaneChange(currentObservedAcceleration, leftBenefit, rightBenefit,
                isRightmost, isLeftmost,
                overallSafetyLeft, overallSafetyRight,
                leftChangeHurtsFollower, rightChangeHurtsFollower,
                road.getLanesPerDirection(), road.getType().getDefaultSpeedLimitMs(), road.getType().getMaxSpeedLimitMs());
    }

    private boolean isSafeGapForLeader(Car car, int targetLocalLaneIndex, List<Car> allCars) {
        Car newLeader = findLeadCarOnLocalLane(car, targetLocalLaneIndex, allCars);
        if (newLeader != null) {
            double distanceToNewLeader = Math.abs(newLeader.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH;
            if (distanceToNewLeader < Car.MIN_GAP * 1.0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkFollowerDecelerationTooHigh(Car carChanging, int targetLocalLaneIndex, List<Car> allCars) {
        Car newFollower = findFollowerOnLocalLane(carChanging, targetLocalLaneIndex, allCars);
        if (newFollower != null) {
            double distanceCarToNewFollower = Math.abs(carChanging.getPosition() - newFollower.getPosition()) - Car.APPROX_CAR_LENGTH;
            if (distanceCarToNewFollower < Car.MIN_GAP * 0.7) {
                if (ENGINE_DEBUG_LOGGING) System.out.printf("Car %d: Follower %d on lane %d is too close (%.1fm) for safety check.%n", carChanging.getId(), newFollower.getId(), targetLocalLaneIndex, distanceCarToNewFollower);
                return true;
            }
            double followerSpeed = newFollower.getCurrentSpeed();
            double followerDesiredSpeed = newFollower.getDesiredSpeed();
            double followerAccel = newFollower.getActualAccelerationParam();
            double followerDecel = newFollower.getActualBaseDecelerationParam();
            double followerSafeTimeHeadway = newFollower.getActualSafeTimeHeadway();
            double deltaV_follower_hypothetical = followerSpeed - carChanging.getCurrentSpeed();
            double s_star_follower = Car.MIN_GAP + Math.max(0, followerSpeed * followerSafeTimeHeadway +
                    (followerSpeed * deltaV_follower_hypothetical) / (2 * Math.sqrt(followerAccel * followerDecel)));
            double freeRoadFollower = followerAccel * (1 - Math.pow(followerSpeed / Math.max(0.1, followerDesiredSpeed), Car.ACCELERATION_EXPONENT));
            double interactionFollower;
            if (distanceCarToNewFollower > 0.1) {
                interactionFollower = -followerAccel * Math.pow(s_star_follower / distanceCarToNewFollower, 2);
            } else {
                interactionFollower = -followerAccel * Math.pow(s_star_follower / 0.1, 2);
            }
            double potentialAccelerationFollower = freeRoadFollower + interactionFollower;
            if (potentialAccelerationFollower < (-Car.SAFE_DECELERATION_FOR_OTHERS + Car.POLITENESS_FACTOR * Car.SAFE_DECELERATION_FOR_OTHERS)) {
                if (ENGINE_DEBUG_LOGGING) System.out.printf("Car %d: Follower %d on lane %d would brake at %.2f (limit %.2f). Change hurts too much.%n", carChanging.getId(), newFollower.getId(), targetLocalLaneIndex, potentialAccelerationFollower, (-Car.SAFE_DECELERATION_FOR_OTHERS + Car.POLITENESS_FACTOR * Car.SAFE_DECELERATION_FOR_OTHERS));
                return true;
            }
        }
        return false;
    }

    private boolean isLaneChangeSafeAndNotConflicting(Car carMakingChange, int targetLocalLane, List<Car> allCars) {
        if (!isSafeGapForLeader(carMakingChange, targetLocalLane, allCars) ||
                checkFollowerDecelerationTooHigh(carMakingChange, targetLocalLane, allCars)) {
            return false;
        }
        for (Car otherCar : allCars) {
            if (otherCar.getId() == carMakingChange.getId() || otherCar.getDirection() != carMakingChange.getDirection()) continue;
            if (otherCar.isChangingLane() && otherCar.getTargetLaneForChange() == targetLocalLane) {
                if (Math.abs(otherCar.getPosition() - carMakingChange.getPosition()) < Car.APPROX_CAR_LENGTH * 5) return false;
            }
            if ((otherCar.isCommittedToChangeLeft() || otherCar.isCommittedToChangeRight()) && otherCar.getCommittedTargetLane() == targetLocalLane) {
                if (Math.abs(otherCar.getPosition() - carMakingChange.getPosition()) < Car.APPROX_CAR_LENGTH * 5) return false;
            }
            if (otherCar.isChangingLane() && otherCar.getCurrentLaneIndex() == targetLocalLane && otherCar.getTargetLaneForChange() == carMakingChange.getCurrentLaneIndex()){
                if (Math.abs(otherCar.getPosition() - carMakingChange.getPosition()) < Car.APPROX_CAR_LENGTH * 4) return false;
            }
        }
        return true;
    }

    private double calculatePotentialAccelerationOnLocalLane(Car car, int targetLocalLaneIndex, List<Car> allCars) {
        Car leadCarOnTargetLane = findLeadCarOnLocalLane(car, targetLocalLaneIndex, allCars);
        double distanceToLead = (leadCarOnTargetLane != null) ? Math.max(0.01, Math.abs(leadCarOnTargetLane.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH) : Double.POSITIVE_INFINITY;
        double desiredSpeed = car.getDesiredSpeed();
        double currentSpeed = car.getCurrentSpeed();
        double accelParam = car.getActualAccelerationParam();
        double decelParam = car.getActualBaseDecelerationParam();
        double safeTimeHeadway = car.getActualSafeTimeHeadway();
        double deltaV_hypothetical = (leadCarOnTargetLane != null) ? currentSpeed - leadCarOnTargetLane.getCurrentSpeed() : 0;
        double s_star = Car.MIN_GAP + Math.max(0, currentSpeed * safeTimeHeadway +
                (currentSpeed * deltaV_hypothetical) / (2 * Math.sqrt(accelParam * decelParam)));
        double freeRoadTerm = accelParam * (1 - Math.pow(currentSpeed / Math.max(0.1, desiredSpeed), Car.ACCELERATION_EXPONENT));
        double interactionTerm = 0.0;
        if (leadCarOnTargetLane != null && distanceToLead < 200) {
            if (distanceToLead > 0.1) {
                interactionTerm = -accelParam * Math.pow(s_star / distanceToLead, 2);
            } else {
                interactionTerm = -accelParam * Math.pow(s_star / 0.1, 2);
            }
        }
        return freeRoadTerm + interactionTerm;
    }

    private Car findLeadCarOnLocalLane(Car currentCar, int targetLocalLaneIndex, List<Car> allCars) {
        Car leader = null;
        double minPositiveDistance = Double.POSITIVE_INFINITY;
        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() || otherCar.getDirection() != currentCar.getDirection() || otherCar.getCurrentLaneIndex() != targetLocalLaneIndex) continue;
            double distance = (currentCar.getDirection() == 0) ? (otherCar.getPosition() - currentCar.getPosition()) : (currentCar.getPosition() - otherCar.getPosition());
            if (distance > 0.01 && distance < minPositiveDistance) { minPositiveDistance = distance; leader = otherCar; }
        }
        return leader;
    }

    private Car findFollowerOnLocalLane(Car currentCar, int targetLocalLaneIndex, List<Car> allCars) {
        Car follower = null;
        double minPositiveDistanceBehind = Double.POSITIVE_INFINITY;
        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() || otherCar.getDirection() != currentCar.getDirection() || otherCar.getCurrentLaneIndex() != targetLocalLaneIndex) continue;
            double distance = (currentCar.getDirection() == 0) ? (currentCar.getPosition() - otherCar.getPosition()) : (otherCar.getPosition() - currentCar.getPosition());
            if (distance > 0.01 && distance < minPositiveDistanceBehind) { minPositiveDistanceBehind = distance; follower = otherCar; }
        }
        return follower;
    }

    private double findEffectiveSpeedLimit(Car car) {
        RoadType currentRoadType = road.getType();
        double roadTypeMaxSpeedMs = currentRoadType.getMaxSpeedLimitMs();
        double roadTypeMinSpeedMs = currentRoadType.getMinSpeedLimitMs();
        double carPersonalMaxSpeedMs = car.getMaxSpeed();
        double effectiveLimitMs = Math.min(roadTypeMaxSpeedMs, carPersonalMaxSpeedMs);
        List<RoadSign> signs = road.getRoadSigns();
        RoadSign activeSign = null;
        if (signs != null && !signs.isEmpty()) {
            int carModelDir = car.getDirection();
            if (carModelDir == 0) {
                for (RoadSign sign : signs) {
                    if (sign.getTargetDirection() == carModelDir || sign.getTargetDirection() == -1) {
                        if (sign.getPosition() <= car.getPosition() + Car.APPROX_CAR_LENGTH * 0.5) activeSign = sign;
                        else break;
                    }
                }
            } else {
                for (int i = signs.size() - 1; i >= 0; i--) {
                    RoadSign sign = signs.get(i);
                    if (sign.getTargetDirection() == carModelDir || sign.getTargetDirection() == -1) {
                        if (sign.getPosition() >= car.getPosition() - Car.APPROX_CAR_LENGTH * 0.5) activeSign = sign;
                        else break;
                    }
                }
            }
        }
        if (activeSign != null) {
            double limitFromSignMs = activeSign.getSpeedLimitValue();
            if (limitFromSignMs >= 0) effectiveLimitMs = Math.min(effectiveLimitMs, limitFromSignMs);
        }
        return Math.max(roadTypeMinSpeedMs, Math.max(0, effectiveLimitMs));
    }

    private TrafficLight findNextTrafficLight(Car car) {
        List<TrafficLight> lights = road.getTrafficLights();
        if (lights == null || lights.isEmpty()) return null;
        TrafficLight nextFoundLight = null;
        double minPositiveDistance = Double.POSITIVE_INFINITY;
        int carModelDir = car.getDirection();
        for (TrafficLight light : lights) {
            if (light.getTargetDirection() != carModelDir && light.getTargetDirection() != -1) continue;
            if (road.getType() == RoadType.TUNNEL && !light.isExternallyControlled()) continue;
            if (road.getType() != RoadType.TUNNEL && light.isExternallyControlled()) continue;
            double distanceToLight = (carModelDir == 0) ? (light.getPosition() - car.getPosition()) : (car.getPosition() - light.getPosition());
            if (distanceToLight > -Car.APPROX_CAR_LENGTH * 0.5 && distanceToLight < minPositiveDistance) {
                minPositiveDistance = distanceToLight;
                nextFoundLight = light;
            }
        }
        return nextFoundLight;
    }

    public void updateParameters(SimulationParameters newParams) {
        boolean wasRunningAndNotPaused = this.running && !this.paused;

        if (this.running) {
            running = false;
            synchronized(pauseLock){ pauseLock.notifyAll(); }
            if (simulationThread != null) {
                try {
                    simulationThread.join(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        this.parameters = newParams;
        performFullInitialization();

        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Параметры обновлены, симуляция полностью переинициализирована.");
    }

    public Road getRoad() { return road; }
    public double getSimulationTime() { return simulationTime; }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
}
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

    private double simulationTime = 0.0;

    private TunnelControlState tunnelControlState;
    private double tunnelPhaseTimer;
    private TrafficLight tunnelLightDir0;
    private TrafficLight tunnelLightDir1;

    private static final boolean ENGINE_DEBUG_LOGGING = false; // Выключим по умолчанию
    private static final double LANE_CHANGE_CHECK_INTERVAL = 0.5;
    private double timeSinceLastLaneChangeCheck = 0.0;


    public SimulationEngine(SimulationParameters params, SimulationPanel panel) {
        this.parameters = params;
        this.simulationPanel = panel;
        initializeSimulation();
    }

    public void initializeSimulation() {
        // ... (без изменений, как в предыдущей версии) ...
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Init. RoadType: " + parameters.getRoadType());
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
        this.timeSinceLastLaneChangeCheck = 0.0;
        this.tunnelLightDir0 = null;
        this.tunnelLightDir1 = null;

        if (this.road.getTrafficLights() != null) this.road.clearTrafficLights();
        if (isTunnelActive && this.road.getRoadSigns() != null) this.road.clearRoadSigns();

        if (isTunnelActive) {
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
            if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel activated. Initial state: DIR0_GREEN (model_dir 0 L->R). Timer: " + tunnelPhaseTimer);
        }
        if (simulationPanel != null) {
            simulationPanel.updateSimulationState(this.road, this.simulationTime);
        }
    }

    private void updateTunnelLogic(double deltaTime) {
        // ... (без изменений, как в предыдущей версии) ...
        if (road.getType() != RoadType.TUNNEL || tunnelLightDir0 == null || tunnelLightDir1 == null) return;
        tunnelPhaseTimer -= deltaTime;
        tunnelLightDir0.update(deltaTime);
        tunnelLightDir1.update(deltaTime);

        if (tunnelPhaseTimer <= 0) {
            switch (tunnelControlState) {
                case DIR0_GREEN:
                    tunnelControlState = TunnelControlState.DIR0_CLEARING;
                    tunnelPhaseTimer = parameters.getTunnelClearanceTime();
                    tunnelLightDir0.setCurrentState(TrafficLightState.RED, true);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel: DIR0_GREEN -> DIR0_CLEARING. Timer: " + tunnelPhaseTimer);
                    break;
                case DIR0_CLEARING:
                    if (!areCarsInTunnelActiveZone(0) || tunnelPhaseTimer <= -parameters.getTunnelClearanceTime() * 0.2) {
                        tunnelControlState = TunnelControlState.DIR1_GREEN;
                        tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
                        tunnelLightDir1.setCurrentState(TrafficLightState.GREEN, true);
                        if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel: DIR0_CLEARING -> DIR1_GREEN. Timer: " + tunnelPhaseTimer);
                    } else {
                        tunnelPhaseTimer = Math.max(tunnelPhaseTimer, 0.1);
                    }
                    break;
                case DIR1_GREEN:
                    tunnelControlState = TunnelControlState.DIR1_CLEARING;
                    tunnelPhaseTimer = parameters.getTunnelClearanceTime();
                    tunnelLightDir1.setCurrentState(TrafficLightState.RED, true);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel: DIR1_GREEN -> DIR1_CLEARING. Timer: " + tunnelPhaseTimer);
                    break;
                case DIR1_CLEARING:
                    if (!areCarsInTunnelActiveZone(1) || tunnelPhaseTimer <= -parameters.getTunnelClearanceTime() * 0.2) {
                        tunnelControlState = TunnelControlState.DIR0_GREEN;
                        tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
                        tunnelLightDir0.setCurrentState(TrafficLightState.GREEN, true);
                        if (ENGINE_DEBUG_LOGGING) System.out.println("Tunnel: DIR1_CLEARING -> DIR0_GREEN. Timer: " + tunnelPhaseTimer);
                    } else {
                        tunnelPhaseTimer = Math.max(tunnelPhaseTimer, 0.1);
                    }
                    break;
            }
        }
    }

    private boolean areCarsInTunnelActiveZone(int modelDirection) {
        // ... (без изменений, как в предыдущей версии) ...
        if (road == null || road.getCars() == null) return false;
        double entryLightPos = (modelDirection == 0) ? road.getLength() * 0.25 : road.getLength() * 0.75;
        double exitLightPos  = (modelDirection == 0) ? road.getLength() * 0.75 : road.getLength() * 0.25;
        double buffer = Car.APPROX_CAR_LENGTH * 0.5;

        for (Car car : road.getCars()) {
            if (car.getDirection() == modelDirection) {
                if (modelDirection == 0) {
                    if (car.getPosition() > entryLightPos + buffer && car.getPosition() < exitLightPos - buffer) return true;
                } else {
                    if (car.getPosition() < entryLightPos - buffer && car.getPosition() > exitLightPos + buffer) return true;
                }
            }
        }
        return false;
    }

    public void startSimulation() { if (running) return; running = true; paused = false; new Thread(this, "SimulationThread").start(); }
    public void stopSimulation() { running = false; synchronized (pauseLock) { paused = false; pauseLock.notifyAll(); } }
    public void pauseSimulation() { paused = true; }
    public void resumeSimulation() { synchronized (pauseLock) { paused = false; pauseLock.notifyAll(); } }

    @Override
    public void run() {
        // ... (без изменений, как в предыдущей версии) ...
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
    }

    private void step(double deltaTime) {
        // ... (без изменений в начале, как в предыдущей версии) ...
        if (deltaTime <= 0 || road == null) return;
        simulationTime += deltaTime;
        timeSinceLastLaneChangeCheck += deltaTime;

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
            if (car.isChangingLane()) {
                Car leadCarOnCurrentVisualLane = findLeadCarOnLocalLane(car, car.getCurrentLaneIndex(), currentCars);
                double distanceToLead = (leadCarOnCurrentVisualLane != null) ?
                        Math.max(0.01, Math.abs(leadCarOnCurrentVisualLane.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH)
                        : Double.POSITIVE_INFINITY;
                double effectiveSpeedLimit = findEffectiveSpeedLimit(car);
                TrafficLight nextLight = findNextTrafficLight(car);
                double distanceToLight = Double.POSITIVE_INFINITY;
                TrafficLightState nextLightState = null;
                if(nextLight != null) {
                    distanceToLight = Math.abs(nextLight.getPosition() - car.getPosition());
                    boolean carPassedLight = (car.getDirection() == 0 && car.getPosition() > nextLight.getPosition() + Car.APPROX_CAR_LENGTH * 0.3) ||
                            (car.getDirection() == 1 && car.getPosition() < nextLight.getPosition() - Car.APPROX_CAR_LENGTH * 0.3);
                    if (!carPassedLight) nextLightState = nextLight.getCurrentState();
                }
                car.update(deltaTime, leadCarOnCurrentVisualLane, distanceToLead, effectiveSpeedLimit, nextLightState, distanceToLight);
                continue;
            }

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

        if (timeSinceLastLaneChangeCheck >= LANE_CHANGE_CHECK_INTERVAL) {
            if (road.getType() != RoadType.TUNNEL && road.getLanesPerDirection() > 1) {
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
        // ... (без изменений, как в предыдущей версии) ...
        List<Car> eligibleCars = cars.stream()
                .filter(c -> road.getType() != RoadType.TUNNEL && road.getLanesPerDirection() > 1 && !c.isChangingLane())
                .collect(Collectors.toList());

        for (Car car : eligibleCars) {
            int currentLocalLane = car.getCurrentLaneIndex();
            double currentObservedAcceleration = calculatePotentialAccelerationOnLocalLane(car, currentLocalLane, cars);

            boolean isRightmost = (currentLocalLane == 0);
            boolean isLeftmost = (currentLocalLane == road.getLanesPerDirection() - 1);

            Double leftBenefit = null;
            int targetLocalLeftLane = -1;
            boolean safeToChangeLeft = false;
            if (!isLeftmost) {
                targetLocalLeftLane = currentLocalLane + 1;
                leftBenefit = calculatePotentialAccelerationOnLocalLane(car, targetLocalLeftLane, cars);
                safeToChangeLeft = isLaneChangeSafe(car, targetLocalLeftLane, cars);
            }

            Double rightBenefit = null;
            int targetLocalRightLane = -1;
            boolean safeToChangeRight = false;
            if (!isRightmost) {
                targetLocalRightLane = currentLocalLane - 1;
                rightBenefit = calculatePotentialAccelerationOnLocalLane(car, targetLocalRightLane, cars);
                safeToChangeRight = isLaneChangeSafe(car, targetLocalRightLane, cars);
            }

            car.evaluateLaneChangeDecision(currentObservedAcceleration, leftBenefit, rightBenefit, isRightmost, isLeftmost, safeToChangeLeft, safeToChangeRight);

            int finalTargetLocalLaneToInitiate = -1;

            if (car.wantsToChangeLaneLeft()) {
                if (targetLocalLeftLane != -1 && safeToChangeLeft) {
                    finalTargetLocalLaneToInitiate = targetLocalLeftLane;
                } else {
                    car.resetLaneChangeDesire();
                }
            } else if (car.wantsToChangeLaneRight()) {
                if (targetLocalRightLane != -1 && safeToChangeRight) {
                    finalTargetLocalLaneToInitiate = targetLocalRightLane;
                } else {
                    car.resetLaneChangeDesire();
                }
            }

            if (finalTargetLocalLaneToInitiate != -1) {
                car.startLaneChange(finalTargetLocalLaneToInitiate);
            }
        }
    }

    private double calculatePotentialAccelerationOnLocalLane(Car car, int targetLocalLaneIndex, List<Car> allCars) {
        // ... (без изменений, как в предыдущей версии) ...
        Car leadCarOnTargetLane = findLeadCarOnLocalLane(car, targetLocalLaneIndex, allCars);
        double distanceToLead = (leadCarOnTargetLane != null) ? Math.max(0.01, Math.abs(leadCarOnTargetLane.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH) : Double.POSITIVE_INFINITY;
        double desiredSpeed = car.getDesiredSpeed();
        double currentSpeed = car.getCurrentSpeed();
        double accelParam = car.getAccelerationParam();
        double decelParam = car.getBaseDecelerationParam();
        double deltaV_hypothetical = (leadCarOnTargetLane != null) ? currentSpeed - leadCarOnTargetLane.getCurrentSpeed() : 0;
        double s_star = Car.MIN_GAP + Math.max(0, currentSpeed * Car.SAFE_TIME_HEADWAY + (currentSpeed * deltaV_hypothetical) / (2 * Math.sqrt(accelParam * decelParam)));
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

    private boolean isLaneChangeSafe(Car car, int targetLocalLaneIndex, List<Car> allCars) {
        // Немного увеличим требуемые зазоры для большей безопасности
        final double MIN_GAP_MULTIPLIER_LEADER = 1.0; // Был 0.8, потом 1.2. Оставим 1.0 для лидера (не слишком близко)
        final double MIN_GAP_MULTIPLIER_FOLLOWER = 0.9; // Был 0.8. Чуть больше для последователя

        Car newLeader = findLeadCarOnLocalLane(car, targetLocalLaneIndex, allCars);
        if (newLeader != null) {
            double distanceToNewLeader = Math.abs(newLeader.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH;
            if (distanceToNewLeader < Car.MIN_GAP * MIN_GAP_MULTIPLIER_LEADER) {
                return false;
            }
        }

        Car newFollower = findFollowerOnLocalLane(car, targetLocalLaneIndex, allCars);
        if (newFollower != null) {
            double distanceCarToNewFollower = Math.abs(car.getPosition() - newFollower.getPosition()) - Car.APPROX_CAR_LENGTH;
            if (distanceCarToNewFollower < Car.MIN_GAP * MIN_GAP_MULTIPLIER_FOLLOWER) {
                return false;
            }

            double followerSpeed = newFollower.getCurrentSpeed();
            double followerDesiredSpeed = newFollower.getDesiredSpeed();
            double followerAccel = newFollower.getAccelerationParam();
            double followerDecel = newFollower.getBaseDecelerationParam();
            // Оцениваем ускорение последователя, ЕСЛИ БЫ наша машина была перед ним
            double deltaV_follower_hypothetical = followerSpeed - car.getCurrentSpeed(); // Скорость последователя - НАША ТЕКУЩАЯ скорость
            double s_star_follower = Car.MIN_GAP + Math.max(0, followerSpeed * Car.SAFE_TIME_HEADWAY +
                    (followerSpeed * deltaV_follower_hypothetical) / (2 * Math.sqrt(followerAccel * followerDecel)));
            double freeRoadFollower = followerAccel * (1 - Math.pow(followerSpeed / Math.max(0.1, followerDesiredSpeed), Car.ACCELERATION_EXPONENT));
            double interactionFollower = -followerAccel * Math.pow(s_star_follower / Math.max(0.1, distanceCarToNewFollower), 2);
            double potentialAccelerationFollower = freeRoadFollower + interactionFollower;

            if (potentialAccelerationFollower < -Car.SAFE_DECELERATION_FOR_OTHERS) {
                return false;
            }
        }
        return true;
    }

    private Car findLeadCarOnLocalLane(Car currentCar, int targetLocalLaneIndex, List<Car> allCars) {
        // ... (без изменений, как в предыдущей версии) ...
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
        // ... (без изменений, как в предыдущей версии) ...
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
        // ... (без изменений, как в предыдущей версии) ...
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
        // ... (без изменений, как в предыдущей версии) ...
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
        this.parameters = newParams;
        this.initializeSimulation();
    }

    public Road getRoad() { return road; }
    public double getSimulationTime() { return simulationTime; }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
}
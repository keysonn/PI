package com.trafficsimulation.simulation;

import com.trafficsimulation.gui.SimulationPanel;
import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadSign;
import com.trafficsimulation.model.RoadType;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;

import javax.swing.SwingUtilities;
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
    private static final int CAR_RENDER_WIDTH_APPROX = 30;


    public SimulationEngine(SimulationParameters params, SimulationPanel panel) {
        this.parameters = params;
        this.simulationPanel = panel;
        initializeSimulation();
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Конструктор выполнен, initializeSimulation() вызвана.");
    }

    public void initializeSimulation() {
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
        this.tunnelLightDir0 = null;
        this.tunnelLightDir1 = null;

        if (this.road != null && this.road.getTrafficLights() != null && !this.road.getTrafficLights().isEmpty()) {
            if (isTunnelActive) {
                this.road.clearTrafficLights(); // Для тоннеля всегда свои светофоры
            }
        }
        // Также очищаем знаки, если переинициализируем дорогу (особенно для тоннеля)
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

    public void startSimulation() { if (running) return; running = true; paused = false; new Thread(this, "SimulationThread").start(); if(ENGINE_DEBUG_LOGGING) System.out.println("Поток симуляции запущен."); }
    public void stopSimulation() { running = false; synchronized (pauseLock) { paused = false; pauseLock.notifyAll(); } if(ENGINE_DEBUG_LOGGING) System.out.println("Симуляция остановлена."); }
    public void pauseSimulation() { paused = true; if(ENGINE_DEBUG_LOGGING) System.out.println("Симуляция на паузе."); }
    public void resumeSimulation() { synchronized (pauseLock) { paused = false; pauseLock.notifyAll(); } if(ENGINE_DEBUG_LOGGING) System.out.println("Симуляция снята с паузы."); }

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
        if (isTunnelActive) {
            updateTunnelLogic(deltaTime);
        } else {
            List<TrafficLight> lights = road.getTrafficLights();
            if (lights != null) {
                for (TrafficLight light : lights) {
                    if (!light.isExternallyControlled()) {
                        light.update(deltaTime);
                    }
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
        List<Car> currentCars = road.getCars();
        if (currentCars != null) {
            final double APPROX_CAR_LENGTH = 5.0;
            for (Car car : currentCars) {
                Car leadCar = findLeadCar(car, currentCars);
                double distanceToLeadBumperToBumper = (leadCar != null) ?
                        Math.max(0.01, Math.abs(leadCar.getPosition() - car.getPosition()) - APPROX_CAR_LENGTH) : Double.POSITIVE_INFINITY;
                double effectiveSpeedLimit = findEffectiveSpeedLimit(car);
                TrafficLight nextLight = findNextTrafficLight(car);
                double distanceToLightAbs = Double.POSITIVE_INFINITY;
                TrafficLightState nextLightState = null;
                if (nextLight != null) {
                    distanceToLightAbs = Math.abs(nextLight.getPosition() - car.getPosition());
                    boolean carPassedLight;
                    if (car.getDirection() == 0) {
                        carPassedLight = car.getPosition() > nextLight.getPosition() + APPROX_CAR_LENGTH * 0.3;
                    } else {
                        carPassedLight = car.getPosition() < nextLight.getPosition() - APPROX_CAR_LENGTH * 0.3;
                    }
                    if (carPassedLight) {
                        nextLightState = null;
                        distanceToLightAbs = Double.POSITIVE_INFINITY;
                    } else {
                        nextLightState = nextLight.getCurrentState();
                    }
                }
                car.update(deltaTime, leadCar, distanceToLeadBumperToBumper, effectiveSpeedLimit, nextLightState, distanceToLightAbs);
            }
        }
        if (road.getCars() != null) {
            double removalBuffer = CAR_RENDER_WIDTH_APPROX * 1.5;
            road.getCars().removeIf(car ->
                    (car.getDirection() == 0 && car.getPosition() > road.getLength() + removalBuffer) ||
                            (car.getDirection() == 1 && car.getPosition() < -removalBuffer)
            );
        }
    }

    private Car findLeadCar(Car currentCar, List<Car> allCars) {
        if (allCars == null) return null;
        Car leader = null;
        double minPositiveDistance = Double.POSITIVE_INFINITY;
        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() ||
                    otherCar.getLaneIndex() != currentCar.getLaneIndex() ||
                    otherCar.getDirection() != currentCar.getDirection()) {
                continue;
            }
            double distance;
            if (currentCar.getDirection() == 0) {
                distance = otherCar.getPosition() - currentCar.getPosition();
            } else {
                distance = currentCar.getPosition() - otherCar.getPosition();
            }
            if (distance > 0.01 && distance < minPositiveDistance) {
                minPositiveDistance = distance;
                leader = otherCar;
            }
        }
        return leader;
    }

    private double findEffectiveSpeedLimit(Car car) {
        RoadType currentRoadType = road.getType();
        double roadTypeMaxSpeedMs = currentRoadType.getMaxSpeedLimitMs();
        double roadTypeMinSpeedMs = currentRoadType.getMinSpeedLimitMs();
        double carPersonalMaxSpeedMs = car.getMaxSpeed();
        double effectiveLimitMs = Math.min(roadTypeMaxSpeedMs, carPersonalMaxSpeedMs);
        List<RoadSign> signs = road.getRoadSigns();

        if (signs != null && !signs.isEmpty()) {
            double activeSignLimitMs = -1.0; // Лимит от последнего действующего знака

            if (car.getDirection() == 0) { // Машина едет вправо
                for (RoadSign sign : signs) { // Знаки отсортированы по позиции
                    // Знак действует, если он для направления машины (0) ИЛИ для обоих (-1)
                    if (sign.getTargetDirection() == 1) continue; // Пропускаем знаки только для встречного (1)

                    if (sign.getPosition() <= car.getPosition() + 1.0) { // Знак уже пройден или очень близко перед машиной
                        double limitFromSign = sign.getSpeedLimitValue(); // в м/с
                        if (limitFromSign >= 0) { // Если это валидный знак ограничения
                            activeSignLimitMs = limitFromSign; // Этот знак становится текущим действующим
                        }
                    } else {
                        // Знаки отсортированы, все последующие будут дальше впереди, они еще не действуют
                        break;
                    }
                }
            } else { // Машина едет влево (направление 1)
                for (int i = signs.size() - 1; i >= 0; i--) { // Идем по знакам справа налево (по их позициям на дороге)
                    RoadSign sign = signs.get(i);
                    // Знак действует, если он для направления машины (1) ИЛИ для обоих (-1)
                    if (sign.getTargetDirection() == 0) continue; // Пропускаем знаки только для встречного (0)

                    if (sign.getPosition() >= car.getPosition() - 1.0) { // Знак справа от машины или очень близко за ней
                        double limitFromSign = sign.getSpeedLimitValue();
                        if (limitFromSign >= 0) {
                            activeSignLimitMs = limitFromSign; // Этот знак становится текущим действующим
                        }
                    } else {
                        // Машина уже проехала все знаки левее (по направлению ее движения), они не актуальны
                        break;
                    }
                }
            }
            // Если был найден действующий знак, применяем его ограничение
            if (activeSignLimitMs >= 0) {
                effectiveLimitMs = Math.min(effectiveLimitMs, activeSignLimitMs);
            }
        }
        // Финальное ограничение: не ниже минимально допустимой для типа дороги и не выше персональной максималки
        return Math.max(roadTypeMinSpeedMs, Math.min(effectiveLimitMs, carPersonalMaxSpeedMs));
    }

    private TrafficLight findNextTrafficLight(Car car) {
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
        this.parameters = newParams;
        this.initializeSimulation();
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Параметры обновлены и симуляция переинициализирована.");
    }

    public Road getRoad() { return road; }
    public double getSimulationTime() { return simulationTime; }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    private int getGuiPanelWidth() {
        if (simulationPanel != null) {
            return simulationPanel.getWidth();
        }
        return 800;
    }
}
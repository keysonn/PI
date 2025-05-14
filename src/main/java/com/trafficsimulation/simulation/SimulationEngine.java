package com.trafficsimulation.simulation;

import com.trafficsimulation.gui.SimulationPanel;
import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadSign;
import com.trafficsimulation.model.RoadSignType;
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

    public SimulationEngine(SimulationParameters params, SimulationPanel panel) {
        this.parameters = params;
        this.simulationPanel = panel;
        initializeSimulation();
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Конструктор выполнен, initialisation() вызвана.");
    }

    public void initializeSimulation() {
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Начало initializeSimulation(). Параметры: " + parameters);
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

        if (isTunnelActive) {
            if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Создание светофоров для тоннеля.");
            tunnelLightDir0 = new TrafficLight(0.0, parameters.getTunnelDefaultRedDuration(), parameters.getTunnelDefaultGreenDuration(), TrafficLightState.GREEN, 0);
            tunnelLightDir0.setExternallyControlled(true);
            this.road.addTrafficLight(tunnelLightDir0);

            tunnelLightDir1 = new TrafficLight(this.road.getLength(), parameters.getTunnelDefaultRedDuration(), parameters.getTunnelDefaultGreenDuration(), TrafficLightState.RED, 1);
            tunnelLightDir1.setExternallyControlled(true);
            this.road.addTrafficLight(tunnelLightDir1);

            tunnelControlState = TunnelControlState.DIR0_GREEN;
            tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
            if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Тоннель активирован. Начальное состояние: " + tunnelControlState + ", таймер фазы: " + String.format("%.2f", tunnelPhaseTimer) + "с");
        }
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Конец initializeSimulation(). Дорога создана: " + this.road);
    }

    private void updateTunnelLogic(double deltaTime) {
        if (!isTunnelActive || tunnelLightDir0 == null || tunnelLightDir1 == null) {
            return;
        }
        tunnelPhaseTimer -= deltaTime;
        if (tunnelPhaseTimer <= 0) {
            if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: Таймер фазы истек. Текущее состояние: " + tunnelControlState);
            switch (tunnelControlState) {
                case DIR0_GREEN:
                    tunnelControlState = TunnelControlState.DIR0_CLEARING;
                    tunnelPhaseTimer = parameters.getTunnelClearanceTime();
                    tunnelLightDir0.setCurrentState(TrafficLightState.RED, true);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: -> DIR0_CLEARING. Таймер: " + String.format("%.2f", tunnelPhaseTimer) + "с. Св0->Красный.");
                    break;
                case DIR0_CLEARING:
                    boolean carsInDir0 = areCarsInTunnel(0);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: Проверка DIR0_CLEARING. Машины напр.0 в тоннеле: " + carsInDir0);
                    if (carsInDir0) {
                        tunnelPhaseTimer = 1.0;
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
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: Проверка DIR1_CLEARING. Машины напр.1 в тоннеле: " + carsInDir1);
                    if (carsInDir1) {
                        tunnelPhaseTimer = 1.0;
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
        double entryMargin = 0.1;
        double exitMargin = road.getLength() - 0.1;
        for (Car car : road.getCars()) {
            if (car.getDirection() == direction) {
                if (direction == 0 && car.getPosition() > entryMargin && car.getPosition() < exitMargin) return true;
                else if (direction == 1 && car.getPosition() < exitMargin && car.getPosition() > entryMargin) return true;
            }
        }
        return false;
    }

    public void startSimulation() { if (running) return; running = true; paused = false; new Thread(this, "SimulationThread").start(); System.out.println("Поток симуляции запущен."); }
    public void stopSimulation() { running = false; synchronized (pauseLock) { paused = false; pauseLock.notifyAll(); } System.out.println("Симуляция остановлена."); }
    public void pauseSimulation() { paused = true; System.out.println("Симуляция на паузе."); }
    public void resumeSimulation() { synchronized (pauseLock) { paused = false; pauseLock.notifyAll(); } System.out.println("Симуляция снята с паузы."); }

    @Override
    public void run() {
        long lastUpdateTime = System.nanoTime();
        while (running) {
            synchronized (pauseLock) {
                if (paused) {
                    try { pauseLock.wait(); if (!running) break; lastUpdateTime = System.nanoTime(); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); running = false; break; }
                }
            }

            long currentTimeNano = System.nanoTime(); // Используем новое имя для текущего времени в наносекундах
            long elapsedTimeNano = currentTimeNano - lastUpdateTime; // Здесь elapsedTimeNano объявляется и используется правильно
            lastUpdateTime = currentTimeNano;

            double deltaTime = elapsedTimeNano / 1_000_000_000.0;
            double simulationDeltaTime = deltaTime * parameters.getSimulationSpeedFactor();

            if (road != null) {
                step(simulationDeltaTime);
            }

            if (simulationPanel != null) {
                Road currentRoadForPanel = road;
                double currentSimTimeForPanel = simulationTime;
                SwingUtilities.invokeLater(() -> simulationPanel.updateSimulationState(currentRoadForPanel, currentSimTimeForPanel));
            }

            try {
                // Используем elapsedTimeNano, которая была рассчитана выше
                long sleepTime = Math.max(0, (long) (parameters.getSimulationTimeStep() * 1000 / parameters.getSimulationSpeedFactor() - (elapsedTimeNano / 1_000_000)));
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                } else {
                    Thread.yield();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        System.out.println("Поток симуляции завершил работу.");
    }

    private void step(double deltaTime) {
        if (deltaTime <= 0 || road == null) return;
        simulationTime += deltaTime;
        if (isTunnelActive) updateTunnelLogic(deltaTime);
        List<TrafficLight> lights = road.getTrafficLights();
        if (lights != null) {
            for (TrafficLight light : lights) light.update(deltaTime);
        }
        if (flowGenerator != null) {
            TunnelControlState currentTunnelStateForGenerator = isTunnelActive ? this.tunnelControlState : null;
            Car[] newCars = flowGenerator.generateCars(deltaTime, currentTunnelStateForGenerator);
            if (newCars != null) for (Car newCar : newCars) if (newCar != null) road.addCar(newCar);
        }
        List<Car> currentCars = road.getCars();
        if (currentCars != null) {
            final double APPROX_CAR_LENGTH = 5.0;
            for (Car car : currentCars) {
                Car leadCar = findLeadCar(car, currentCars, car.getDirection());
                double distanceToLeadBumperToBumper = (leadCar != null) ? Math.max(0.1, Math.abs(leadCar.getPosition() - car.getPosition()) - APPROX_CAR_LENGTH) : Double.POSITIVE_INFINITY;
                double effectiveSpeedLimit = findEffectiveSpeedLimit(car, car.getDirection());
                TrafficLight nextLight = findNextTrafficLight(car, car.getDirection());
                double distanceToLightAbs = (nextLight != null) ? Math.abs(nextLight.getPosition() - car.getPosition()) : Double.POSITIVE_INFINITY;
                TrafficLightState nextLightState = (nextLight != null) ? nextLight.getCurrentState() : null;
                car.update(deltaTime, leadCar, distanceToLeadBumperToBumper, effectiveSpeedLimit, nextLightState, distanceToLightAbs);
            }
        }
        if (road.getCars() != null) road.getCars().removeIf(car -> (car.getDirection() == 0) ? car.getPosition() > road.getLength() + 20 : car.getPosition() < -20);
    }

    private Car findLeadCar(Car currentCar, List<Car> allCars, int carDirection) {
        if (allCars == null) return null;
        Car leader = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() || otherCar.getLaneIndex() != currentCar.getLaneIndex() || otherCar.getDirection() != carDirection) continue;
            double distance = (carDirection == 0) ? (otherCar.getPosition() - currentCar.getPosition()) : (currentCar.getPosition() - otherCar.getPosition());
            if (distance > 0 && distance < minDistance) {
                minDistance = distance;
                leader = otherCar;
            }
        }
        return leader;
    }

    private double findEffectiveSpeedLimit(Car car, int carDirection) {
        List<RoadSign> signs = road.getRoadSigns();
        double carMaxSpeed = car.getMaxSpeed();
        if (signs == null || signs.isEmpty()) return carMaxSpeed;
        double activeLimit = carMaxSpeed;
        if (carDirection == 0) {
            for (RoadSign sign : signs) {
                if (sign.getTargetDirection() != 0 && sign.getTargetDirection() != -1) continue;
                if (sign.getPosition() <= car.getPosition()) {
                    double limitFromSign = sign.getSpeedLimitValue();
                    if (limitFromSign >= 0) activeLimit = limitFromSign;
                } else break;
            }
        } else { // carDirection == 1
            activeLimit = carMaxSpeed;
            for (RoadSign sign : signs) {
                if (sign.getTargetDirection() != 1 && sign.getTargetDirection() != -1) continue;
                if (sign.getPosition() >= car.getPosition()) {
                    double limitFromSign = sign.getSpeedLimitValue();
                    if (limitFromSign >= 0) { activeLimit = limitFromSign; break; }
                }
            }
        }
        return Math.min(activeLimit, carMaxSpeed);
    }

    private TrafficLight findNextTrafficLight(Car car, int carDirection) {
        List<TrafficLight> lights = road.getTrafficLights();
        if (lights == null) return null;
        TrafficLight nextFoundLight = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (TrafficLight light : lights) {
            if (light.getTargetDirection() != carDirection && light.getTargetDirection() != -1) continue;
            double distance = (carDirection == 0) ? (light.getPosition() - car.getPosition()) : (car.getPosition() - light.getPosition());
            if (distance > 0 && distance < minDistance) {
                minDistance = distance;
                nextFoundLight = light;
            }
        }
        return nextFoundLight;
    }

    public void updateParameters(SimulationParameters newParams) {
        this.parameters = newParams;
        if (this.flowGenerator != null) {
            this.flowGenerator.updateParameters(newParams);
        }
    }

    public Road getRoad() { return road; }
    public double getSimulationTime() { return simulationTime; }
}
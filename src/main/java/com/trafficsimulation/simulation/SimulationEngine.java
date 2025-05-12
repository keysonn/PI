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

    public SimulationEngine(SimulationParameters params, SimulationPanel panel) {
        this.parameters = params;
        this.simulationPanel = panel;
        initializeSimulation();
        System.out.println("SimulationEngine создан.");
    }

    public void initializeSimulation() {
        System.out.println("Инициализация симуляции с параметрами: " + parameters);
        this.road = new Road(
                parameters.getRoadLengthKm(),
                parameters.getRoadType(),
                parameters.getLanesPerDirection(),
                parameters.getNumberOfDirections()
        );
        this.flowGenerator = new TrafficFlowGenerator(parameters, this.road);
        this.simulationTime = 0.0;
        // Светофоры и знаки по умолчанию удалены, добавляются пользователем через GUI
    }

    public void startSimulation() {
        if (running) return;
        running = true;
        paused = false;
        Thread simulationThread = new Thread(this, "SimulationThread");
        simulationThread.start();
        System.out.println("Поток симуляции запущен.");
    }

    public void stopSimulation() {
        running = false;
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        System.out.println("Симуляция остановлена.");
    }

    public void pauseSimulation() {
        paused = true;
        System.out.println("Симуляция на паузе.");
    }

    public void resumeSimulation() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        System.out.println("Симуляция снята с паузы.");
    }

    @Override
    public void run() {
        long lastUpdateTime = System.nanoTime();
        while (running) {
            synchronized (pauseLock) {
                if (paused) {
                    try {
                        pauseLock.wait();
                        if (!running) break;
                        lastUpdateTime = System.nanoTime();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running = false;
                        break;
                    }
                }
            }

            long currentTime = System.nanoTime();
            long elapsedTimeNano = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
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
                long sleepTime = Math.max(0, (long) (parameters.getSimulationTimeStep() * 1000 / parameters.getSimulationSpeedFactor() - elapsedTimeNano / 1_000_000));
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

        if (flowGenerator != null) {
            Car[] newCars = flowGenerator.generateCars(deltaTime);
            if (newCars != null) {
                for (Car newCar : newCars) {
                    if (newCar != null) road.addCar(newCar);
                }
            }
        }

        List<TrafficLight> lights = road.getTrafficLights();
        if (lights != null) {
            for (TrafficLight light : lights) light.update(deltaTime);
        }

        List<Car> currentCars = road.getCars();
        if (currentCars != null) {
            final double APPROX_CAR_LENGTH = 5.0;
            for (Car car : currentCars) {
                Car leadCar = findLeadCar(car, currentCars, car.getDirection());
                double distanceToLeadBumperToBumper;
                if (leadCar != null) {
                    double distBetweenFronts = Math.abs(leadCar.getPosition() - car.getPosition());
                    distanceToLeadBumperToBumper = distBetweenFronts - APPROX_CAR_LENGTH;
                    distanceToLeadBumperToBumper = Math.max(0.1, distanceToLeadBumperToBumper);
                } else {
                    distanceToLeadBumperToBumper = Double.POSITIVE_INFINITY;
                }
                double effectiveSpeedLimit = findEffectiveSpeedLimit(car, car.getDirection());
                TrafficLight nextLight = findNextTrafficLight(car, car.getDirection());
                double distanceToLightAbs = (nextLight != null) ? Math.abs(nextLight.getPosition() - car.getPosition()) : Double.POSITIVE_INFINITY;
                TrafficLightState nextLightState = (nextLight != null) ? nextLight.getCurrentState() : null;
                car.update(deltaTime, leadCar, distanceToLeadBumperToBumper, effectiveSpeedLimit, nextLightState, distanceToLightAbs);
            }
        }

        if (road.getCars() != null) {
            road.getCars().removeIf(car -> (car.getDirection() == 0) ? car.getPosition() > road.getLength() + 20 : car.getPosition() < -20);
        }
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
        if (signs == null || signs.isEmpty()) {
            return carMaxSpeed;
        }

        double activeLimit = carMaxSpeed;

        if (carDirection == 0) { // Машина едет слева направо (позиция растет)
            // Ищем последний знак, позиция которого <= позиции машины и который для этого направления
            for (RoadSign sign : signs) {
                if (sign.getTargetDirection() != 0 && sign.getTargetDirection() != -1) continue; // -1 для общих знаков (пока не используем)

                if (sign.getPosition() <= car.getPosition()) {
                    double limitFromSign = sign.getSpeedLimitValue();
                    if (limitFromSign >= 0) {
                        activeLimit = limitFromSign;
                    }
                } else {
                    // Так как знаки отсортированы, все последующие будут еще дальше впереди
                    break;
                }
            }
        } else { // Машина едет справа налево (carDirection == 1, позиция убывает)
            // Ищем первый знак (т.к. они отсортированы по возрастанию позиции),
            // позиция которого >= позиции машины и который для этого направления.
            // Этот знак и будет текущим действующим ограничением.
            activeLimit = carMaxSpeed; // По умолчанию, если не найдем знаков справа/на позиции
            for (RoadSign sign : signs) {
                if (sign.getTargetDirection() != 1 && sign.getTargetDirection() != -1) continue;

                if (sign.getPosition() >= car.getPosition()) {
                    double limitFromSign = sign.getSpeedLimitValue();
                    if (limitFromSign >= 0) {
                        activeLimit = limitFromSign;
                        break; // Нашли первый подходящий знак справа (или на позиции), он и действует
                    }
                }
                // Если мы прошли все знаки, и ни один не был >= car.position,
                // значит, все знаки слева от машины (по ходу движения машины - они уже позади).
                // В этом случае должен действовать последний проехавший знак.
                // Для этого нужно было бы запоминать *последний* знак, который был sign.position < car.position.
                // Но для упрощенной модели: если справа нет знаков, действует carMaxSpeed.
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
            // Проверяем, что светофор для нужного направления
            // (можно добавить поддержку "общего" светофора с targetDirection = -1)
            if (light.getTargetDirection() != carDirection && light.getTargetDirection() != -1) {
                continue;
            }

            double distance;
            if (carDirection == 0) { // Слева направо
                distance = light.getPosition() - car.getPosition();
            } else { // Справа налево
                distance = car.getPosition() - light.getPosition();
            }

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
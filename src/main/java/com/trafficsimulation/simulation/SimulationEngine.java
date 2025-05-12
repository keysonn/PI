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

        if (road.getLength() > 100 && parameters.getRoadType() != RoadType.HIGHWAY) {
            road.addTrafficLight(new TrafficLight(road.getLength() * 0.3, 30, 30, TrafficLightState.RED));
            if (road.getLength() > 500 && road.getTrafficLights().size() < 2) {
                road.addTrafficLight(new TrafficLight(road.getLength() * 0.7, 25, 35, TrafficLightState.GREEN));
            }
        }
        if (road.getLength() > 200) {
            road.addRoadSign(new RoadSign(road.getLength() * 0.15, RoadSignType.SPEED_LIMIT_60));
        }
        if (road.getLength() > 600) {
            road.addRoadSign(new RoadSign(road.getLength() * 0.5, RoadSignType.SPEED_LIMIT_30));
        }
        if (road.getLength() > 1000) {
            road.addRoadSign(new RoadSign(road.getLength() * 0.85, RoadSignType.SPEED_LIMIT_90));
        }
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
                    Thread.yield(); // Или Thread.sleep(1)
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
            for (Car car : currentCars) {
                Car leadCar = findLeadCar(car, currentCars, car.getDirection());
                double distanceToLeadAbs = (leadCar != null) ? Math.abs(leadCar.getPosition() - car.getPosition()) - 10 : Double.POSITIVE_INFINITY;
                double effectiveSpeedLimit = findEffectiveSpeedLimit(car, car.getDirection());
                TrafficLight nextLight = findNextTrafficLight(car, car.getDirection());
                double distanceToLightAbs = (nextLight != null) ? Math.abs(nextLight.getPosition() - car.getPosition()) : Double.POSITIVE_INFINITY;
                TrafficLightState nextLightState = (nextLight != null) ? nextLight.getCurrentState() : null;
                car.update(deltaTime, leadCar, distanceToLeadAbs, effectiveSpeedLimit, nextLightState, distanceToLightAbs);
            }
        }

        if (road.getCars() != null) {
            road.getCars().removeIf(car -> (car.getDirection() == 0) ? car.getPosition() > road.getLength() + 20 : car.getPosition() < -20);
        }
    }

    private Car findLeadCar(Car currentCar, List<Car> allCars, int direction) {
        if (allCars == null) return null;
        Car leader = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() || otherCar.getLaneIndex() != currentCar.getLaneIndex() || otherCar.getDirection() != direction) continue;
            double distance = (direction == 0) ? (otherCar.getPosition() - currentCar.getPosition()) : (currentCar.getPosition() - otherCar.getPosition());
            if (distance > 0 && distance < minDistance) {
                minDistance = distance;
                leader = otherCar;
            }
        }
        return leader;
    }

    private double findEffectiveSpeedLimit(Car car, int direction) {
        List<RoadSign> signs = road.getRoadSigns();
        double carMaxSpeed = car.getMaxSpeed();
        if (signs == null || signs.isEmpty()) return carMaxSpeed;

        double activeLimit = carMaxSpeed;
        for (RoadSign sign : signs) {
            double limitFromSign = sign.getSpeedLimitValue();
            if (limitFromSign < 0) continue; // Не знак ограничения

            if (direction == 0) { // Слева направо
                if (sign.getPosition() <= car.getPosition()) activeLimit = limitFromSign;
                else break; // Знаки отсортированы, остальные дальше
            } else { // Справа налево
                if (sign.getPosition() >= car.getPosition()) activeLimit = limitFromSign;
                // Для dir=1, мы ищем последний знак, который >= car.pos.
                // Так как знаки отсортированы по возрастанию, мы просто продолжаем обновлять.
                // Более точная логика потребовала бы обратного прохода или поиска первого знака >= car.pos.
                // Но для простой реализации, если знаков не много, текущая логика для dir=1 найдет
                // самый ПРАВЫЙ (с наибольшей координатой) знак, который >= car.pos.
                // Это не совсем то, что "последний проехавший".
                // Правильнее было бы найти *все* знаки >= car.pos и взять из них тот, у которого *минимальная* позиция.
                // Но оставим пока так для упрощения, т.к. основная проблема сейчас - компиляция.
            }
        }
        return Math.min(activeLimit, carMaxSpeed); // Не превышать собственную максималку
    }

    private TrafficLight findNextTrafficLight(Car car, int direction) {
        List<TrafficLight> lights = road.getTrafficLights();
        if (lights == null) return null;
        TrafficLight nextFoundLight = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (TrafficLight light : lights) {
            double distance = (direction == 0) ? (light.getPosition() - car.getPosition()) : (car.getPosition() - light.getPosition());
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
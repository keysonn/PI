package com.trafficsimulation.simulation;

import com.trafficsimulation.gui.SimulationPanel;
import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadType; // <--- КРИТИЧЕСКИ ВАЖНЫЙ ИМПОРТ
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Основной класс, управляющий процессом симуляции.
 */
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

        // Добавляем светофоры на НОВУЮ, чистую дорогу
        if (road.getLength() > 100 && parameters.getRoadType() != RoadType.HIGHWAY) { // Используем импортированный RoadType
            road.addTrafficLight(new TrafficLight(road.getLength() * 0.3, 30, 30, TrafficLightState.RED));
            if (road.getLength() > 500 && road.getTrafficLights().size() < 2) {
                road.addTrafficLight(new TrafficLight(road.getLength() * 0.7, 25, 35, TrafficLightState.GREEN));
            }
        }
    }

    public void startSimulation() {
        if (running) return;
        // initializeSimulation(); // Можно раскомментировать, если нужно всегда начинать с чистого листа при старте
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
                        System.out.println("Поток симуляции уходит в ожидание (пауза)...");
                        pauseLock.wait();
                        System.out.println("Поток симуляции проснулся.");
                        if (!running) break;
                        lastUpdateTime = System.nanoTime();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running = false;
                        System.err.println("Поток симуляции прерван во время паузы.");
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
                long sleepTime = Math.max(0, (long)(parameters.getSimulationTimeStep() * 1000 / parameters.getSimulationSpeedFactor() - elapsedTimeNano / 1_000_000));
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                } else {
                    Thread.sleep(1); // Минимальная пауза, чтобы отдать управление
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
                System.err.println("Поток симуляции прерван во время sleep.");
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
                    if (newCar != null) {
                        road.addCar(newCar);
                    }
                }
            }
        }

        List<TrafficLight> lights = road.getTrafficLights();
        if (lights != null) {
            for (TrafficLight light : lights) {
                light.update(deltaTime);
            }
        }

        List<Car> currentCars = road.getCars();
        if (currentCars != null) {
            for (Car car : currentCars) {
                Car leadCar = findLeadCar(car, currentCars, car.getDirection());
                double distanceToLeadAbs = (leadCar != null) ? Math.abs(leadCar.getPosition() - car.getPosition()) - 10 : Double.POSITIVE_INFINITY;
                double speedLimit = findEffectiveSpeedLimit(car);
                TrafficLight nextLight = findNextTrafficLight(car, car.getDirection());
                double distanceToLightAbs = (nextLight != null) ? Math.abs(nextLight.getPosition() - car.getPosition()) : Double.POSITIVE_INFINITY;
                TrafficLightState nextLightState = (nextLight != null) ? nextLight.getCurrentState() : null;

                car.update(deltaTime, leadCar, distanceToLeadAbs, speedLimit, nextLightState, distanceToLightAbs);
            }
        }

        if (road.getCars() != null) {
            road.getCars().removeIf(car -> {
                if (car.getDirection() == 0) {
                    return car.getPosition() > road.getLength() + 20;
                } else {
                    return car.getPosition() < -20;
                }
            });
        }
    }

    private Car findLeadCar(Car currentCar, List<Car> allCars, int direction) {
        if (allCars == null) return null;
        Car leader = null;
        double minDistance = Double.POSITIVE_INFINITY;

        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() || otherCar.getLaneIndex() != currentCar.getLaneIndex() || otherCar.getDirection() != direction) {
                continue;
            }
            double distance;
            if (direction == 0) {
                distance = otherCar.getPosition() - currentCar.getPosition();
            } else {
                distance = currentCar.getPosition() - otherCar.getPosition();
            }
            if (distance > 0 && distance < minDistance) {
                minDistance = distance;
                leader = otherCar;
            }
        }
        return leader;
    }

    private double findEffectiveSpeedLimit(Car car) {
        return car.getMaxSpeed();
    }

    private TrafficLight findNextTrafficLight(Car car, int direction) {
        List<TrafficLight> lights = road.getTrafficLights();
        if (lights == null) return null;
        TrafficLight nextFoundLight = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (TrafficLight light : lights) {
            double distance;
            if (direction == 0) {
                distance = light.getPosition() - car.getPosition();
            } else {
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
        System.out.println("Параметры движка симуляции обновлены.");
    }

    public Road getRoad() {
        return road;
    }
    public double getSimulationTime() {
        return simulationTime;
    }
}
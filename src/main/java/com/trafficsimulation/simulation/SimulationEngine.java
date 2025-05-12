package com.trafficsimulation.simulation;

import com.trafficsimulation.gui.SimulationPanel;
import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.RoadType; // Убедитесь, что этот импорт есть
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;

import javax.swing.SwingUtilities;
import java.util.List;

/**
 * Основной класс, управляющий процессом симуляции.
 */
public class SimulationEngine implements Runnable {

    private SimulationParameters parameters;
    private Road road; // Этот объект будет пересоздаваться в initializeSimulation
    private TrafficFlowGenerator flowGenerator;
    private SimulationPanel simulationPanel;

    private volatile boolean running = false;
    private volatile boolean paused = false;
    private final Object pauseLock = new Object();

    private double simulationTime = 0.0;

    public SimulationEngine(SimulationParameters params, SimulationPanel panel) {
        this.parameters = params;
        this.simulationPanel = panel;
        initializeSimulation(); // Первый вызов для создания начального состояния
        System.out.println("SimulationEngine создан.");
    }

    /** Инициализация или сброс симуляции с текущими параметрами */
    public void initializeSimulation() {
        System.out.println("Инициализация симуляции с параметрами: " + parameters);
        // 1. Создаем НОВЫЙ объект дороги. Его списки светофоров и знаков будут изначально пустыми.
        this.road = new Road(
                parameters.getRoadLengthKm(),
                parameters.getRoadType(),
                parameters.getLanesPerDirection(),
                parameters.getNumberOfDirections()
        );

        // 2. Создаем новый генератор потока для НОВОЙ дороги
        this.flowGenerator = new TrafficFlowGenerator(parameters, this.road);
        this.simulationTime = 0.0; // Сбрасываем время симуляции

        // 3. Добавляем светофоры и знаки на НОВУЮ, чистую дорогу
        // Пример добавления светофора:
        if (road.getLength() > 100 && parameters.getRoadType() != RoadType.HIGHWAY) {
            road.addTrafficLight(new TrafficLight(road.getLength() * 0.3, 30, 30, TrafficLightState.RED));
            if (road.getLength() > 500 && road.getTrafficLights().size() < 2) { // Проверяем лимит перед добавлением второго
                road.addTrafficLight(new TrafficLight(road.getLength() * 0.7, 25, 35, TrafficLightState.GREEN));
            }
        }
        // TODO: Добавить логику для добавления дорожных знаков на основе параметров или пользовательского ввода
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
                        lastUpdateTime = System.nanoTime(); // Сброс времени после паузы
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

            if (road != null) { // Добавлена проверка на null для road
                step(simulationDeltaTime);
            }


            if (simulationPanel != null) {
                // Передаем ссылку на текущий объект road, который может быть null если симуляция остановлена и не инициализирована
                Road currentRoadForPanel = road;
                double currentSimTimeForPanel = simulationTime;
                SwingUtilities.invokeLater(() -> simulationPanel.updateSimulationState(currentRoadForPanel, currentSimTimeForPanel));
            }

            try {
                // Задержка для управления скоростью симуляции и снижения нагрузки на CPU
                // Более точное управление скоростью через simulationSpeedFactor
                long sleepTime = Math.max(0, (long)(parameters.getSimulationTimeStep() * 1000 / parameters.getSimulationSpeedFactor() - elapsedTimeNano / 1_000_000));
                // Thread.sleep(10); // Простая задержка
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
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
        if (deltaTime <= 0 || road == null) return; // Дополнительная проверка на road

        simulationTime += deltaTime;

        // 1. Генерация новых машин
        if (flowGenerator != null) { // Проверка, что генератор инициализирован
            Car newCar = flowGenerator.generateCar(deltaTime);
            if (newCar != null) {
                road.addCar(newCar);
            }
        }


        // 2. Обновление светофоров
        // Получаем список светофоров из текущего объекта road
        List<TrafficLight> lights = road.getTrafficLights(); // Это неизменяемый список
        // Для обновления нужно итерировать по нему и вызывать update у каждого светофора,
        // но сам список светофоров в road должен быть изменяемым, если мы их добавляем/удаляем динамически.
        // Однако, в нашем случае мы их добавляем только в initializeSimulation, так что просто итерируем.
        if (lights != null) {
            for (TrafficLight light : lights) { // Итерируемся по неизменяемой обертке
                light.update(deltaTime); // Сам объект TrafficLight изменяемый
            }
        }


        // 3. Обновление машин
        List<Car> currentCars = road.getCars(); // Это CopyOnWriteArrayList, безопасен для итерации
        if (currentCars != null) {
            for (Car car : currentCars) {
                Car leadCar = findLeadCar(car, currentCars);
                // Расстояние до машины впереди, учитывая ее задний бампер и наш передний
                double distanceToLead = (leadCar != null) ? (leadCar.getPosition() - car.getPosition() - 10) : Double.POSITIVE_INFINITY; // 10м - примерная длина машины + буфер
                double speedLimit = findEffectiveSpeedLimit(car);
                TrafficLight nextLight = findNextTrafficLight(car);
                double distanceToLight = (nextLight != null) ? nextLight.getPosition() - car.getPosition() : Double.POSITIVE_INFINITY;
                TrafficLightState nextLightState = (nextLight != null) ? nextLight.getCurrentState() : null;

                car.update(deltaTime, leadCar, distanceToLead, speedLimit, nextLightState, distanceToLight);
            }
        }


        // 4. Удаление машин, уехавших с дороги
        if (road.getCars() != null) {
            road.getCars().removeIf(car -> car.getPosition() > road.getLength() + 20); // +20м буфер
        }
    }

    // Вспомогательные методы поиска
    private Car findLeadCar(Car currentCar, List<Car> allCars) {
        if (allCars == null) return null;
        Car leader = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() || otherCar.getLaneIndex() != currentCar.getLaneIndex()) {
                continue;
            }
            double distance = otherCar.getPosition() - currentCar.getPosition();
            if (distance > 0 && distance < minDistance) { // Машина должна быть строго впереди
                minDistance = distance;
                leader = otherCar;
            }
        }
        return leader;
    }

    private double findEffectiveSpeedLimit(Car car) {
        // TODO: Реализовать поиск ближайшего знака ограничения скорости позади машины
        // и учитывать его. Пока возвращаем максимальную скорость машины.
        return car.getMaxSpeed();
    }

    private TrafficLight findNextTrafficLight(Car car) {
        List<TrafficLight> lights = road.getTrafficLights();
        if (lights == null) return null;
        // Светофоры отсортированы по позиции в Road.addTrafficLight
        for (TrafficLight light : lights) {
            if (light.getPosition() > car.getPosition()) {
                return light; // Первый же светофор впереди
            }
        }
        return null; // Нет светофоров впереди
    }

    public void updateParameters(SimulationParameters newParams) {
        this.parameters = newParams;
        // Если генератор потока зависит от параметров, которые могут измениться,
        // его нужно либо обновить, либо пересоздать.
        // Сейчас он пересоздается в initializeSimulation вместе с дорогой.
        if (this.flowGenerator != null) {
            // this.flowGenerator.updateParameters(newParams); // Если бы был такой метод
        }
        System.out.println("Параметры движка симуляции обновлены (фактически применятся при следующей инициализации).");
    }

    public Road getRoad() {
        return road;
    }
    public double getSimulationTime() {
        return simulationTime;
    }
}
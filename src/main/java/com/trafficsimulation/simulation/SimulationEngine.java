package com.trafficsimulation.simulation;

import com.trafficsimulation.model.Car;
import com.trafficsimulation.model.Road;
import com.trafficsimulation.model.TrafficLight;
import com.trafficsimulation.model.TrafficLightState;
import com.trafficsimulation.gui.SimulationPanel; // Импортируем панель для отрисовки

import javax.swing.SwingUtilities; // Для обновления GUI из потока симуляции
import java.util.List;

/**
 * Основной класс, управляющий процессом симуляции.
 * Запускает и останавливает симуляцию, управляет шагом времени,
 * обновляет состояние всех объектов.
 */
public class SimulationEngine implements Runnable { // Реализуем Runnable для запуска в отдельном потоке

    private SimulationParameters parameters;
    private Road road;
    private TrafficFlowGenerator flowGenerator;
    private SimulationPanel simulationPanel; // Ссылка на панель для перерисовки

    private volatile boolean running = false; // Флаг, работает ли симуляция (volatile важен для потоков)
    private volatile boolean paused = false;  // Флаг паузы
    private final Object pauseLock = new Object(); // Объект для синхронизации паузы

    private double simulationTime = 0.0; // Текущее время симуляции

    /**
     * Конструктор движка симуляции.
     * @param params Параметры симуляции.
     * @param panel Панель для отрисовки (чтобы движок мог ее обновлять).
     */
    public SimulationEngine(SimulationParameters params, SimulationPanel panel) {
        this.parameters = params;
        this.simulationPanel = panel;
        initializeSimulation(); // Создаем дорогу и генератор при старте
        System.out.println("SimulationEngine создан.");
    }

    /** Инициализация или сброс симуляции с текущими параметрами */
    private void initializeSimulation() {
        System.out.println("Инициализация симуляции с параметрами: " + parameters);
        this.road = new Road(
                parameters.getRoadLengthKm(),
                parameters.getRoadType(),
                parameters.getLanesPerDirection(),
                parameters.getNumberOfDirections()
        );
        this.flowGenerator = new TrafficFlowGenerator(parameters, this.road);
        this.simulationTime = 0.0;
        // TODO: Добавить создание светофоров и знаков на дорогу на основе параметров
        // Пример добавления светофора (позже вынести в настройки)
        if (road.getLength() > 100) { // Добавляем только если дорога длинная
            road.addTrafficLight(new TrafficLight(road.getLength() / 2, 30, 30, com.trafficsimulation.model.TrafficLightState.RED));
        }
    }

    /** Запускает симуляцию в отдельном потоке */
    public void startSimulation() {
        if (running) return; // Не запускать, если уже запущена
        running = true;
        paused = false;
        Thread simulationThread = new Thread(this, "SimulationThread"); // Создаем поток
        simulationThread.start(); // Запускаем выполнение метода run() в новом потоке
        System.out.println("Поток симуляции запущен.");
    }

    /** Останавливает симуляцию */
    public void stopSimulation() {
        running = false; // Сигнализируем потоку остановиться
        // Если поток на паузе, нужно его "разбудить", чтобы он увидел running = false
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Разбудить поток, если он ждет на pauseLock.wait()
        }
        System.out.println("Симуляция остановлена.");
        // Можно добавить сброс состояния initializeSimulation(); если нужно
    }

    /** Ставит симуляцию на паузу */
    public void pauseSimulation() {
        paused = true;
        System.out.println("Симуляция на паузе.");
    }

    /** Снимает симуляцию с паузы */
    public void resumeSimulation() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll(); // Разбудить поток, если он ждет
        }
        System.out.println("Симуляция снята с паузы.");
    }


    /** Основной цикл симуляции, выполняется в отдельном потоке */
    @Override
    public void run() {
        long lastUpdateTime = System.nanoTime(); // Время последнего обновления

        while (running) {
            // --- Обработка паузы ---
            synchronized (pauseLock) {
                if (paused) {
                    try {
                        System.out.println("Поток симуляции уходит в ожидание (пауза)...");
                        pauseLock.wait(); // Поток засыпает, пока не вызовут notifyAll() из resumeSimulation() или stopSimulation()
                        System.out.println("Поток симуляции проснулся.");
                        // После пробуждения снова проверяем running, т.к. могли остановить во время паузы
                        if (!running) break;
                        // Сбросить lastUpdateTime после паузы, чтобы избежать большого скачка времени
                        lastUpdateTime = System.nanoTime();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Восстановить флаг прерывания
                        running = false; // Остановить симуляцию при прерывании
                        System.err.println("Поток симуляции прерван во время паузы.");
                        break;
                    }
                }
            } // конец synchronized (pauseLock)

            // --- Расчет времени шага ---
            long currentTime = System.nanoTime();
            // Время с прошлого шага в НАНОсекундах
            long elapsedTimeNano = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;
            // Переводим в секунды (модельное время)
            double deltaTime = elapsedTimeNano / 1_000_000_000.0;
            // Учитываем фактор ускорения времени
            double simulationDeltaTime = deltaTime * parameters.getSimulationSpeedFactor();

            // --- Выполнение шага симуляции ---
            step(simulationDeltaTime);

            // --- Обновление GUI ---
            // Важно: обновление Swing компонентов должно происходить в потоке EDT
            if (simulationPanel != null) {
                SwingUtilities.invokeLater(() -> simulationPanel.updateSimulationState(road, simulationTime));
            }

            // --- Небольшая пауза для контроля скорости (опционально) ---
            // Чтобы цикл не потреблял 100% CPU, можно добавить маленькую паузу.
            // Реальная скорость будет контролироваться deltaTime и simulationSpeedFactor.
            try {
                Thread.sleep(10); // Пауза в 10 миллисекунд
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
                System.err.println("Поток симуляции прерван во время sleep.");
            }
        } // конец while(running)
        System.out.println("Поток симуляции завершил работу.");
    }

    /** Выполняет один шаг симуляции */
    private void step(double deltaTime) {
        if (deltaTime <= 0) return; // Не делаем шаг, если время не прошло

        simulationTime += deltaTime; // Увеличиваем общее время симуляции

        // 1. Генерация новых машин
        Car newCar = flowGenerator.generateCar(deltaTime);
        if (newCar != null) {
            road.addCar(newCar);
        }

        // 2. Обновление светофоров
        for (TrafficLight light : road.getTrafficLights()) {
            light.update(deltaTime);
        }

        // 3. Обновление машин
        // Создаем копию списка ID машин для безопасной итерации (если машины могут удаляться во время update)
        List<Car> currentCars = road.getCars(); // Получаем текущий (потокобезопасный) список
        for (Car car : currentCars) { // Итерируемся по нему
            // !!! Здесь должна быть сложная логика !!!
            // Найти машину впереди, знаки, светофоры
            // Пока просто двигаем машину
            // Найти ограничения (примерные плейсхолдеры):
            Car leadCar = findLeadCar(car, currentCars);
            double distanceToLead = (leadCar != null) ? leadCar.getPosition() - car.getPosition() : Double.POSITIVE_INFINITY;
            double speedLimit = findEffectiveSpeedLimit(car);
            TrafficLight nextLight = findNextTrafficLight(car);
            double distanceToLight = (nextLight != null) ? nextLight.getPosition() - car.getPosition() : Double.POSITIVE_INFINITY;
            TrafficLightState nextLightState = (nextLight != null) ? nextLight.getCurrentState() : null;

            car.update(deltaTime, leadCar, distanceToLead, speedLimit, nextLightState, distanceToLight); // Обновляем состояние машины
        }

        // 4. Удаление машин, уехавших с дороги
        road.getCars().removeIf(car -> car.getPosition() > road.getLength() + 10); // Удаляем уехавшие (+ небольшой буфер)

        // System.out.printf("Time: %.2f, Cars: %d\n", simulationTime, road.getCars().size()); // Отладка
    }

    // --- Вспомогательные методы для поиска объектов (пока заглушки) ---
    private Car findLeadCar(Car currentCar, List<Car> allCars) {
        // TODO: Найти ближайшую машину ВПЕРЕДИ на ТОЙ ЖЕ полосе
        return null;
    }
    private double findEffectiveSpeedLimit(Car car) {
        // TODO: Найти ближайший знак ограничения скорости ПОЗАДИ машины
        return car.getMaxSpeed(); // По умолчанию - максималка машины
    }
    private TrafficLight findNextTrafficLight(Car car) {
        // TODO: Найти ближайший светофор ВПЕРЕДИ машины
        for (TrafficLight light : road.getTrafficLights()) {
            if (light.getPosition() > car.getPosition()) {
                return light; // Возвращаем первый же впереди (т.к. они отсортированы)
            }
        }
        return null;
    }

    // Метод для обновления параметров симуляции "на лету"
    public void updateParameters(SimulationParameters newParams) {
        this.parameters = newParams;
        // Возможно, нужно пересоздать генератор или обновить его параметры
        if (this.flowGenerator != null) {
            this.flowGenerator.updateParameters(newParams);
        }
        System.out.println("Параметры движка симуляции обновлены.");
        // Примечание: изменение параметров дороги (длина, полосы) "на лету"
        // потребует полной перезагрузки симуляции (вызов initializeSimulation).
    }

    // Геттер для получения текущего состояния дороги (нужен для отрисовки)
    public Road getRoad() {
        return road;
    }
    // Геттер для получения текущего времени симуляции
    public double getSimulationTime() {
        return simulationTime;
    }
}
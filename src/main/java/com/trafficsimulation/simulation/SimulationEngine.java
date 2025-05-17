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
    private TrafficLight tunnelLightDir0; // Светофор для въезда в тоннель со стороны направления 0
    private TrafficLight tunnelLightDir1; // Светофор для въезда в тоннель со стороны направления 1

    private static final boolean ENGINE_DEBUG_LOGGING = true;
    private static final double LANE_CHANGE_CHECK_INTERVAL = 0.6;
    private double timeSinceLastLaneChangeCheck = 0.0;


    public SimulationEngine(SimulationParameters params, SimulationPanel panel) {
        this.parameters = params;
        this.simulationPanel = panel;
        initializeSimulation();
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Конструктор выполнен, initializeSimulation() вызвана.");
    }

    public void initializeSimulation() {
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Начало initializeSimulation(). Параметры: " + parameters.getRoadType());
        boolean isTunnelActive = (parameters.getRoadType() == RoadType.TUNNEL);

        if (isTunnelActive) {
            if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Тип дороги - Тоннель. Установка параметров для тоннеля.");
            // В тоннеле всегда 2 направления и 1 полоса на направление (для нашей модели)
            parameters.setNumberOfDirections(2);
            parameters.setLanesPerDirection(1);
        }

        this.road = new Road(
                parameters.getRoadLengthKm(),
                parameters.getRoadType(),
                parameters.getLanesPerDirection(), // Это уже должно быть 1 для тоннеля
                parameters.getNumberOfDirections() // Это уже должно быть 2 для тоннеля
        );
        // Важно: TrafficFlowGenerator должен создаваться ПОСЛЕ Road, т.к. он может на него ссылаться
        this.flowGenerator = new TrafficFlowGenerator(parameters, this.road);
        this.simulationTime = 0.0;
        this.timeSinceLastLaneChangeCheck = 0.0;
        this.tunnelLightDir0 = null;
        this.tunnelLightDir1 = null;

        // Очистка старых светофоров, если они были (особенно при смене типа дороги на тоннель)
        if (this.road != null && this.road.getTrafficLights() != null && !this.road.getTrafficLights().isEmpty()) {
            this.road.clearTrafficLights();
        }
        // Очистка знаков, если это тоннель (в тоннеле знаки не ставятся пользователем в нашей модели)
        if (this.road != null && isTunnelActive && this.road.getRoadSigns() != null) {
            this.road.clearRoadSigns();
        }


        if (isTunnelActive) {
            if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Создание светофоров для тоннеля.");
            double roadModelLength = this.road.getLength();
            // Позиции светофоров на въездах в "активную" зону тоннеля
            double leftLightPosition = roadModelLength * 0.25;  // Для потока dir 0 (слева направо)
            double rightLightPosition = roadModelLength * 0.75; // Для потока dir 1 (справа налево)

            // Светофор для направления 0 (управляет въездом слева)
            tunnelLightDir0 = new TrafficLight(leftLightPosition, parameters.getTunnelDefaultRedDuration(), parameters.getTunnelDefaultGreenDuration(), TrafficLightState.GREEN, 0);
            tunnelLightDir0.setExternallyControlled(true);
            this.road.addTrafficLight(tunnelLightDir0);

            // Светофор для направления 1 (управляет въездом справа)
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
            // Обновляем панель с новой дорогой
            simulationPanel.updateSimulationState(this.road, this.simulationTime);
        }
    }

    private void updateTunnelLogic(double deltaTime) {
        if (road.getType() != RoadType.TUNNEL || tunnelLightDir0 == null || tunnelLightDir1 == null) return;

        tunnelPhaseTimer -= deltaTime;

        // Обновляем внутреннее состояние самих объектов TrafficLight (таймеры и т.д.)
        // Это нужно, даже если они externallyControlled, чтобы getRemainingTime() работал.
        tunnelLightDir0.update(deltaTime);
        tunnelLightDir1.update(deltaTime);


        if (tunnelPhaseTimer <= 0) {
            if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: Таймер фазы истек ("+String.format("%.2f", tunnelPhaseTimer)+"). Текущее состояние: " + tunnelControlState);
            switch (tunnelControlState) {
                case DIR0_GREEN:
                    tunnelControlState = TunnelControlState.DIR0_CLEARING;
                    tunnelPhaseTimer = parameters.getTunnelClearanceTime();
                    tunnelLightDir0.setCurrentState(TrafficLightState.RED, true); // Закрываем въезд для Dir0
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: -> DIR0_CLEARING. Таймер: " + String.format("%.2f", tunnelPhaseTimer) + "с. Св0->Красный.");
                    break;
                case DIR0_CLEARING:
                    boolean carsInDir0ActiveZone = areCarsInTunnelActiveZone(0);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: Проверка DIR0_CLEARING. Машины напр.0 в акт.зоне: " + carsInDir0ActiveZone + ". Осталось времени очистки: " + String.format("%.2f", tunnelPhaseTimer));
                    if (carsInDir0ActiveZone && tunnelPhaseTimer > -parameters.getTunnelClearanceTime() * 0.5) { // Даем еще немного времени, если кто-то застрял
                        tunnelPhaseTimer = Math.max(tunnelPhaseTimer, 0.2); // Не уходить в глубокий минус, если постоянно машины
                        break;
                    }
                    // Если машин нет ИЛИ время вышло с запасом
                    tunnelControlState = TunnelControlState.DIR1_GREEN;
                    tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
                    tunnelLightDir1.setCurrentState(TrafficLightState.GREEN, true); // Открываем въезд для Dir1
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: -> DIR1_GREEN. Таймер: " + String.format("%.2f", tunnelPhaseTimer) + "с. Св1->Зеленый.");
                    break;
                case DIR1_GREEN:
                    tunnelControlState = TunnelControlState.DIR1_CLEARING;
                    tunnelPhaseTimer = parameters.getTunnelClearanceTime();
                    tunnelLightDir1.setCurrentState(TrafficLightState.RED, true); // Закрываем въезд для Dir1
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: -> DIR1_CLEARING. Таймер: " + String.format("%.2f", tunnelPhaseTimer) + "с. Св1->Красный.");
                    break;
                case DIR1_CLEARING:
                    boolean carsInDir1ActiveZone = areCarsInTunnelActiveZone(1);
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: Проверка DIR1_CLEARING. Машины напр.1 в акт.зоне: " + carsInDir1ActiveZone + ". Осталось времени очистки: " + String.format("%.2f", tunnelPhaseTimer));
                    if (carsInDir1ActiveZone && tunnelPhaseTimer > -parameters.getTunnelClearanceTime() * 0.5) {
                        tunnelPhaseTimer = Math.max(tunnelPhaseTimer, 0.2);
                        break;
                    }
                    tunnelControlState = TunnelControlState.DIR0_GREEN;
                    tunnelPhaseTimer = parameters.getTunnelDefaultGreenDuration();
                    tunnelLightDir0.setCurrentState(TrafficLightState.GREEN, true); // Открываем въезд для Dir0
                    if (ENGINE_DEBUG_LOGGING) System.out.println("TunnelLogic: -> DIR0_GREEN. Таймер: " + String.format("%.2f", tunnelPhaseTimer) + "с. Св0->Зеленый.");
                    break;
            }
        }
    }

    // Проверяет наличие машин в "активной" зоне тоннеля (между светофорами)
    private boolean areCarsInTunnelActiveZone(int direction) {
        if (road == null || road.getCars() == null || road.getCars().isEmpty()) return false;

        double entryLightPos = (direction == 0) ? road.getLength() * 0.25 : road.getLength() * 0.75;
        double exitLightPos = (direction == 0) ? road.getLength() * 0.75 : road.getLength() * 0.25;
        // Небольшой буфер, чтобы машина считалась "въехавшей" чуть за светофор
        // и "выехавшей" чуть за другой светофор.
        double entryBuffer = Car.APPROX_CAR_LENGTH * 0.5;
        double exitBuffer = Car.APPROX_CAR_LENGTH * 0.5;


        for (Car car : road.getCars()) {
            if (car.getDirection() == direction) {
                if (direction == 0) { // Едет слева направо
                    // Машина считается в активной зоне, если она ПРОЕХАЛА въездной светофор
                    // И НЕ ПРОЕХАЛА выездной светофор
                    if (car.getPosition() > entryLightPos + entryBuffer && car.getPosition() < exitLightPos - exitBuffer) {
                        return true;
                    }
                } else { // Едет справа налево
                    // Машина считается в активной зоне, если она ПРОЕХАЛА въездной светофор (позиция МЕНЬШЕ)
                    // И НЕ ПРОЕХАЛА выездной светофор (позиция БОЛЬШЕ)
                    if (car.getPosition() < entryLightPos - entryBuffer && car.getPosition() > exitLightPos + exitBuffer) {
                        return true;
                    }
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
        final double TARGET_FPS = 30.0; // Целевой FPS
        final double OPTIMAL_TIME_PER_FRAME_NANO = 1_000_000_000.0 / TARGET_FPS; // Оптимальное время на кадр в наносекундах

        while (running) {
            synchronized (pauseLock) {
                if (paused) {
                    try {
                        pauseLock.wait();
                        if (!running) break; // Если остановили во время паузы
                        lastUpdateTime = System.nanoTime(); // Сбросить время после пробуждения
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running = false; // Прервать симуляцию
                        break;
                    }
                }
            }

            long loopStartTime = System.nanoTime();
            double deltaTimeFromLastFrame = (loopStartTime - lastUpdateTime) / 1_000_000_000.0; // Дельта в секундах
            lastUpdateTime = loopStartTime;

            // Ограничение deltaTime, чтобы избежать "прыжков" после долгой паузы или лагов
            deltaTimeFromLastFrame = Math.min(deltaTimeFromLastFrame, 0.1); // Максимум 0.1с реального времени за шаг

            double simulationDeltaTime = deltaTimeFromLastFrame * parameters.getSimulationSpeedFactor();

            if (road != null) {
                step(simulationDeltaTime);
            }

            if (simulationPanel != null) {
                // Передаем копии или неизменяемые состояния, если возможно, для потокобезопасности
                Road currentRoadForPanel = road; // Road сам по себе не потокобезопасен для изменений во время отрисовки
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
                    Thread.currentThread().interrupt();
                    running = false;
                }
            } else {
                // Если обработка кадра заняла больше времени, чем OPTIMAL_TIME_PER_FRAME_NANO,
                // то не спим, а уступаем управление другим потокам, чтобы не блокировать GUI.
                Thread.yield();
            }
        }
        if(ENGINE_DEBUG_LOGGING) System.out.println("Поток симуляции завершил работу.");
    }

    private void step(double deltaTime) {
        if (deltaTime <= 0 || road == null) return;

        simulationTime += deltaTime;
        timeSinceLastLaneChangeCheck += deltaTime;

        // Обновление логики тоннеля или обычных светофоров
        if (road.getType() == RoadType.TUNNEL) {
            updateTunnelLogic(deltaTime);
        } else {
            List<TrafficLight> lights = road.getTrafficLights();
            if (lights != null) {
                for (TrafficLight light : lights) {
                    if (!light.isExternallyControlled()) { // Обновляем только не-тоннельные
                        light.update(deltaTime);
                    }
                }
            }
        }

        // Генерация новых машин
        if (flowGenerator != null) {
            TunnelControlState currentTunnelStateForGenerator = (road.getType() == RoadType.TUNNEL) ? this.tunnelControlState : null;
            Car[] newCars = flowGenerator.generateCars(deltaTime, currentTunnelStateForGenerator);
            if (newCars != null) {
                for (Car newCar : newCars) {
                    if (newCar != null) road.addCar(newCar);
                }
            }
        }

        List<Car> currentCars = new ArrayList<>(road.getCars()); // Работаем с копией для избежания ConcurrentModificationException
        // Сортируем машины по направлению и затем по позиции для корректного поиска лидеров.
        // Для направления 0 (слева направо) - по возрастанию позиции.
        // Для направления 1 (справа налево) - по убыванию позиции.
        currentCars.sort((car1, car2) -> {
            if (car1.getDirection() != car2.getDirection()) {
                return Integer.compare(car1.getDirection(), car2.getDirection());
            }
            if (car1.getDirection() == 0) {
                return Double.compare(car1.getPosition(), car2.getPosition());
            } else { // direction == 1
                return Double.compare(car2.getPosition(), car1.getPosition()); // Обратный порядок для направления 1
            }
        });


        // Обновление состояния каждой машины
        for (Car car : currentCars) {
            // Находим лидера на текущей ЛОКАЛЬНОЙ полосе машины
            Car leadCar = findLeadCarOnLocalLane(car, car.getLaneIndex(), currentCars);
            double distanceToLead = (leadCar != null) ?
                    Math.max(0.01, Math.abs(leadCar.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH)
                    : Double.POSITIVE_INFINITY;

            double effectiveSpeedLimit = findEffectiveSpeedLimit(car);
            TrafficLight nextLight = findNextTrafficLight(car);
            double distanceToLight = Double.POSITIVE_INFINITY;
            TrafficLightState nextLightState = null;

            if (nextLight != null) {
                distanceToLight = Math.abs(nextLight.getPosition() - car.getPosition());
                // Проверка, не проехала ли машина уже светофор
                boolean carPassedLight = (car.getDirection() == 0 && car.getPosition() > nextLight.getPosition() + Car.APPROX_CAR_LENGTH * 0.3) ||
                        (car.getDirection() == 1 && car.getPosition() < nextLight.getPosition() - Car.APPROX_CAR_LENGTH * 0.3);
                if (!carPassedLight) {
                    nextLightState = nextLight.getCurrentState();
                } else {
                    distanceToLight = Double.POSITIVE_INFINITY; // Светофор позади, не влияем
                }
            }
            car.update(deltaTime, leadCar, distanceToLead, effectiveSpeedLimit, nextLightState, distanceToLight);
        }

        // Обработка смены полос
        if (timeSinceLastLaneChangeCheck >= LANE_CHANGE_CHECK_INTERVAL) {
            if (road.getType() != RoadType.TUNNEL && road.getLanesPerDirection() > 1) { // Смена полос только если есть куда и не в тоннеле
                processLaneChanges(currentCars); // Передаем уже отсортированный (по направлению, затем по позиции) список
            }
            timeSinceLastLaneChangeCheck = 0.0;
        }

        // Удаление машин, уехавших с дороги
        if (road.getCars() != null) {
            double removalBuffer = Car.APPROX_CAR_LENGTH * 3.0; // Буфер для удаления
            road.getCars().removeIf(car ->
                    (car.getDirection() == 0 && car.getPosition() > road.getLength() + removalBuffer) ||
                            (car.getDirection() == 1 && car.getPosition() < -removalBuffer)
            );
        }
    }

    private void processLaneChanges(List<Car> cars) {
        // Фильтруем машины, которые не в тоннеле и для которых есть смысл менять полосу
        List<Car> eligibleCars = cars.stream()
                .filter(c -> road.getType() != RoadType.TUNNEL && road.getLanesPerDirection() > 1)
                .collect(Collectors.toList());

        for (Car car : eligibleCars) {
            int currentLocalLane = car.getLaneIndex();
            double currentAcceleration = calculatePotentialAccelerationOnLocalLane(car, currentLocalLane, cars);

            boolean isRightmost = (currentLocalLane == 0); // Локальная 0 - крайняя правая
            boolean isLeftmost = (currentLocalLane == road.getLanesPerDirection() - 1); // Локальная N-1 - крайняя левая

            Double leftBenefit = null;
            int targetLocalLeftLane = -1;
            if (!isLeftmost) {
                targetLocalLeftLane = currentLocalLane + 1; // Левее -> локальный индекс увеличивается
                leftBenefit = calculatePotentialAccelerationOnLocalLane(car, targetLocalLeftLane, cars);
            }

            Double rightBenefit = null;
            int targetLocalRightLane = -1;
            if (!isRightmost) {
                targetLocalRightLane = currentLocalLane - 1; // Правее -> локальный индекс уменьшается
                rightBenefit = calculatePotentialAccelerationOnLocalLane(car, targetLocalRightLane, cars);
            }

            car.evaluateLaneChange(currentAcceleration, leftBenefit, rightBenefit, isRightmost, isLeftmost);

            int finalTargetLocalLane = -1;
            String changeLogDirection = "";

            if (car.wantsToChangeLaneLeft() && targetLocalLeftLane != -1) {
                finalTargetLocalLane = targetLocalLeftLane;
                changeLogDirection = " налево (на локальную " + targetLocalLeftLane + ")";
            } else if (car.wantsToChangeLaneRight() && targetLocalRightLane != -1) {
                finalTargetLocalLane = targetLocalRightLane;
                changeLogDirection = " направо (на локальную " + targetLocalRightLane + ")";
            }

            if (finalTargetLocalLane != -1) {
                if (isLaneChangeSafe(car, finalTargetLocalLane, cars)) {
                    if(ENGINE_DEBUG_LOGGING) System.out.printf("Car %d (Dir %d, locLane %d, spd %.1f km/h) CHANGING LANE%s. MyAccel: %.2f, LeftB: %.2f, RightB: %.2f%n",
                            car.getId(), car.getDirection(), currentLocalLane, car.getCurrentSpeed()*3.6,
                            changeLogDirection, currentAcceleration,
                            leftBenefit != null ? leftBenefit : -99.0,
                            rightBenefit != null ? rightBenefit : -99.0);
                    car.setLaneIndex(finalTargetLocalLane); // Устанавливаем новый ЛОКАЛЬНЫЙ индекс
                } else {
                    if(ENGINE_DEBUG_LOGGING && (car.wantsToChangeLaneLeft() || car.wantsToChangeLaneRight()))
                        System.out.printf("Car %d (Dir %d, locLane %d) wanted to change%s BUT NOT SAFE.%n", car.getId(), car.getDirection(), currentLocalLane, changeLogDirection);
                }
            }
        }
    }

    // Вычисляет потенциальное ускорение, если бы машина была на указанной ЛОКАЛЬНОЙ полосе
    private double calculatePotentialAccelerationOnLocalLane(Car car, int targetLocalLaneIndex, List<Car> allCars) {
        Car leadCarOnTargetLane = findLeadCarOnLocalLane(car, targetLocalLaneIndex, allCars);
        double distanceToLead = (leadCarOnTargetLane != null) ?
                Math.max(0.01, Math.abs(leadCarOnTargetLane.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH)
                : Double.POSITIVE_INFINITY;

        double desiredSpeed = car.getDesiredSpeed(); // Используем текущую желаемую скорость машины
        double currentSpeed = car.getCurrentSpeed();
        double accelParam = car.getAccelerationParam();
        double decelParam = car.getBaseDecelerationParam();

        double deltaV_hypothetical = (leadCarOnTargetLane != null) ? currentSpeed - leadCarOnTargetLane.getCurrentSpeed() : 0;
        double s_star = Car.MIN_GAP + Math.max(0, currentSpeed * Car.SAFE_TIME_HEADWAY +
                (currentSpeed * deltaV_hypothetical) /
                        (2 * Math.sqrt(accelParam * decelParam)));

        double freeRoadTerm = accelParam * (1 - Math.pow(currentSpeed / Math.max(0.1, desiredSpeed), Car.ACCELERATION_EXPONENT));
        double interactionTerm = 0.0;
        if (leadCarOnTargetLane != null && distanceToLead < 200) { // Взаимодействуем только с близкими
            if (distanceToLead > 0.1) { // Защита от деления на ноль, если машины "слиплись"
                interactionTerm = -accelParam * Math.pow(s_star / distanceToLead, 2);
            } else {
                interactionTerm = -accelParam * Math.pow(s_star / 0.1, 2); // Очень сильное торможение
            }
        }
        return freeRoadTerm + interactionTerm;
    }

    // Проверяет безопасность смены на указанную ЛОКАЛЬНУЮ полосу
    private boolean isLaneChangeSafe(Car car, int targetLocalLaneIndex, List<Car> allCars) {
        // 1. Проверка нового лидера на целевой полосе
        Car newLeader = findLeadCarOnLocalLane(car, targetLocalLaneIndex, allCars);
        if (newLeader != null) {
            double distanceToNewLeader = Math.abs(newLeader.getPosition() - car.getPosition()) - Car.APPROX_CAR_LENGTH;
            // Машина не должна "подрезать" нового лидера слишком сильно
            if (distanceToNewLeader < Car.MIN_GAP * 1.2) { // Немного больший зазор для безопасности "подрезания"
                if(ENGINE_DEBUG_LOGGING) System.out.println("LC unsafe for car " + car.getId() + ": too close to new leader " + newLeader.getId() + " on local lane " + targetLocalLaneIndex + " (dist: " + String.format("%.1f", distanceToNewLeader) + ")");
                return false;
            }
        }

        // 2. Проверка нового последователя на целевой полосе
        Car newFollower = findFollowerOnLocalLane(car, targetLocalLaneIndex, allCars);
        if (newFollower != null) {
            double distanceCarToNewFollower = Math.abs(car.getPosition() - newFollower.getPosition()) - Car.APPROX_CAR_LENGTH;
            // Машина не должна создавать опасную ситуацию для нового последователя
            if (distanceCarToNewFollower < Car.MIN_GAP * 0.8) { // Меньший зазор допустим, но не слишком
                if(ENGINE_DEBUG_LOGGING) System.out.println("LC unsafe for car " + car.getId() + ": too close to new follower " + newFollower.getId() + " on local lane " + targetLocalLaneIndex + " (dist: " + String.format("%.1f", distanceCarToNewFollower) +")");
                return false;
            }

            // Оцениваем, не заставит ли маневр нового последователя слишком резко тормозить (модель MOBIL)
            double followerSpeed = newFollower.getCurrentSpeed();
            double followerDesiredSpeed = newFollower.getDesiredSpeed();
            double followerAccel = newFollower.getAccelerationParam();
            double followerDecel = newFollower.getBaseDecelerationParam();

            // Потенциальное ускорение последователя ПОСЛЕ того, как наша машина окажется перед ним
            double deltaV_follower_hypothetical = followerSpeed - car.getCurrentSpeed(); // Скорость последователя минус скорость нашей машины
            double s_star_follower = Car.MIN_GAP + Math.max(0, followerSpeed * Car.SAFE_TIME_HEADWAY +
                    (followerSpeed * deltaV_follower_hypothetical) /
                            (2 * Math.sqrt(followerAccel * followerDecel)));

            double freeRoadFollower = followerAccel * (1 - Math.pow(followerSpeed / Math.max(0.1, followerDesiredSpeed), Car.ACCELERATION_EXPONENT));
            double interactionFollower;
            if (distanceCarToNewFollower > 0.1) {
                interactionFollower = -followerAccel * Math.pow(s_star_follower / distanceCarToNewFollower, 2);
            } else {
                interactionFollower = -followerAccel * Math.pow(s_star_follower / 0.1, 2);
            }
            double potentialAccelerationFollower = freeRoadFollower + interactionFollower;

            if (potentialAccelerationFollower < -Car.SAFE_DECELERATION_FOR_OTHERS) {
                if(ENGINE_DEBUG_LOGGING) System.out.println("LC unsafe for car " + car.getId() + ". New follower " + newFollower.getId() + " on local lane " + targetLocalLaneIndex + " would decelerate too hard: " + String.format("%.2f", potentialAccelerationFollower) + " (limit: " + -Car.SAFE_DECELERATION_FOR_OTHERS + ")");
                return false;
            }
        }
        return true;
    }


    // Находит ведущую машину для currentCar на указанной ЛОКАЛЬНОЙ полосе targetLocalLaneIndex
    private Car findLeadCarOnLocalLane(Car currentCar, int targetLocalLaneIndex, List<Car> allCars) {
        Car leader = null;
        double minPositiveDistance = Double.POSITIVE_INFINITY;

        for (Car otherCar : allCars) {
            // Рассматриваем только машины того же направления и на той же ЛОКАЛЬНОЙ полосе
            if (otherCar.getId() == currentCar.getId() ||
                    otherCar.getDirection() != currentCar.getDirection() ||
                    otherCar.getLaneIndex() != targetLocalLaneIndex) {
                continue;
            }

            double distance;
            if (currentCar.getDirection() == 0) { // Едем слева направо
                distance = otherCar.getPosition() - currentCar.getPosition();
            } else { // Едем справа налево
                distance = currentCar.getPosition() - otherCar.getPosition();
            }

            if (distance > 0.01 && distance < minPositiveDistance) { // otherCar находится ВПЕРЕДИ currentCar
                minPositiveDistance = distance;
                leader = otherCar;
            }
        }
        return leader;
    }

    // Находит следующую машину (последователя) для currentCar на указанной ЛОКАЛЬНОЙ полосе targetLocalLaneIndex
    private Car findFollowerOnLocalLane(Car currentCar, int targetLocalLaneIndex, List<Car> allCars) {
        Car follower = null;
        double minPositiveDistanceBehind = Double.POSITIVE_INFINITY;

        for (Car otherCar : allCars) {
            if (otherCar.getId() == currentCar.getId() ||
                    otherCar.getDirection() != currentCar.getDirection() ||
                    otherCar.getLaneIndex() != targetLocalLaneIndex) {
                continue;
            }

            double distance;
            if (currentCar.getDirection() == 0) { // Едем слева направо
                distance = currentCar.getPosition() - otherCar.getPosition(); // currentCar ВПЕРЕДИ otherCar
            } else { // Едем справа налево
                distance = otherCar.getPosition() - currentCar.getPosition(); // currentCar ВПЕРЕДИ otherCar (по убыванию позиции)
            }

            if (distance > 0.01 && distance < minPositiveDistanceBehind) { // otherCar находится СЗАДИ currentCar
                minPositiveDistanceBehind = distance;
                follower = otherCar;
            }
        }
        return follower;
    }


    private double findEffectiveSpeedLimit(Car car) {
        RoadType currentRoadType = road.getType();
        double roadTypeMaxSpeedMs = currentRoadType.getMaxSpeedLimitMs();
        double roadTypeMinSpeedMs = currentRoadType.getMinSpeedLimitMs(); // Минимальная скорость для типа дороги
        double carPersonalMaxSpeedMs = car.getMaxSpeed(); // Персональный максимум машины

        // Начальный эффективный лимит - минимум из максимума дороги и персонального максимума машины
        double effectiveLimitMs = Math.min(roadTypeMaxSpeedMs, carPersonalMaxSpeedMs);

        List<RoadSign> signs = road.getRoadSigns();
        RoadSign activeSign = null;

        if (signs != null && !signs.isEmpty()) {
            // Сортировка знаков по позиции (должна быть сделана при добавлении в Road)
            // signs.sort(Comparator.comparingDouble(RoadSign::getPosition)); // Уже не нужна здесь, если Road сортирует

            if (car.getDirection() == 0) { // Движение слева направо
                for (RoadSign sign : signs) {
                    // Знак действует на это направление (0) или на оба (-1)
                    if (sign.getTargetDirection() == 0 || sign.getTargetDirection() == -1) {
                        if (sign.getPosition() <= car.getPosition() + Car.APPROX_CAR_LENGTH * 0.5) { // Машина проехала или на уровне знака
                            activeSign = sign; // Последний пройденный знак этого направления
                        } else {
                            break; // Знаки дальше по ходу движения, нет смысла смотреть
                        }
                    }
                }
            } else { // Движение справа налево (car.getDirection() == 1)
                // Идем по знакам в обратном порядке, т.к. позиция машины уменьшается
                for (int i = signs.size() - 1; i >= 0; i--) {
                    RoadSign sign = signs.get(i);
                    // Знак действует на это направление (1) или на оба (-1)
                    if (sign.getTargetDirection() == 1 || sign.getTargetDirection() == -1) {
                        if (sign.getPosition() >= car.getPosition() - Car.APPROX_CAR_LENGTH * 0.5) { // Машина проехала или на уровне знака
                            activeSign = sign; // Последний пройденный знак этого направления
                        } else {
                            break; // Знаки дальше по ходу движения (с бОльшими номерами, но меньшими позициями), нет смысла смотреть
                        }
                    }
                }
            }
        }

        if (activeSign != null) {
            double limitFromSignMs = activeSign.getSpeedLimitValue(); // Предполагаем, что это уже в м/с
            if (limitFromSignMs >= 0) { // Если знак валидный
                effectiveLimitMs = Math.min(effectiveLimitMs, limitFromSignMs);
            }
        }

        // Финальный лимит не должен быть ниже минимума для дороги и не ниже 0
        return Math.max(roadTypeMinSpeedMs, Math.max(0, effectiveLimitMs));
    }


    private TrafficLight findNextTrafficLight(Car car) {
        List<TrafficLight> lights = road.getTrafficLights();
        if (lights == null || lights.isEmpty()) return null;

        TrafficLight nextFoundLight = null;
        double minPositiveDistance = Double.POSITIVE_INFINITY;

        for (TrafficLight light : lights) {
            // Светофор должен действовать на направление машины или на оба (-1 для тоннеля, но там своя логика)
            if (light.getTargetDirection() != car.getDirection() && light.getTargetDirection() != -1 ) {
                continue;
            }
            // Для тоннеля используем только системные светофоры
            if (road.getType() == RoadType.TUNNEL && !light.isExternallyControlled()){
                continue;
            }
            // Для не-тоннеля пропускаем системные светофоры (если вдруг такие появятся)
            if (road.getType() != RoadType.TUNNEL && light.isExternallyControlled()){
                continue;
            }


            double distanceToLight;
            if (car.getDirection() == 0) { // слева направо
                distanceToLight = light.getPosition() - car.getPosition();
            } else { // справа налево
                distanceToLight = car.getPosition() - light.getPosition();
            }

            // Рассматриваем только светофоры ВПЕРЕДИ машины (или очень близко)
            if (distanceToLight > -Car.APPROX_CAR_LENGTH * 0.5 && distanceToLight < minPositiveDistance) {
                minPositiveDistance = distanceToLight;
                nextFoundLight = light;
            }
        }
        return nextFoundLight;
    }


    public void updateParameters(SimulationParameters newParams) {
        this.parameters = newParams; // Обновляем ссылку на параметры
        // Переинициализация симуляции полностью (создание новой дороги, генератора и т.д.)
        this.initializeSimulation();
        // Генератор потока также должен быть уведомлен об изменении параметров,
        // особенно если они влияют на таймеры или типы генерации.
        // (TrafficFlowGenerator.updateParameters уже вызывается из initializeSimulation косвенно через создание нового генератора)
        if (ENGINE_DEBUG_LOGGING) System.out.println("SimulationEngine: Параметры обновлены и симуляция переинициализирована.");
    }

    public Road getRoad() { return road; }
    public double getSimulationTime() { return simulationTime; }
    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    // private int getGuiPanelWidth() { if (simulationPanel != null) { return simulationPanel.getWidth(); } return 800; }
}
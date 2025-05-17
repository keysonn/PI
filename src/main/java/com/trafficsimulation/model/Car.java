package com.trafficsimulation.model;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class Car {

    private static final AtomicLong idCounter = new AtomicLong(0);

    public static final double SAFE_TIME_HEADWAY = 2.0; // s
    public static final double MIN_GAP = 2.5; // m, минимальное расстояние до машины впереди или до светофора
    public static final int ACCELERATION_EXPONENT = 4; // стандартное значение для IDM
    public static final double APPROX_CAR_LENGTH = 5.0; // m, длина машины, используется для расчета бампер-в-бампер

    // Параметры для логики смены полос (можно вынести в SimulationParameters, если нужно настраивать)
    public static final double POLITENESS_FACTOR = 0.3; // Коэффициент вежливости (0..1)
    public static final double ACCELERATION_THRESHOLD_GAIN = 0.15; // Порог выигрыша в ускорении для смены полосы (м/с^2)
    public static final double LANE_CHANGE_RIGHT_BIAS = 0.1; // "Бонус" к ускорению для правой полосы (стимул вернуться) (м/с^2)
    public static final double SAFE_DECELERATION_FOR_OTHERS = 2.5; // Максимальное безопасное замедление для машины сзади на целевой полосе (м/с^2)
    private static final double LANE_CHANGE_COOLDOWN = 2.5; // Минимальное время между сменами полос (с)


    private final long id;
    private double position;        // метры, от начала дороги для направления 0, от конца для направления 1
    private double currentSpeed;    // м/с
    private double desiredSpeed;    // м/с, целевая скорость машины с учетом ограничений и ситуации
    private double maxSpeed;        // м/с, максимальная скорость, которую машина может развить (персональная)

    private double accelerationParam;    // a (м/с^2), параметр IDM - максимальное ускорение
    private double baseDecelerationParam; // b (м/с^2), параметр IDM - комфортное замедление

    // laneIndex - ЛОКАЛЬНЫЙ индекс полосы для данного направления.
    // 0 = крайняя правая полоса.
    // 1 = следующая левее, и т.д.
    // lanesPerDirection - 1 = крайняя левая полоса (у осевой).
    private int laneIndex;
    private final int direction; // 0 для слева-направо, 1 для справа-налево

    private boolean wantsToChangeLaneLeft = false;
    private boolean wantsToChangeLaneRight = false;
    private double timeSinceLastLaneChange = LANE_CHANGE_COOLDOWN; // Начинаем с возможности смены

    private static final boolean CAR_DEBUG_LOGGING = true; // Включить/выключить детальное логирование для машин

    public Car(double initialPosition, double initialSpeed, double maxSpeed,
               double accelerationParam, double baseDecelerationParam,
               int localLaneIndex, // Теперь это ЛОКАЛЬНЫЙ индекс
               int direction) {
        this.id = idCounter.incrementAndGet();
        this.position = initialPosition;
        this.currentSpeed = Math.max(0, initialSpeed);
        this.maxSpeed = Math.max(0, maxSpeed); // Персональный максимум
        this.desiredSpeed = this.maxSpeed;     // Начальная желаемая скорость равна персональному максимуму
        this.accelerationParam = Math.max(0.5, accelerationParam); // Защита от слишком малых значений
        this.baseDecelerationParam = Math.max(1.0, baseDecelerationParam); // Защита

        this.laneIndex = localLaneIndex; // Сохраняем локальный индекс
        this.direction = direction;
    }

    public void updateDesiredSpeed(double externalSpeedLimitMs) {
        this.desiredSpeed = Math.min(this.maxSpeed, externalSpeedLimitMs);
        this.desiredSpeed = Math.max(0, this.desiredSpeed); // Не может быть отрицательной
    }

    public void update(double deltaTime, Car leadCar, double distanceToLeadBumperToBumper,
                       double effectiveSpeedLimit, TrafficLightState nextLightState, double distanceToLightAbs) {
        if (deltaTime <= 0) return;

        updateDesiredSpeed(effectiveSpeedLimit); // Обновляем желаемую скорость на основе лимитов

        // IDM модель:
        double deltaV = (leadCar != null) ? this.currentSpeed - leadCar.getCurrentSpeed() : 0;
        // s_0 + s_1*sqrt(v/v_0) + T*v + (v*delta_v)/(2*sqrt(a*b)) -- полная формула
        // Упрощенная: s_0 + T*v + (v*delta_v)/(2*sqrt(a*b))
        double s_star = MIN_GAP + Math.max(0, this.currentSpeed * SAFE_TIME_HEADWAY +
                (this.currentSpeed * deltaV) /
                        (2 * Math.sqrt(this.accelerationParam * this.baseDecelerationParam)));

        double freeRoadTerm = this.accelerationParam * (1 - Math.pow(this.currentSpeed / Math.max(0.1, this.desiredSpeed), ACCELERATION_EXPONENT));

        double interactionTerm = 0.0;
        if (leadCar != null && distanceToLeadBumperToBumper < 200) { // Взаимодействуем только с близкими лидерами
            interactionTerm = -this.accelerationParam * Math.pow(s_star / Math.max(0.1, distanceToLeadBumperToBumper), 2);
        }

        double lightInteractionTerm = 0.0;
        if (nextLightState == TrafficLightState.RED && distanceToLightAbs < 100 && distanceToLightAbs > 0.01) { // Учитываем светофор
            double s_star_light = MIN_GAP + this.currentSpeed * SAFE_TIME_HEADWAY; // Упрощенный s* для светофора (без deltaV)
            lightInteractionTerm = -this.accelerationParam * Math.pow(s_star_light / Math.max(0.1, distanceToLightAbs), 2);

            // Более резкое торможение, если уже близко к красному и все еще едем
            if (this.currentSpeed > 0.5 && distanceToLightAbs < MIN_GAP * 1.5 && distanceToLightAbs < this.currentSpeed * SAFE_TIME_HEADWAY * 0.7) {
                lightInteractionTerm = Math.min(lightInteractionTerm, -this.baseDecelerationParam * 1.2);
            }
            // Если почти стоим у светофора
            if (this.currentSpeed < 0.5 && distanceToLightAbs < MIN_GAP * 0.5) {
                this.currentSpeed = 0;
                lightInteractionTerm = -100; // Гарантированная остановка
            }
        }

        // Выбираем более сильное замедление (от лидера или от светофора)
        double finalInteraction = Math.min(interactionTerm, lightInteractionTerm);
        if (leadCar == null && (nextLightState != TrafficLightState.RED || distanceToLightAbs > 100 || distanceToLightAbs <= 0.01) ) {
            finalInteraction = 0; // Если нет лидера и светофор не мешает, взаимодействия нет
        }


        double finalAcceleration = freeRoadTerm + finalInteraction;
        // Ограничение ускорения/замедления
        finalAcceleration = Math.max(-this.baseDecelerationParam * 2.5, Math.min(finalAcceleration, this.accelerationParam)); // Макс. замедление и макс. ускорение

        double previousSpeed = this.currentSpeed;
        this.currentSpeed += finalAcceleration * deltaTime;
        this.currentSpeed = Math.max(0, this.currentSpeed);             // Скорость не может быть отрицательной
        this.currentSpeed = Math.min(this.currentSpeed, this.desiredSpeed); // Не быстрее желаемой (которая уже учитывает лимиты)


        // Условие для полной остановки, если скорость очень мала и ускорение почти нулевое,
        // и есть препятствие (светофор или лидер) очень близко.
        boolean fullyStoppedCondition = this.currentSpeed < 0.01 && Math.abs(finalAcceleration) < 0.01 &&
                ( (nextLightState == TrafficLightState.RED && distanceToLightAbs < MIN_GAP *0.8) ||
                        (leadCar != null && distanceToLeadBumperToBumper < MIN_GAP*0.8 && leadCar.getCurrentSpeed() < 0.1) );

        if (!fullyStoppedCondition) {
            double averageSpeedForInterval = (previousSpeed + this.currentSpeed) / 2.0;
            double deltaPos = averageSpeedForInterval * deltaTime;
            if (this.direction == 0) { // слева направо
                this.position += deltaPos;
            } else { // справа налево
                this.position -= deltaPos;
            }
        } else {
            this.currentSpeed = 0; // Принудительная остановка
        }


        // if (CAR_DEBUG_LOGGING && this.id % 10 == 1 && deltaTime > 0.001) { // Логируем реже
        //     System.out.printf("Car ID %d (Dir %d, LaneL %d) Pos: %.2f, Speed: %.1f kmh (%.2f m/s), Accel: %.2f, DesiredSpd: %.1f kmh. LeadDist: %.1f, LightDist: %.1f (%s) dt: %.3f%n",
        //             id, direction, laneIndex, position, currentSpeed * 3.6, currentSpeed, finalAcceleration, desiredSpeed*3.6,
        //             distanceToLeadBumperToBumper, distanceToLightAbs, nextLightState, deltaTime);
        // }

        timeSinceLastLaneChange += deltaTime;
        wantsToChangeLaneLeft = false; // Сбрасываем намерения на каждом шаге, они будут переоценены
        wantsToChangeLaneRight = false;
    }

    public void evaluateLaneChange(double ownAcceleration,
                                   Double leftLanePotentialAccel,   // Потенциальное ускорение на левой полосе (относительно текущей)
                                   Double rightLanePotentialAccel,  // Потенциальное ускорение на правой полосе (относительно текущей)
                                   boolean isCurrentLaneTheRightmost, // Является ли текущая полоса самой правой в данном направлении
                                   boolean isCurrentLaneTheLeftmost) { // Является ли текущая полоса самой левой

        if (timeSinceLastLaneChange < LANE_CHANGE_COOLDOWN) {
            this.wantsToChangeLaneLeft = false;
            this.wantsToChangeLaneRight = false;
            return;
        }

        this.wantsToChangeLaneLeft = false;
        this.wantsToChangeLaneRight = false;

        // Для правостороннего движения:
        // - Обгон СЛЕВА (на полосу с бОльшим локальным индексом, если 0 - правая, N-1 - левая)
        // - Возврат НАПРАВО (на полосу с меньшим локальным индексом)

        // 1. Проверка смены НАПРАВО (возврат на "домашнюю" полосу)
        //    Это актуально, если мы НЕ на самой правой полосе (isCurrentLaneTheRightmost = false)
        //    и есть куда двигаться направо (rightLanePotentialAccel != null)
        if (!isCurrentLaneTheRightmost && rightLanePotentialAccel != null) {
            // Перестраиваемся направо, если это выгоднее ИЛИ если это "правильная" полоса (LANE_CHANGE_RIGHT_BIAS)
            // и это не приведет к значительному проигрышу в ускорении.
            if (rightLanePotentialAccel + LANE_CHANGE_RIGHT_BIAS > ownAcceleration + ACCELERATION_THRESHOLD_GAIN) {
                // И если это не сильно хуже, чем оставаться на текущей (или даже лучше)
                if (rightLanePotentialAccel >= ownAcceleration - ACCELERATION_THRESHOLD_GAIN * 0.5) { // Не теряем слишком много
                    this.wantsToChangeLaneRight = true;
                }
            }
        }

        // 2. Проверка смены НАЛЕВО (обычно для обгона)
        //    Это актуально, если мы НЕ на самой левой полосе (isCurrentLaneTheLeftmost = false)
        //    и есть куда двигаться налево (leftLanePotentialAccel != null)
        if (!isCurrentLaneTheLeftmost && leftLanePotentialAccel != null) {
            boolean leftIsBetterThanCurrent = leftLanePotentialAccel > ownAcceleration + ACCELERATION_THRESHOLD_GAIN;
            boolean leftIsBetterThanRightOption = true;

            if (wantsToChangeLaneRight && rightLanePotentialAccel != null) { // Если уже есть желание направо
                // Если левая полоса ненамного лучше чем вариант с правой, остаемся с вариантом направо
                leftIsBetterThanRightOption = leftLanePotentialAccel > rightLanePotentialAccel + ACCELERATION_THRESHOLD_GAIN * 0.2;
            }

            if (leftIsBetterThanCurrent && leftIsBetterThanRightOption) {
                this.wantsToChangeLaneLeft = true;
                this.wantsToChangeLaneRight = false; // Отменяем желание направо, если левая предпочтительнее
            }
        }

        // Дополнительная логика: если мы очень медленно едем (уперлись) и текущее ускорение почти 0,
        // а на соседней полосе ЛЮБОЙ выигрыш, то пытаемся перестроиться.
        if (!wantsToChangeLaneLeft && !wantsToChangeLaneRight &&
                currentSpeed < desiredSpeed * 0.6 && // Едем медленнее 60% от желаемой
                ownAcceleration < 0.05) { // Почти не ускоряемся

            if (!isCurrentLaneTheLeftmost && leftLanePotentialAccel != null && leftLanePotentialAccel > ownAcceleration + 0.02) { // Даже небольшой выигрыш слева
                wantsToChangeLaneLeft = true;
            } else if (!isCurrentLaneTheRightmost && rightLanePotentialAccel != null && rightLanePotentialAccel > ownAcceleration + 0.02) { // Даже небольшой выигрыш справа
                wantsToChangeLaneRight = true;
            }
        }
    }


    public boolean wantsToChangeLaneLeft() { return wantsToChangeLaneLeft; }
    public boolean wantsToChangeLaneRight() { return wantsToChangeLaneRight; }

    // Принимает и устанавливает ЛОКАЛЬНЫЙ индекс полосы
    public void setLaneIndex(int newLocalLaneIndex) {
        if (CAR_DEBUG_LOGGING && this.laneIndex != newLocalLaneIndex) {
            System.out.println("Car " + id + " (Dir " + direction + ") SETTING LOCAL LANE from " + this.laneIndex + " to " + newLocalLaneIndex);
        }
        this.laneIndex = newLocalLaneIndex;
        this.wantsToChangeLaneLeft = false;
        this.wantsToChangeLaneRight = false;
        this.timeSinceLastLaneChange = 0.0;
    }

    public long getId() { return id; }
    public double getPosition() { return position; }
    public double getCurrentSpeed() { return currentSpeed; }
    public int getLaneIndex() { return laneIndex; } // Возвращает ЛОКАЛЬНЫЙ индекс
    public double getMaxSpeed() { return maxSpeed; }
    public double getDesiredSpeed() { return desiredSpeed; }
    public int getDirection() { return direction; }
    public double getAccelerationParam() { return accelerationParam; }
    public double getBaseDecelerationParam() { return baseDecelerationParam; }

    @Override
    public String toString() {
        return String.format("Car{id=%d, dir=%d, locLane=%d, pos=%.1f, spd=%.1f (%.1f km/h), desiredSpd=%.1f km/h}",
                id, direction, laneIndex, position, currentSpeed, currentSpeed * 3.6, desiredSpeed * 3.6);
    }
}
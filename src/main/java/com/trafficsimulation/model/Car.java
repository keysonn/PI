package com.trafficsimulation.model;

import java.util.concurrent.atomic.AtomicLong;

public class Car {

    private static final AtomicLong idCounter = new AtomicLong(0);

    // Константы для модели поведения (можно вынести в SimulationParameters или отдельный класс Config)
    private static final double SAFE_TIME_HEADWAY = 1.5; // Желаемое время до лидера в секундах
    private static final double MIN_DISTANCE = 5.0;      // Минимальная дистанция до лидера в метрах (бампер-к-бамперу)
    private static final double COMFORTABLE_DECELERATION = 2.0; // Комфортное замедление (м/с^2)
    private static final double MAX_DECELERATION_EMERGENCY = 7.0; // Максимальное экстренное замедление

    private final long id;
    private double position;
    private double currentSpeed; // м/с
    // targetSpeed теперь будет рассчитываться динамически в update, а не храниться как поле
    private double maxSpeed;     // м/с
    private double acceleration; // м/с^2
    private double baseDeceleration; // Базовое замедление (м/с^2), используемое для обычного торможения
    private int laneIndex;
    private final int direction; // 0: слева-направо, 1: справа-налево

    public Car(double initialPosition, double initialSpeed, double maxSpeed, double acceleration, double baseDeceleration, int laneIndex, int direction) {
        this.id = idCounter.incrementAndGet();
        this.position = initialPosition;
        this.currentSpeed = Math.abs(initialSpeed);
        this.maxSpeed = Math.abs(maxSpeed);
        this.acceleration = acceleration;
        this.baseDeceleration = baseDeceleration; // Теперь это базовое замедление
        this.laneIndex = laneIndex;
        this.direction = direction;
        System.out.println("Создан Car ID=" + this.id + " на полосе " + this.laneIndex + " в направлении " + this.direction + " со скоростью " + String.format("%.1f", currentSpeed * 3.6) + " км/ч");
    }

    /**
     * Основной метод обновления состояния машины за промежуток времени.
     * @param deltaTime Прошедшее время в секундах.
     * @param leadCar Машина впереди (или null, если нет).
     * @param distanceToLeadAbs АБСОЛЮТНОЕ расстояние до бампера машины впереди (уже учтена длина машин).
     * @param effectiveSpeedLimit Действующее ограничение скорости (м/с).
     * @param nextLightState Состояние следующего светофора (или null).
     * @param distanceToLightAbs АБСОЛЮТНОЕ расстояние до следующего светофора.
     */
    public void update(double deltaTime, Car leadCar, double distanceToLeadAbs, double effectiveSpeedLimit, TrafficLightState nextLightState, double distanceToLightAbs) {
        if (deltaTime <= 0) return;

        // 1. Определяем желаемую скорость без учета других машин (только знаки и maxSpeed)
        double desiredFreewaySpeed = Math.min(this.maxSpeed, effectiveSpeedLimit);

        // 2. Рассчитываем ускорение/замедление для достижения этой скорости
        double accelForFreeway = calculateAcceleration(desiredFreewaySpeed);

        // 3. Рассчитываем ускорение/замедление для взаимодействия с машиной впереди (IDM-подобная логика)
        double accelForLeadCar = Double.POSITIVE_INFINITY; // Если нет машины впереди, не ограничиваем
        if (leadCar != null && distanceToLeadAbs < 150) { // Рассматриваем лидера только если он достаточно близко (150м)
            // s_star - желаемая дистанция до машины впереди
            double s_star = MIN_DISTANCE + Math.max(0, this.currentSpeed * SAFE_TIME_HEADWAY +
                    (this.currentSpeed * (this.currentSpeed - leadCar.getCurrentSpeed())) /
                            (2 * Math.sqrt(this.acceleration * COMFORTABLE_DECELERATION))); // COMFORTABLE_DECELERATION - типовое значение для "b" в IDM

            // Если текущая дистанция (distanceToLeadAbs) меньше желаемой (s_star), нужно тормозить
            // Коэффициент (s_star / distanceToLeadAbs)^2 определяет, насколько сильно нужно тормозить
            // Порог distanceToLeadAbs > 0, чтобы избежать деления на ноль, если машины "внутри" друг друга
            if (distanceToLeadAbs > 0.1) { // 0.1м - очень маленькая дистанция
                accelForLeadCar = this.acceleration * (1 - Math.pow(this.currentSpeed / desiredFreewaySpeed, 4) - Math.pow(s_star / distanceToLeadAbs, 2));
            } else { // Если машины почти столкнулись или одна в другой
                accelForLeadCar = -MAX_DECELERATION_EMERGENCY; // Экстренное торможение
            }
        }

        // 4. Рассчитываем ускорение/замедление для остановки перед светофором
        double accelForTrafficLight = Double.POSITIVE_INFINITY;
        if ((nextLightState == TrafficLightState.RED || nextLightState == TrafficLightState.YELLOW) && distanceToLightAbs < 100 && distanceToLightAbs > 0.1) { // Рассматриваем светофор в 100м
            // s_stop - желаемая дистанция остановки (например, 2м до светофора)
            double s_stop = MIN_DISTANCE;
            // Это упрощенная формула, похожая на IDM для неподвижного препятствия
            accelForTrafficLight = this.acceleration * (1 - Math.pow(s_stop / distanceToLightAbs, 2));
            // Если мы уже очень близко и скорость большая, нужно тормозить сильнее
            if (this.currentSpeed > 0.1 && distanceToLightAbs < (this.currentSpeed * this.currentSpeed) / (2 * COMFORTABLE_DECELERATION) + s_stop) {
                accelForTrafficLight = -COMFORTABLE_DECELERATION; // Принудительное комфортное торможение
                if (distanceToLightAbs < s_stop + 5 && this.currentSpeed > 1){ // Очень близко
                    accelForTrafficLight = -MAX_DECELERATION_EMERGENCY;
                }
            }
            if (this.currentSpeed < 0.5 && distanceToLightAbs < s_stop +1) accelForTrafficLight = -this.currentSpeed/deltaTime; // Плавная остановка у линии


        } else if (nextLightState == TrafficLightState.GREEN && distanceToLightAbs < 20 && this.currentSpeed < 1.0){
            // Если стоим на зеленый, надо ехать
            accelForTrafficLight = this.acceleration;
        }


        // 5. Выбираем наиболее ограничивающее ускорение (минимальное из рассчитанных)
        double finalAcceleration = Math.min(accelForFreeway, Math.min(accelForLeadCar, accelForTrafficLight));

        // Ограничиваем реальное замедление базовым значением, если только не экстренная ситуация
        if (finalAcceleration < 0) {
            finalAcceleration = Math.max(finalAcceleration, -this.baseDeceleration);
            // Для очень сильного торможения от accelForLeadCar или accelForTrafficLight можно разрешить превысить baseDeceleration
            if (accelForLeadCar < -this.baseDeceleration || accelForTrafficLight < -this.baseDeceleration) {
                finalAcceleration = Math.min(accelForLeadCar, accelForTrafficLight); // Выбираем самое сильное торможение
                finalAcceleration = Math.max(finalAcceleration, -MAX_DECELERATION_EMERGENCY); // Но не более экстренного
            }
        }


        // 6. Обновляем скорость и позицию
        this.currentSpeed += finalAcceleration * deltaTime;
        this.currentSpeed = Math.max(0, this.currentSpeed); // Скорость не может быть отрицательной
        this.currentSpeed = Math.min(this.currentSpeed, this.maxSpeed); // И не выше максимальной для машины

        if (this.direction == 0) { // Слева направо
            this.position += this.currentSpeed * deltaTime + 0.5 * finalAcceleration * deltaTime * deltaTime;
        } else { // Справа налево
            this.position -= (this.currentSpeed * deltaTime + 0.5 * finalAcceleration * deltaTime * deltaTime);
        }
    }

    /**
     * Рассчитывает ускорение, необходимое для достижения желаемой скорости.
     * @param desiredSpeed Желаемая скорость (м/с).
     * @return Ускорение (положительное) или замедление (отрицательное).
     */
    private double calculateAcceleration(double desiredSpeed) {
        double speedDiff = desiredSpeed - this.currentSpeed;
        if (speedDiff > 0) {
            // Ускоряемся, но не быстрее, чем this.acceleration
            // Простая модель: если хотим ускориться, применяем полное ускорение
            // (можно сделать более сложную IDM-подобную формулу (1 - (v/v0)^delta))
            return this.acceleration * (1 - Math.pow(this.currentSpeed / Math.max(0.1, desiredSpeed), 4));
        } else if (speedDiff < 0) {
            // Замедляемся, но не быстрее, чем -this.baseDeceleration
            // Для простого замедления к желаемой скорости без препятствий
            return -this.baseDeceleration; // Например, просто базовое замедление
        }
        return 0; // Скорость уже желаемая
    }


    public long getId() { return id; }
    public double getPosition() { return position; }
    public double getCurrentSpeed() { return currentSpeed; }
    public int getLaneIndex() { return laneIndex; }
    public double getMaxSpeed() { return maxSpeed; }
    public int getDirection() { return direction; }

    // targetSpeed как таковой больше не нужен, он вычисляется в update
    // public void setTargetSpeed(double targetSpeed) { ... }


    @Override
    public String toString() {
        return String.format("Car{id=%d, dir=%d, pos=%.1f, spd=%.1f (%.1f km/h), lane=%d}",
                id, direction, position, currentSpeed, currentSpeed * 3.6, laneIndex);
    }
}
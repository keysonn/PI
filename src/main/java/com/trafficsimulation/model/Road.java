package com.trafficsimulation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList; // Потокобезопасный список для машин

/**
 * Представляет участок дороги со всеми машинами, светофорами и знаками.
 */
public class Road {

    private final double length;                  // Длина дороги (метры)
    private final RoadType type;                  // Тип дороги
    private final int numberOfLanes;              // Общее количество полос (для ТЗ берем полосы в одном направлении * кол-во направлений)
    private final int numberOfDirections;         // 1 или 2

    // Используем CopyOnWriteArrayList для машин, т.к. симуляция будет менять список,
    // а отрисовка (в другом потоке) - читать его. Это безопасно.
    private final List<Car> cars;
    // Светофоры и знаки обычно не меняются во время симуляции, можно ArrayList
    private final List<TrafficLight> trafficLights;
    private final List<RoadSign> roadSigns;

    /**
     * Конструктор дороги.
     * @param lengthKm Длина в километрах (будет ограничена 1-50).
     * @param type Тип дороги.
     * @param lanesPerDirection Кол-во полос в одном направлении (1-4).
     * @param directions Кол-во направлений (1-2).
     */
    public Road(double lengthKm, RoadType type, int lanesPerDirection, int directions) {
        // Применяем ограничения из ТЗ (Приложение п. 1.16, 1.17, 1.12, 1.13, 1.11)
        this.length = Math.max(1.0, Math.min(lengthKm, 50.0)) * 1000.0; // Переводим в метры
        this.type = type;
        int validatedLanesPerDir = Math.max(1, Math.min(lanesPerDirection, 4));
        this.numberOfDirections = Math.max(1, Math.min(directions, 2));
        this.numberOfLanes = validatedLanesPerDir * this.numberOfDirections;

        this.cars = new CopyOnWriteArrayList<>();
        this.trafficLights = new ArrayList<>();
        this.roadSigns = new ArrayList<>();
        System.out.println("Создана Road: " + this);
    }

    /** Добавляет машину на дорогу */
    public void addCar(Car car) {
        this.cars.add(car);
    }

    /** Удаляет машину с дороги */
    public void removeCar(Car car) {
        this.cars.remove(car);
    }

    /** Добавляет светофор (с проверками) */
    public void addTrafficLight(TrafficLight light) {
        if (light.getPosition() >= 0 && light.getPosition() <= this.length) {
            if (this.trafficLights.size() < 2) { // ТЗ п. 1.4 - макс 2 светофора
                this.trafficLights.add(light);
                // Сразу сортируем по позиции для удобства поиска
                Collections.sort(this.trafficLights, (l1, l2) -> Double.compare(l1.getPosition(), l2.getPosition()));
            } else {
                System.err.println("Превышен лимит светофоров (макс 2)");
            }
        } else {
            System.err.println("Светофор вне дороги: " + light);
        }
    }

    /** Добавляет знак */
    public void addRoadSign(RoadSign sign) {
        if (sign.getPosition() >= 0 && sign.getPosition() <= this.length) {
            this.roadSigns.add(sign);
            // Сортируем по позиции
            Collections.sort(this.roadSigns, (s1, s2) -> Double.compare(s1.getPosition(), s2.getPosition()));
        } else {
            System.err.println("Знак вне дороги: " + sign);
        }
    }

    // --- Геттеры для получения информации о дороге и объектах на ней ---

    public double getLength() { return length; }
    public RoadType getType() { return type; }
    public int getNumberOfLanes() { return numberOfLanes; }
    public int getNumberOfDirections() { return numberOfDirections; }

    // Возвращаем КОПИЮ списка машин, чтобы внешний код не мог изменить наш основной список
    // public List<Car> getCars() { return new ArrayList<>(cars); } // Не очень эффективно для отрисовки
    // Лучше вернуть сам CopyOnWriteArrayList, он безопасен для чтения из другого потока
    public List<Car> getCars() { return cars; }

    // Возвращаем неизменяемое представление списков светофоров и знаков
    public List<TrafficLight> getTrafficLights() { return Collections.unmodifiableList(trafficLights); }
    public List<RoadSign> getRoadSigns() { return Collections.unmodifiableList(roadSigns); }


    @Override
    public String toString() {
        return String.format("Road[type=%s, len=%.0fm, lanes=%d, cars=%d, lights=%d, signs=%d]",
                type, length, numberOfLanes, cars.size(), trafficLights.size(), roadSigns.size());
    }
}
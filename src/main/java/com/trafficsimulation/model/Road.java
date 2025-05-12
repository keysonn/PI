package com.trafficsimulation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Представляет участок дороги со всеми машинами, светофорами и знаками.
 */
public class Road {

    private final double length;
    private final RoadType type;
    private final int numberOfLanes;
    private final int numberOfDirections;

    private final List<Car> cars;
    private final List<TrafficLight> trafficLights;
    private final List<RoadSign> roadSigns;

    public Road(double lengthKm, RoadType type, int lanesPerDirection, int directions) {
        this.length = Math.max(1.0, Math.min(lengthKm, 50.0)) * 1000.0;
        this.type = type;
        int validatedLanesPerDir = Math.max(1, Math.min(lanesPerDirection, 4));
        this.numberOfDirections = Math.max(1, Math.min(directions, 2));
        this.numberOfLanes = validatedLanesPerDir * this.numberOfDirections;

        this.cars = new CopyOnWriteArrayList<>();
        this.trafficLights = new ArrayList<>();
        this.roadSigns = new ArrayList<>();
        System.out.println("Создана Road: " + this);
    }

    public void addCar(Car car) {
        this.cars.add(car);
    }

    public void removeCar(Car car) {
        this.cars.remove(car);
    }

    public void addTrafficLight(TrafficLight light) {
        if (light.getPosition() >= 0 && light.getPosition() <= this.length) {
            if (this.trafficLights.size() < 2) {
                this.trafficLights.add(light);
                Collections.sort(this.trafficLights, (l1, l2) -> Double.compare(l1.getPosition(), l2.getPosition()));
            } else {
                System.err.println("Превышен лимит светофоров (макс 2)");
            }
        } else {
            System.err.println("Светофор вне дороги: " + light);
        }
    }

    public void addRoadSign(RoadSign sign) {
        if (sign.getPosition() >= 0 && sign.getPosition() <= this.length) {
            this.roadSigns.add(sign);
            Collections.sort(this.roadSigns, (s1, s2) -> Double.compare(s1.getPosition(), s2.getPosition()));
        } else {
            System.err.println("Знак вне дороги: " + sign);
        }
    }

    /** Очищает список светофоров на дороге */
    public void clearTrafficLights() {
        if (this.trafficLights != null) {
            this.trafficLights.clear();
        }
    }

    /** Очищает список дорожных знаков на дороге */
    public void clearRoadSigns() {
        if (this.roadSigns != null) {
            this.roadSigns.clear();
        }
    }

    public double getLength() { return length; }
    public RoadType getType() { return type; }
    public int getNumberOfLanes() { return numberOfLanes; }
    public int getNumberOfDirections() { return numberOfDirections; }
    public List<Car> getCars() { return cars; }
    public List<TrafficLight> getTrafficLights() { return Collections.unmodifiableList(trafficLights); }
    public List<RoadSign> getRoadSigns() { return Collections.unmodifiableList(roadSigns); }

    @Override
    public String toString() {
        return String.format("Road[type=%s, len=%.0fm, lanes=%d, cars=%d, lights=%d, signs=%d]",
                type, length, numberOfLanes, cars.size(), trafficLights.size(), roadSigns.size());
    }
}
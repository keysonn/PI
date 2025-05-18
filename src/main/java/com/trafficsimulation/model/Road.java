package com.trafficsimulation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Road {

    private final double length;
    private final RoadType type;
    private final int lanesPerDirection;
    private final int numberOfDirections;
    private final int totalLanes;

    private final List<Car> cars;
    private final List<TrafficLight> trafficLights;
    private final List<RoadSign> roadSigns;

    public Road(double lengthKm, RoadType type, int lanesPerDirParam, int directionsParam) {
        this.length = Math.max(1.0, Math.min(lengthKm, 50.0)) * 1000.0;
        this.type = type;
        this.numberOfDirections = Math.max(1, Math.min(directionsParam, 2));
        this.lanesPerDirection = Math.max(1, Math.min(lanesPerDirParam, 4));
        this.totalLanes = this.lanesPerDirection * this.numberOfDirections;

        this.cars = new CopyOnWriteArrayList<>();
        this.trafficLights = new ArrayList<>();
        this.roadSigns = new ArrayList<>();
    }

    public void addCar(Car car) {
        if (car != null) {
            this.cars.add(car);
        }
    }

    public void removeCar(Car car) {
        if (car != null) {
            this.cars.remove(car);
        }
    }

    public void addTrafficLight(TrafficLight light) {
        if (light != null && light.getPosition() >= 0 && light.getPosition() <= this.length) {
            boolean canAdd = (this.type == RoadType.TUNNEL && light.isExternallyControlled()) ||
                    (this.trafficLights.stream().filter(tl -> !tl.isExternallyControlled()).count() < 2 &&
                            !containsTrafficLightAt(light.getPosition()));

            if (canAdd) {
                this.trafficLights.add(light);
                this.trafficLights.sort(Comparator.comparingDouble(TrafficLight::getPosition));
            } else if (this.type != RoadType.TUNNEL && this.trafficLights.stream().filter(tl -> !tl.isExternallyControlled()).count() >= 2) {
                System.err.println("Road: Превышен лимит пользовательских светофоров (макс 2). Светофор не добавлен: " + light);
            }
        } else if (light != null) {
            System.err.println("Road: Попытка добавить светофор вне дороги: " + light);
        }
    }

    private boolean containsTrafficLightAt(double position) {
        for (TrafficLight tl : trafficLights) {
            if (Math.abs(tl.getPosition() - position) < 0.1) return true;
        }
        return false;
    }

    public void removeTrafficLight(TrafficLight light) {
        if (light != null) {
            this.trafficLights.remove(light);
        }
    }

    public void clearTrafficLights() {
        if (this.trafficLights != null) {
            this.trafficLights.clear();
        }
    }

    public void addRoadSign(RoadSign sign) {
        if (sign != null && sign.getPosition() >= 0 && sign.getPosition() <= this.length) {
            this.roadSigns.add(sign);
            this.roadSigns.sort(Comparator.comparingDouble(RoadSign::getPosition));
        } else if (sign != null) {
            System.err.println("Road: Попытка добавить знак вне дороги: " + sign);
        }
    }

    public void removeRoadSign(RoadSign sign) {
        if (sign != null) {
            this.roadSigns.remove(sign);
        }
    }

    public void clearRoadSigns() {
        if (this.roadSigns != null) {
            this.roadSigns.clear();
        }
    }

    public int getGlobalLaneIndexForDrawing(int localLaneIndex, int modelDirection) {
        if (localLaneIndex < 0 || localLaneIndex >= lanesPerDirection) {
            return -1;
        }
        if (modelDirection == 1) { // Модельное R->L (верхний блок экрана)
            return localLaneIndex; // local 0 (правая) -> global 0 (верхняя)
        } else if (modelDirection == 0) { // Модельное L->R (нижний блок или единственная для односторонней)
            if (numberOfDirections == 1) { // Односторонняя L->R
                return localLaneIndex; // global == local
            } else { // Двухсторонняя, нижний блок L->R
                // local 0 (правая) -> global lanesPerDirection + (lanesPerDirection - 1) (самая нижняя)
                // local N-1 (левая) -> global lanesPerDirection (у осевой)
                return lanesPerDirection + (lanesPerDirection - 1 - localLaneIndex);
            }
        }
        return -1;
    }

    public int getFirstDirection() {
        if (numberOfDirections == 1) {
            return 0;
        }
        return -1;
    }

    public double getLength() { return length; }
    public RoadType getType() { return type; }
    public int getLanesPerDirection() { return lanesPerDirection; }
    public int getNumberOfDirections() { return numberOfDirections; }
    public int getNumberOfLanes() { return totalLanes; }
    public List<Car> getCars() { return cars; }
    public List<TrafficLight> getTrafficLights() { return Collections.unmodifiableList(new ArrayList<>(trafficLights)); }
    public List<RoadSign> getRoadSigns() { return Collections.unmodifiableList(new ArrayList<>(roadSigns)); }

    @Override
    public String toString() {
        return String.format("Road[type=%s, len=%.0fm, dirs=%d (model), lanesPerDir=%d (totalLanes=%d), cars=%d, lights=%d, signs=%d]",
                type, length, numberOfDirections, lanesPerDirection, totalLanes, cars.size(), trafficLights.size(), roadSigns.size());
    }
}
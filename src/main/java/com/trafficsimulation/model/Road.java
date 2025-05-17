package com.trafficsimulation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Road {

    private final double length; // в метрах
    private final RoadType type;
    private final int lanesPerDirection;
    private final int numberOfDirections; // Количество МОДЕЛЬНЫХ направлений
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
                    (this.trafficLights.size() < 2 && !containsTrafficLightAt(light.getPosition()));

            if (canAdd) {
                this.trafficLights.add(light);
                this.trafficLights.sort(Comparator.comparingDouble(TrafficLight::getPosition));
            } else if (this.trafficLights.size() >= 2 && this.type != RoadType.TUNNEL) {
                System.err.println("Road: Превышен лимит светофоров (макс 2 для не-тоннеля). Светофор не добавлен: " + light);
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

    /**
     * Преобразует локальный индекс полосы (0=правая, N-1=левая для своего направления)
     * и модельное направление машины в глобальный индекс полосы для отрисовки.
     * 0 - самая верхняя полоса на экране.
     *
     * @param localLaneIndex локальный индекс полосы машины (0 для крайней правой)
     * @param modelDirection модельное направление машины (0 для L->R, 1 для R->L)
     * @return глобальный индекс полосы для отрисовки или -1, если ошибка
     */
    public int getGlobalLaneIndexForDrawing(int localLaneIndex, int modelDirection) {
        if (localLaneIndex < 0 || localLaneIndex >= lanesPerDirection) {
            System.err.println("Road.getGlobalLaneIndexForDrawing: Invalid localLaneIndex " + localLaneIndex + " for lanesPerDirection " + lanesPerDirection);
            return -1;
        }

        // Определяем, какой это блок полос на экране:
        // Верхний блок (screenBlock = 0) -> для машин, едущих R->L (modelDirection = 1)
        // Нижний блок (screenBlock = 1) -> для машин, едущих L->R (modelDirection = 0)
        int screenBlock;

        if (numberOfDirections == 1) {
            // Односторонняя дорога (в нашей модели всегда modelDirection = 0)
            // Рисуется как "нижний" блок, если бы был второй.
            // Если полос всего N, то локальный индекс 0..N-1 совпадает с глобальным 0..N-1.
            if (modelDirection == 0) { // L->R
                screenBlock = 0; // Рисуем в единственном доступном блоке, который считаем верхним для простоты глобальной нумерации
            } else { // R->L - этого не должно быть для нашей односторонней
                System.err.println("Road.getGlobalLaneIndexForDrawing: Single direction road with unexpected modelDirection " + modelDirection);
                return -1;
            }
        } else { // Двухсторонняя
            if (modelDirection == 1) { // Модельное R->L, рисуется в ВЕРХНЕМ блоке экрана
                screenBlock = 0;
            } else { // Модельное L->R, рисуется в НИЖНЕМ блоке экрана
                screenBlock = 1;
            }
        }

        if (screenBlock == 0) { // Верхний блок экрана
            // Локальная 0 (правая для этого потока R->L) -> Глобальная 0 (самая верхняя)
            // Локальная 1 (левее) -> Глобальная 1
            // ...
            // Локальная N-1 (левая, у осевой) -> Глобальная N-1
            return localLaneIndex;
        } else { // screenBlock == 1, Нижний блок экрана (или единственный блок для односторонней)
            int baseGlobalIndex = (numberOfDirections == 2) ? lanesPerDirection : 0;
            // Локальная 0 (правая для этого потока L->R) -> Глобальная base + 0
            // Локальная 1 (левее) -> Глобальная base + 1
            // ...
            // Локальная N-1 (левая, у осевой) -> Глобальная base + (N-1)
            // НО! Это если бы нижний блок тоже нумеровался сверху вниз.
            // А мы хотим, чтобы локальная 0 (правая) для нижнего потока была САМОЙ ПРАВОЙ (т.е. самой ВНЕШНЕЙ из нижних)
            // Глобальные индексы идут сверху вниз: 0, 1, ... (LPD-1), LPD, ... (2*LPD-1)

            // Верхний блок (modelDir=1): local 0..LPD-1 -> global 0..LPD-1
            // Нижний блок (modelDir=0): local 0..LPD-1 -> global LPD .. 2*LPD-1
            // Для нижнего блока:
            //   локальная 0 (правая) должна стать глобальной LPD + (LPD-1) -- самая нижняя
            //   локальная LPD-1 (левая) должна стать глобальной LPD -- ближайшая к осевой

            // Пересмотренная логика для нижнего блока (modelDirection=0):
            if (modelDirection == 0) {
                if (numberOfDirections == 1) { // Односторонняя L->R
                    // Локальная 0 (правая) -> Глобальная 0
                    // Локальная N-1 (левая) -> Глобальная N-1
                    return localLaneIndex;
                } else { // Двухсторонняя, нижний блок L->R
                    // Локальная 0 (правая) -> Глобальная lanesPerDirection + (lanesPerDirection - 1) (самая нижняя из всех)
                    // Локальная 1 (левее) -> Глобальная lanesPerDirection + (lanesPerDirection - 2)
                    // ...
                    // Локальная lanesPerDirection-1 (левая, у осевой) -> Глобальная lanesPerDirection
                    return lanesPerDirection + (lanesPerDirection - 1 - localLaneIndex);
                }
            } else { // modelDirection == 1, верхний блок R->L
                // Локальная 0 (правая) -> Глобальная 0 (самая верхняя из всех)
                // Локальная 1 (левее) -> Глобальная 1
                // ...
                // Локальная lanesPerDirection-1 (левая, у осевой) -> Глобальная lanesPerDirection - 1
                return localLaneIndex;
            }
        }
    }

    /**
     * Возвращает модельное направление дороги, если она односторонняя.
     * В нашей текущей реализации односторонняя дорога всегда имеет модельное направление 0 (L->R).
     * @return модельное направление (0 или 1) или -1 если двухсторонняя.
     */
    public int getFirstDirection() {
        if (numberOfDirections == 1) {
            return 0; // По нашей договоренности, односторонняя всегда modelDirection = 0
        }
        return -1; // Не применимо для двухсторонней в этом контексте
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
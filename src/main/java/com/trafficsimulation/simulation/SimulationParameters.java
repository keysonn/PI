package com.trafficsimulation.simulation;

import com.trafficsimulation.model.RoadType; // Импортируем тип дороги

/**
 * Хранит все настраиваемые параметры симуляции,
 * которые будут задаваться пользователем через GUI.
 */
public class SimulationParameters {

    // Параметры дороги (значения по умолчанию, будут меняться из GUI)
    private RoadType roadType = RoadType.CITY_ROAD;
    private double roadLengthKm = 5.0; // Длина дороги в км (ТЗ: 1-50)
    private int lanesPerDirection = 2;  // Полос в одном направлении (ТЗ: 1-4)
    private int numberOfDirections = 1; // Направлений движения (ТЗ: 1-2)

    // Параметры светофоров (ТЗ: 0-2 светофора)
    // Пока не храним их здесь, будем добавлять на дорогу напрямую

    // Параметры потока (ТЗ п. 1.14 - 2 типа потока: детерминированный/случайный)
    private boolean isRandomFlow = false; // По умолчанию - детерминированный

    // --- Параметры для детерминированного потока ---
    private double deterministicIntervalSeconds = 15.0; // Время между машинами (ТЗ п. 1.19-1.20: 10-15 у.е., берем секунды)
    private double deterministicSpeedKmh = 60.0;       // Скорость машин (ТЗ п. 5.1.1)

    // --- Параметры для случайного потока ---
    // TODO: Добавить параметры для случайного потока (закон распределения, МО, дисперсия и т.д.)
    // Например:
    // private DistributionType timeDistribution = DistributionType.NORMAL;
    // private double meanTimeInterval = ...;
    // private double varianceTimeInterval = ...;
    // private DistributionType speedDistribution = ...;
    // ... и так далее

    // Параметры визуализации и симуляции
    private double simulationTimeStep = 0.1; // Шаг времени симуляции в секундах
    private double simulationSpeedFactor = 1.0; // Ускорение времени (1.0 = реальное время)

    // --- Геттеры (для получения значений параметров) ---

    public RoadType getRoadType() { return roadType; }
    public double getRoadLengthKm() { return roadLengthKm; }
    public int getLanesPerDirection() { return lanesPerDirection; }
    public int getNumberOfDirections() { return numberOfDirections; }
    public boolean isRandomFlow() { return isRandomFlow; }
    public double getDeterministicIntervalSeconds() { return deterministicIntervalSeconds; }
    public double getDeterministicSpeedKmh() { return deterministicSpeedKmh; }
    public double getSimulationTimeStep() { return simulationTimeStep; }
    public double getSimulationSpeedFactor() { return simulationSpeedFactor; }

    // --- Сеттеры (для изменения параметров из GUI) ---
    // Добавим сеттеры по мере необходимости, когда будем делать GUI

    public void setRoadType(RoadType roadType) { this.roadType = roadType; }
    public void setRoadLengthKm(double roadLengthKm) { this.roadLengthKm = roadLengthKm; }
    // ... и так далее для остальных параметров ...

    @Override
    public String toString() {
        // Удобно для отладки
        return "SimulationParameters{" +
                "roadType=" + roadType +
                ", roadLengthKm=" + roadLengthKm +
                ", lanesPerDirection=" + lanesPerDirection +
                ", numberOfDirections=" + numberOfDirections +
                ", isRandomFlow=" + isRandomFlow +
                // ... добавить другие параметры ...
                '}';
    }
}
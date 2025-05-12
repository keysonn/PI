package com.trafficsimulation.simulation;

import com.trafficsimulation.model.RoadType;

/**
 * Хранит все настраиваемые параметры симуляции.
 */
public class SimulationParameters {

    // Параметры дороги
    private RoadType roadType = RoadType.CITY_ROAD;
    private double roadLengthKm = 5.0;
    private int lanesPerDirection = 2;
    private int numberOfDirections = 1;

    // Параметры потока
    private boolean isRandomFlow = false;

    // --- Детерминированный поток ---
    private double deterministicIntervalSeconds = 15.0;
    private double deterministicSpeedKmh = 60.0;

    // --- Случайный поток: Параметры для ВРЕМЕНИ ПОЯВЛЕНИЯ ---
    private DistributionLaw timeDistributionLaw = DistributionLaw.NORMAL;
    // Равномерное (время)
    private double timeUniformMinSec = 10.0;  // ТЗ п. 1.19 (мин. интервал)
    private double timeUniformMaxSec = 15.0;  // ТЗ п. 1.20 (макс. интервал)
    // Нормальное (время)
    private double timeNormalMeanSec = 20.0;  // ТЗ п. 1.21 (мин. МО) - установим начальное значение
    private double timeNormalVarianceSec = 5.0; // ТЗ п. 1.23-1.24 (дисперсия 0-20)
    // Показательное (время)
    private double timeExponentialIntensityPerSec = 1.0 / 15.0; // 1 машина / 15 сек (ТЗ п. 1.25-1.26 интенсивность 1-20 авто/с - это очень много, возьмем по смыслу интервала)

    // --- Случайный поток: Параметры для СКОРОСТНОГО РЕЖИМА ---
    private DistributionLaw speedDistributionLaw = DistributionLaw.NORMAL;
    // Равномерное (скорость)
    private double speedUniformMinKmh = 40.0;
    private double speedUniformMaxKmh = 80.0;
    // Нормальное (скорость)
    private double speedNormalMeanKmh = 60.0;     // ТЗ п. 1.27 (мин. МО скорости)
    private double speedNormalVarianceKmh = 10.0;   // ТЗ п. 1.29-1.30 (дисперсия скорости 0-40)
    // Показательное (скорость) - менее типично для скорости, но для примера
    private double speedExponentialIntensityPerKmh = 1.0 / 60.0; // Параметр для генерации, если бы скорость была по экспоненте

    // Параметры симуляции
    private double simulationTimeStep = 0.1;
    private double simulationSpeedFactor = 1.0;

    // --- Геттеры ---
    public RoadType getRoadType() { return roadType; }
    public double getRoadLengthKm() { return roadLengthKm; }
    public int getLanesPerDirection() { return lanesPerDirection; }
    public int getNumberOfDirections() { return numberOfDirections; }
    public boolean isRandomFlow() { return isRandomFlow; }
    public double getDeterministicIntervalSeconds() { return deterministicIntervalSeconds; }
    public double getDeterministicSpeedKmh() { return deterministicSpeedKmh; }

    public DistributionLaw getTimeDistributionLaw() { return timeDistributionLaw; }
    public double getTimeUniformMinSec() { return timeUniformMinSec; }
    public double getTimeUniformMaxSec() { return timeUniformMaxSec; }
    public double getTimeNormalMeanSec() { return timeNormalMeanSec; }
    public double getTimeNormalVarianceSec() { return timeNormalVarianceSec; }
    public double getTimeExponentialIntensityPerSec() { return timeExponentialIntensityPerSec; }

    public DistributionLaw getSpeedDistributionLaw() { return speedDistributionLaw; }
    public double getSpeedUniformMinKmh() { return speedUniformMinKmh; }
    public double getSpeedUniformMaxKmh() { return speedUniformMaxKmh; }
    public double getSpeedNormalMeanKmh() { return speedNormalMeanKmh; }
    public double getSpeedNormalVarianceKmh() { return speedNormalVarianceKmh; }
    public double getSpeedExponentialIntensityPerKmh() { return speedExponentialIntensityPerKmh; }

    public double getSimulationTimeStep() { return simulationTimeStep; }
    public double getSimulationSpeedFactor() { return simulationSpeedFactor; }

    // --- Сеттеры ---
    public void setRoadType(RoadType roadType) { this.roadType = roadType; }
    public void setRoadLengthKm(double roadLengthKm) { this.roadLengthKm = roadLengthKm; }
    public void setLanesPerDirection(int lanes) { this.lanesPerDirection = lanes; }
    public void setNumberOfDirections(int directions) { this.numberOfDirections = directions; }
    public void setRandomFlow(boolean isRandom) { this.isRandomFlow = isRandom; }
    public void setDeterministicIntervalSeconds(double interval) { this.deterministicIntervalSeconds = interval; }
    public void setDeterministicSpeedKmh(double speed) { this.deterministicSpeedKmh = speed; }

    public void setTimeDistributionLaw(DistributionLaw law) { this.timeDistributionLaw = law; }
    public void setTimeUniformMinSec(double min) { this.timeUniformMinSec = min; }
    public void setTimeUniformMaxSec(double max) { this.timeUniformMaxSec = max; }
    public void setTimeNormalMeanSec(double mean) { this.timeNormalMeanSec = mean; }
    public void setTimeNormalVarianceSec(double variance) { this.timeNormalVarianceSec = variance; }
    public void setTimeExponentialIntensityPerSec(double intensity) { this.timeExponentialIntensityPerSec = intensity; }

    public void setSpeedDistributionLaw(DistributionLaw law) { this.speedDistributionLaw = law; }
    public void setSpeedUniformMinKmh(double min) { this.speedUniformMinKmh = min; }
    public void setSpeedUniformMaxKmh(double max) { this.speedUniformMaxKmh = max; }
    public void setSpeedNormalMeanKmh(double mean) { this.speedNormalMeanKmh = mean; }
    public void setSpeedNormalVarianceKmh(double variance) { this.speedNormalVarianceKmh = variance; }
    public void setSpeedExponentialIntensityPerKmh(double intensity) { this.speedExponentialIntensityPerKmh = intensity; }

    public void setSimulationTimeStep(double step) { this.simulationTimeStep = step; }
    public void setSimulationSpeedFactor(double factor) { this.simulationSpeedFactor = factor; }

    @Override
    public String toString() {
        return "SimulationParameters{" +
                "roadType=" + roadType +
                ", roadLengthKm=" + roadLengthKm +
                ", lanesPerDirection=" + lanesPerDirection +
                ", numberOfDirections=" + numberOfDirections +
                ", isRandomFlow=" + isRandomFlow +
                ", detInterval=" + deterministicIntervalSeconds +
                ", detSpeed=" + deterministicSpeedKmh +
                // Добавить вывод параметров случайного потока для отладки
                '}';
    }
}
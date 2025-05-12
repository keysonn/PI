package com.trafficsimulation.simulation;

import com.trafficsimulation.model.RoadType;

public class SimulationParameters {

    // Дорога
    private RoadType roadType = RoadType.CITY_ROAD;
    private double roadLengthKm = 5.0;
    private int lanesPerDirection = 2;
    private int numberOfDirections = 1;

    // --- Новые поля для настроек светофоров тоннеля ---
    private double tunnelRedDuration = 30.0;  // Значение по умолчанию
    private double tunnelGreenDuration = 30.0; // Значение по умолчанию

    // Флаги случайности
    private boolean isRandomSpeedFlow = false;
    private boolean isRandomTimeFlow = false;

    // Детерминированный поток
    private double deterministicIntervalSeconds = 10.0;
    private double deterministicSpeedKmh = 60.0;

    // Случайный поток - Время
    private DistributionLaw timeDistributionLaw = DistributionLaw.NORMAL;
    private double timeUniformMinSec = 10.0;
    private double timeUniformMaxSec = 15.0;
    private double timeNormalMeanSec = 20.0;
    private double timeNormalVarianceSec = 5.0;
    private double timeExponentialIntensityPerSec = 1.0;

    // Случайный поток - Скорость
    private DistributionLaw speedDistributionLaw = DistributionLaw.NORMAL;
    private double speedUniformMinKmh = 40.0;
    private double speedUniformMaxKmh = 80.0;
    private double speedNormalMeanKmh = 60.0;
    private double speedNormalVarianceKmh = 10.0;
    private double speedExponentialIntensityPerKmh = 0.05;

    // Симуляция
    private double simulationTimeStep = 0.1;
    private double simulationSpeedFactor = 1.0;

    // --- Геттеры ---
    public RoadType getRoadType() { return roadType; }
    public double getRoadLengthKm() { return roadLengthKm; }
    public int getLanesPerDirection() { return lanesPerDirection; }
    public int getNumberOfDirections() { return numberOfDirections; }

    public double getTunnelRedDuration() { return tunnelRedDuration; }   // Новый
    public double getTunnelGreenDuration() { return tunnelGreenDuration; } // Новый

    public boolean isRandomSpeedFlow() { return isRandomSpeedFlow; }
    public boolean isRandomTimeFlow() { return isRandomTimeFlow; }

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

    public void setTunnelRedDuration(double duration) { this.tunnelRedDuration = duration; }   // Новый
    public void setTunnelGreenDuration(double duration) { this.tunnelGreenDuration = duration; } // Новый

    public void setRandomSpeedFlow(boolean randomSpeedFlow) { this.isRandomSpeedFlow = randomSpeedFlow; }
    public void setRandomTimeFlow(boolean randomTimeFlow) { this.isRandomTimeFlow = randomTimeFlow; }

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
        return "SimulationParameters{" + /* ... как раньше, можно добавить параметры тоннеля ... */ '}';
    }
}
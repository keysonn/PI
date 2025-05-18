package com.trafficsimulation.simulation;

import com.trafficsimulation.model.RoadType;

public class SimulationParameters {

    private RoadType roadType = RoadType.CITY_ROAD;
    private int lanesPerDirection = 2;
    private int numberOfDirections = 1;
    private final double roadLengthKm = 2.0; // Фиксированная длина

    private boolean randomSpeedFlow = true;
    private double deterministicSpeedKmh = 60.0;
    private DistributionLaw speedDistributionLaw = DistributionLaw.NORMAL;
    private double speedUniformMinKmh = 40.0;
    private double speedUniformMaxKmh = 80.0;
    private double speedNormalMeanKmh = 60.0;
    private double speedNormalVarianceKmh = 100.0; // (км/ч)^2, дает СКО 10 км/ч
    private double speedExponentialIntensityPerKmh = 0.02; // 1/50

    private boolean randomTimeFlow = true;
    private double deterministicIntervalSeconds = 5.0;
    private DistributionLaw timeDistributionLaw = DistributionLaw.NORMAL;
    private double timeUniformMinSec = 2.0;
    private double timeUniformMaxSec = 8.0;
    private double timeNormalMeanSec = 5.0;
    private double timeNormalVarianceSec = 4.0; // СКО 2с
    private double timeExponentialIntensityPerSec = 0.5; // 0.5 авто/сек -> средний интервал 2с

    private double simulationSpeedFactor = 1.0;

    // Параметры для тоннеля (время очистки теперь рассчитывается в SimulationEngine)
    private double tunnelDefaultGreenDuration = 30.0;
    private double tunnelDefaultRedDuration = 30.0; // Используется для системных светофоров тоннеля

    public SimulationParameters() {
    }

    public RoadType getRoadType() {
        return roadType;
    }

    public void setRoadType(RoadType roadType) {
        this.roadType = roadType;
    }

    public int getLanesPerDirection() {
        return lanesPerDirection;
    }

    public void setLanesPerDirection(int lanesPerDirection) {
        this.lanesPerDirection = Math.max(1, Math.min(lanesPerDirection, 4));
    }

    public int getNumberOfDirections() {
        return numberOfDirections;
    }

    public void setNumberOfDirections(int numberOfDirections) {
        this.numberOfDirections = Math.max(1, Math.min(numberOfDirections, 2));
    }

    public double getRoadLengthKm() {
        return roadLengthKm;
    }

    public boolean isRandomSpeedFlow() {
        return randomSpeedFlow;
    }

    public void setRandomSpeedFlow(boolean randomSpeedFlow) {
        this.randomSpeedFlow = randomSpeedFlow;
    }

    public double getDeterministicSpeedKmh() {
        return deterministicSpeedKmh;
    }

    public void setDeterministicSpeedKmh(double deterministicSpeedKmh) {
        this.deterministicSpeedKmh = deterministicSpeedKmh;
    }

    public DistributionLaw getSpeedDistributionLaw() {
        return speedDistributionLaw;
    }

    public void setSpeedDistributionLaw(DistributionLaw speedDistributionLaw) {
        this.speedDistributionLaw = speedDistributionLaw;
    }

    public double getSpeedUniformMinKmh() {
        return speedUniformMinKmh;
    }

    public void setSpeedUniformMinKmh(double speedUniformMinKmh) {
        this.speedUniformMinKmh = speedUniformMinKmh;
    }

    public double getSpeedUniformMaxKmh() {
        return speedUniformMaxKmh;
    }

    public void setSpeedUniformMaxKmh(double speedUniformMaxKmh) {
        this.speedUniformMaxKmh = speedUniformMaxKmh;
    }

    public double getSpeedNormalMeanKmh() {
        return speedNormalMeanKmh;
    }

    public void setSpeedNormalMeanKmh(double speedNormalMeanKmh) {
        this.speedNormalMeanKmh = speedNormalMeanKmh;
    }

    public double getSpeedNormalVarianceKmh() {
        return speedNormalVarianceKmh;
    }

    public void setSpeedNormalVarianceKmh(double speedNormalVarianceKmh) {
        this.speedNormalVarianceKmh = Math.max(0, speedNormalVarianceKmh);
    }

    public double getSpeedExponentialIntensityPerKmh() {
        return speedExponentialIntensityPerKmh;
    }

    public void setSpeedExponentialIntensityPerKmh(double speedExponentialIntensityPerKmh) {
        this.speedExponentialIntensityPerKmh = Math.max(0.001, speedExponentialIntensityPerKmh);
    }

    public boolean isRandomTimeFlow() {
        return randomTimeFlow;
    }

    public void setRandomTimeFlow(boolean randomTimeFlow) {
        this.randomTimeFlow = randomTimeFlow;
    }

    public double getDeterministicIntervalSeconds() {
        return deterministicIntervalSeconds;
    }

    public void setDeterministicIntervalSeconds(double deterministicIntervalSeconds) {
        this.deterministicIntervalSeconds = Math.max(0.1, deterministicIntervalSeconds);
    }

    public DistributionLaw getTimeDistributionLaw() {
        return timeDistributionLaw;
    }

    public void setTimeDistributionLaw(DistributionLaw timeDistributionLaw) {
        this.timeDistributionLaw = timeDistributionLaw;
    }

    public double getTimeUniformMinSec() {
        return timeUniformMinSec;
    }

    public void setTimeUniformMinSec(double timeUniformMinSec) {
        this.timeUniformMinSec = Math.max(0.1, timeUniformMinSec);
    }

    public double getTimeUniformMaxSec() {
        return timeUniformMaxSec;
    }

    public void setTimeUniformMaxSec(double timeUniformMaxSec) {
        this.timeUniformMaxSec = Math.max(0.1, timeUniformMaxSec);
    }

    public double getTimeNormalMeanSec() {
        return timeNormalMeanSec;
    }

    public void setTimeNormalMeanSec(double timeNormalMeanSec) {
        this.timeNormalMeanSec = Math.max(0.1, timeNormalMeanSec);
    }

    public double getTimeNormalVarianceSec() {
        return timeNormalVarianceSec;
    }

    public void setTimeNormalVarianceSec(double timeNormalVarianceSec) {
        this.timeNormalVarianceSec = Math.max(0, timeNormalVarianceSec);
    }

    public double getTimeExponentialIntensityPerSec() {
        return timeExponentialIntensityPerSec;
    }

    public void setTimeExponentialIntensityPerSec(double timeExponentialIntensityPerSec) {
        this.timeExponentialIntensityPerSec = Math.max(0.01, timeExponentialIntensityPerSec);
    }

    public double getSimulationSpeedFactor() {
        return simulationSpeedFactor;
    }

    public void setSimulationSpeedFactor(double simulationSpeedFactor) {
        this.simulationSpeedFactor = Math.max(0.1, Math.min(simulationSpeedFactor, 10.0)); // e.g. 0.1x to 10x
    }

    public double getTunnelDefaultGreenDuration() {
        return tunnelDefaultGreenDuration;
    }

    public void setTunnelDefaultGreenDuration(double tunnelDefaultGreenDuration) {
        this.tunnelDefaultGreenDuration = Math.max(10, tunnelDefaultGreenDuration);
    }

    public double getTunnelDefaultRedDuration() {
        return tunnelDefaultRedDuration;
    }

    public void setTunnelDefaultRedDuration(double tunnelDefaultRedDuration) {
        this.tunnelDefaultRedDuration = Math.max(10, tunnelDefaultRedDuration);
    }
}
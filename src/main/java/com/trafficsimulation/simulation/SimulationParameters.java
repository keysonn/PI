package com.trafficsimulation.simulation;

import com.trafficsimulation.model.RoadType;

public class SimulationParameters {

    private RoadType roadType = RoadType.CITY_ROAD;
    private double roadLengthKm = 5.0;
    private int lanesPerDirection = 2;
    private int numberOfDirections = 1;

    private boolean isRandomSpeedFlow = false;
    private boolean isRandomTimeFlow = false;

    private double deterministicIntervalSeconds = 10.0;
    private double deterministicSpeedKmh = 60.0;

    private DistributionLaw timeDistributionLaw = DistributionLaw.NORMAL;
    private double timeUniformMinSec = 10.0;
    private double timeUniformMaxSec = 15.0;
    private double timeNormalMeanSec = 20.0;
    private double timeNormalVarianceSec = 5.0;
    private double timeExponentialIntensityPerSec = 1.0;

    private DistributionLaw speedDistributionLaw = DistributionLaw.NORMAL;
    private double speedUniformMinKmh = 40.0;
    private double speedUniformMaxKmh = 80.0;
    private double speedNormalMeanKmh = 60.0;
    private double speedNormalVarianceKmh = 10.0;
    private double speedExponentialIntensityPerKmh = 0.05;

    private double simulationTimeStep = 0.1;
    private double simulationSpeedFactor = 1.0;

    public RoadType getRoadType() { return roadType; }
    public void setRoadType(RoadType roadType) { this.roadType = roadType; }
    public double getRoadLengthKm() { return roadLengthKm; }
    public void setRoadLengthKm(double roadLengthKm) { this.roadLengthKm = roadLengthKm; }
    public int getLanesPerDirection() { return lanesPerDirection; }
    public void setLanesPerDirection(int lanes) { this.lanesPerDirection = lanes; }
    public int getNumberOfDirections() { return numberOfDirections; }
    public void setNumberOfDirections(int directions) { this.numberOfDirections = directions; }

    public boolean isRandomSpeedFlow() { return isRandomSpeedFlow; }
    public void setRandomSpeedFlow(boolean randomSpeedFlow) { this.isRandomSpeedFlow = randomSpeedFlow; }
    public boolean isRandomTimeFlow() { return isRandomTimeFlow; }
    public void setRandomTimeFlow(boolean randomTimeFlow) { this.isRandomTimeFlow = randomTimeFlow; }

    public double getDeterministicIntervalSeconds() { return deterministicIntervalSeconds; }
    public void setDeterministicIntervalSeconds(double interval) { this.deterministicIntervalSeconds = interval; }
    public double getDeterministicSpeedKmh() { return deterministicSpeedKmh; }
    public void setDeterministicSpeedKmh(double speed) { this.deterministicSpeedKmh = speed; }

    public DistributionLaw getTimeDistributionLaw() { return timeDistributionLaw; }
    public void setTimeDistributionLaw(DistributionLaw law) { this.timeDistributionLaw = law; }
    public double getTimeUniformMinSec() { return timeUniformMinSec; }
    public void setTimeUniformMinSec(double min) { this.timeUniformMinSec = min; }
    public double getTimeUniformMaxSec() { return timeUniformMaxSec; }
    public void setTimeUniformMaxSec(double max) { this.timeUniformMaxSec = max; }
    public double getTimeNormalMeanSec() { return timeNormalMeanSec; }
    public void setTimeNormalMeanSec(double mean) { this.timeNormalMeanSec = mean; }
    public double getTimeNormalVarianceSec() { return timeNormalVarianceSec; }
    public void setTimeNormalVarianceSec(double variance) { this.timeNormalVarianceSec = variance; }
    public double getTimeExponentialIntensityPerSec() { return timeExponentialIntensityPerSec; }
    public void setTimeExponentialIntensityPerSec(double intensity) { this.timeExponentialIntensityPerSec = intensity; }

    public DistributionLaw getSpeedDistributionLaw() { return speedDistributionLaw; }
    public void setSpeedDistributionLaw(DistributionLaw law) { this.speedDistributionLaw = law; }
    public double getSpeedUniformMinKmh() { return speedUniformMinKmh; }
    public void setSpeedUniformMinKmh(double min) { this.speedUniformMinKmh = min; }
    public double getSpeedUniformMaxKmh() { return speedUniformMaxKmh; }
    public void setSpeedUniformMaxKmh(double max) { this.speedUniformMaxKmh = max; }
    public double getSpeedNormalMeanKmh() { return speedNormalMeanKmh; }
    public void setSpeedNormalMeanKmh(double mean) { this.speedNormalMeanKmh = mean; }
    public double getSpeedNormalVarianceKmh() { return speedNormalVarianceKmh; }
    public void setSpeedNormalVarianceKmh(double variance) { this.speedNormalVarianceKmh = variance; }
    public double getSpeedExponentialIntensityPerKmh() { return speedExponentialIntensityPerKmh; }
    public void setSpeedExponentialIntensityPerKmh(double intensity) { this.speedExponentialIntensityPerKmh = intensity; }

    public double getSimulationTimeStep() { return simulationTimeStep; }
    public void setSimulationTimeStep(double step) { this.simulationTimeStep = step; }
    public double getSimulationSpeedFactor() { return simulationSpeedFactor; }
    public void setSimulationSpeedFactor(double factor) { this.simulationSpeedFactor = factor; }


    @Override
    public String toString() {
        return "SimulationParameters{" +
                "roadType=" + roadType +
                ", roadLengthKm=" + roadLengthKm +
                ", lanesPerDir=" + lanesPerDirection +
                ", numDirs=" + numberOfDirections +
                ", isRandomSpeed=" + isRandomSpeedFlow +
                ", isRandomTime=" + isRandomTimeFlow +
                ", detInt=" + deterministicIntervalSeconds +
                ", detSpd=" + deterministicSpeedKmh +
                '}';
    }
}
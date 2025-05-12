package com.trafficsimulation.model;

/**
 * Возможные состояния светофора.
 */
public enum TrafficLightState {
    RED,
    YELLOW, // Используем для цикла переключения, хотя в ТЗ реакции только на R/G
    GREEN
}
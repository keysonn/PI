package com.trafficsimulation.simulation; // или com.trafficsimulation.model

/**
 * Типы законов распределения случайных величин.
 * (ТЗ, Приложение, п. 1.15 - 3 закона)
 */
public enum DistributionLaw {
    UNIFORM,      // Равномерное
    NORMAL,       // Нормальное (Гауссово)
    EXPONENTIAL   // Показательное (Экспоненциальное)
}
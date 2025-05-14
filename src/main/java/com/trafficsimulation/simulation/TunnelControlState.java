package com.trafficsimulation.simulation;

public enum TunnelControlState {
    DIR0_GREEN,     // Направление 0 (слева-направо) имеет зеленый
    DIR0_CLEARING,  // Тоннель очищается от машин направления 0 (оба красные)
    DIR1_GREEN,     // Направление 1 (справа-налево) имеет зеленый
    DIR1_CLEARING   // Тоннель очищается от машин направления 1 (оба красные)
}
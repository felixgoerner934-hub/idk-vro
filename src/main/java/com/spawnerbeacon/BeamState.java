package com.spawnerbeacon;

public record BeamState(double x, double y, double z, double topY, float halfWidth,
                        float r, float g, float b, float a, float distanceFade, String type) {}

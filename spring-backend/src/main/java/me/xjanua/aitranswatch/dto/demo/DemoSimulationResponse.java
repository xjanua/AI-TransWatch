package me.xjanua.aitranswatch.dto.demo;

import me.xjanua.aitranswatch.dto.gps.GpsLogResponse;

public record DemoSimulationResponse(
        String scenario,
        String message,
        GpsLogResponse gpsLog,
        String expectedAlert) {
}

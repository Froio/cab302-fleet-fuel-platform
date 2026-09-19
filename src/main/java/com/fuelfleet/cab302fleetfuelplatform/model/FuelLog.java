package com.fuelfleet.cab302fleetfuelplatform.model;

import java.time.LocalDate;
import java.util.Objects;

public record FuelLog(
        int id,
        int vehicleId,
        String vehicleRegistration,
        LocalDate date,
        double litres,
        double cost,
        double odometer,
        String fuelType,
        boolean fullTank
) {
    public FuelLog {
        if (id <= 0) {
            throw new IllegalArgumentException("Fuel log ID must be positive");
        }
        if (vehicleId <= 0) {
            throw new IllegalArgumentException("Vehicle ID must be positive");
        }
        vehicleRegistration = required(vehicleRegistration, "Vehicle registration");
        date = Objects.requireNonNull(date, "Date is required");
        if (!Double.isFinite(litres) || litres <= 0) {
            throw new IllegalArgumentException("Litres must be a positive finite value");
        }
        if (!Double.isFinite(cost) || cost < 0) {
            throw new IllegalArgumentException("Cost must be a non-negative finite value");
        }
        if (!Double.isFinite(odometer) || odometer < 0) {
            throw new IllegalArgumentException("Odometer must be a non-negative finite value");
        }
        fuelType = required(fuelType, "Fuel type");
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }
}

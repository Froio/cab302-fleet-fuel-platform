package com.fuelfleet.cab302fleetfuelplatform.model;

import java.time.LocalDate;
import java.util.Objects;

/** A stored fill-up. Fuel type is a historical snapshot, not the current vehicle type. */
public record FuelRecord(int vehicleId, LocalDate date, double litres, double cost,
                         double odometer, String fuelType, boolean fullTank) {
    public FuelRecord {
        Objects.requireNonNull(date, "Date is required");
        if (vehicleId <= 0 || !Double.isFinite(litres) || litres <= 0
                || !Double.isFinite(cost) || cost < 0
                || !Double.isFinite(odometer) || odometer < 0) {
            throw new IllegalArgumentException("Invalid fuel record values");
        }
        fuelType = fuelType == null ? "Unknown" : fuelType.trim();
    }
}

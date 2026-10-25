package com.fuelfleet.cab302fleetfuelplatform.model;

import java.util.Objects;
import java.util.OptionalDouble;

public record FuelHistoryEntry(FuelLog log, OptionalDouble litresPer100Km, OptionalDouble costPerKm) {
    public FuelHistoryEntry(FuelLog log, OptionalDouble litresPer100Km) {
        this(log, litresPer100Km, OptionalDouble.empty());
    }

    public FuelHistoryEntry {
        log = Objects.requireNonNull(log, "Fuel log is required");
        litresPer100Km = Objects.requireNonNull(litresPer100Km, "Efficiency is required");
        costPerKm = Objects.requireNonNull(costPerKm, "Cost per kilometre is required");
    }
}

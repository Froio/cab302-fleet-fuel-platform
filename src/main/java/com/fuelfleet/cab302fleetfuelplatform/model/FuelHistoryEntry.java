package com.fuelfleet.cab302fleetfuelplatform.model;

import java.util.Objects;
import java.util.OptionalDouble;

public record FuelHistoryEntry(FuelLog log, OptionalDouble litresPer100Km) {
    public FuelHistoryEntry {
        log = Objects.requireNonNull(log, "Fuel log is required");
        litresPer100Km = Objects.requireNonNull(litresPer100Km, "Efficiency is required");
    }
}

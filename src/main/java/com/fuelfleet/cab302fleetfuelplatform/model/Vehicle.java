package com.fuelfleet.cab302fleetfuelplatform.model;

import java.util.Locale;

public record Vehicle(
        int id,
        String registration,
        String make,
        String model,
        String fuelType,
        long currentOdometer,
        Integer assignedDriverId,
        String assignedDriverUsername
) {
    public Vehicle {
        if (id <= 0) {
            throw new IllegalArgumentException("Vehicle ID must be positive");
        }
        registration = required(registration, "Registration").toUpperCase(Locale.ROOT);
        make = required(make, "Make");
        model = required(model, "Model");
        fuelType = required(fuelType, "Fuel type");
        if (currentOdometer < 0) {
            throw new IllegalArgumentException("Current odometer cannot be negative");
        }
        if (assignedDriverUsername != null) {
            assignedDriverUsername = assignedDriverUsername.trim();
        }
    }

    public String assignedDriverDisplay() {
        return assignedDriverUsername == null || assignedDriverUsername.isBlank()
                ? "Unassigned"
                : assignedDriverUsername;
    }

    @Override
    public String toString() {
        return registration + " - " + make + " " + model;
    }

    private static String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}

package com.fuelfleet.cab302fleetfuelplatform.model;

import java.util.Locale;

public enum Role {
    DRIVER("driver", "Driver"),
    MANAGER("manager", "Fleet Manager");

    private final String databaseValue;
    private final String displayName;

    Role(String databaseValue, String displayName) {
        this.databaseValue = databaseValue;
        this.displayName = displayName;
    }

    public String toDatabase() {
        return databaseValue;
    }

    public static Role fromDatabase(String value) {
        if (value == null) {
            throw new IllegalArgumentException("User role is missing");
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "driver" -> DRIVER;
            case "manager" -> MANAGER;
            default -> throw new IllegalArgumentException("Unknown user role: " + value);
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
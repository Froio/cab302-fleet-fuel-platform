package com.fuelfleet.cab302fleetfuelplatform.model;

import java.util.Objects;

public record User(int id, String username, Role role) {
    public User {
        if (id <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        username = username.trim();
        role = Objects.requireNonNull(role, "Role is required");
    }

    @Override
    public String toString() {
        return username;
    }
}
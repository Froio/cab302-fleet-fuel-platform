package com.fuelfleet.cab302fleetfuelplatform.service;

public interface MaintenancePolicy {
    MaintenanceStatus calculate(
            long currentOdometer,
            Long nextServiceOdometer
    );
}
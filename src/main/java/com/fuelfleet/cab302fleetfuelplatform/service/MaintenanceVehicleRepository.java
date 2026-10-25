package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;

import java.util.Optional;



public interface MaintenanceVehicleRepository {

    Optional<Vehicle> findById(int vehicleId);

    Optional<Long> findNextServiceOdometer (int vehicleId);

    boolean updateNextServiceOdometer(
            int vehicleId,
            Long nextServiceOdometer
    );
}

package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;

import java.util.Objects;
import java.util.Optional;




public final class DaoMaintenanceVehicleRepository
        implements  MaintenanceVehicleRepository {

    private final VehicleDao vehicleDao;

    public DaoMaintenanceVehicleRepository(VehicleDao vehicleDao) {
        this.vehicleDao = Objects.requireNonNull(
                vehicleDao,
                "Vehicle DAO cannot be null"
        );
    }
    @Override
    public Optional<Vehicle> findById (int vehicleId) {
        return vehicleDao.findById(vehicleId);
    }

    @Override
    public Optional<Long> findNextServiceOdometer(int vehicleId) {
        return vehicleDao.findNextServiceOdometer(vehicleId);
    }

    @Override
    public boolean updateNextServiceOdometer(
            int vehicleId,
            Long nextServiceOdometer
    ) {
        return vehicleDao.updateNextServiceOdometer(
                vehicleId,
                nextServiceOdometer
        );
    }
}

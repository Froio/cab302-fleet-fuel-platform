package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

import java.util.List;

public final class VehicleAssignmentService {
    private final VehicleDao vehicleDao;
    private final UserDao userDao;
    private final AppSession session;

    public VehicleAssignmentService() {
        this(new VehicleDao(), new UserDao(), AppSession.getInstance());
    }

    public VehicleAssignmentService(VehicleDao vehicleDao, UserDao userDao, AppSession session) {
        this.vehicleDao = vehicleDao;
        this.userDao = userDao;
        this.session = session;
    }

    public List<User> listDrivers() {
        requireManager();
        return userDao.listByRole(Role.DRIVER);
    }

    public void assignDriver(int vehicleId, int driverId) {
        requireManager();
        requireVehicle(vehicleId);
        User driver = userDao.findById(driverId)
                .orElseThrow(() -> new ValidationException("The selected driver no longer exists."));
        if (driver.role() != Role.DRIVER) {
            throw new ValidationException("Only a Driver account can be assigned to a vehicle.");
        }
        if (!vehicleDao.assignDriver(vehicleId, driverId)) {
            throw new ValidationException("The selected vehicle no longer exists.");
        }
    }

    public void unassignDriver(int vehicleId) {
        requireManager();
        requireVehicle(vehicleId);
        if (!vehicleDao.assignDriver(vehicleId, null)) {
            throw new ValidationException("The selected vehicle no longer exists.");
        }
    }

    private void requireVehicle(int vehicleId) {
        if (vehicleDao.findById(vehicleId).isEmpty()) {
            throw new ValidationException("The selected vehicle no longer exists.");
        }
    }

    private void requireManager() {
        if (!session.isManager()) {
            throw new AuthorizationException("Fleet manager access is required.");
        }
    }
}

package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

import java.util.List;

/** Live account/assignment data for the manager's supporting screens. */
public final class FleetDirectoryService {
    private final UserDao users;
    private final VehicleDao vehicles;
    private final AppSession session;

    public FleetDirectoryService() {
        this(new UserDao(), new VehicleDao(), AppSession.getInstance());
    }

    public FleetDirectoryService(UserDao users, VehicleDao vehicles, AppSession session) {
        this.users = users;
        this.vehicles = vehicles;
        this.session = session;
    }

    public List<DriverAssignment> listDrivers() {
        requireManager();
        List<Vehicle> fleet = vehicles.listAll();
        return users.listByRole(Role.DRIVER).stream()
                .map(driver -> new DriverAssignment(driver, fleet.stream()
                        .filter(vehicle -> vehicle.assignedDriverId() != null && vehicle.assignedDriverId() == driver.id())
                        .toList()))
                .toList();
    }

    public List<Vehicle> listVehicles(boolean unassignedOnly) {
        requireManager();
        return vehicles.listAll().stream()
                .filter(vehicle -> !unassignedOnly || vehicle.assignedDriverId() == null)
                .toList();
    }

    private void requireManager() {
        if (!session.isManager()) throw new AuthorizationException("Fleet manager access is required.");
    }

    public record DriverAssignment(User driver, List<Vehicle> vehicles) {
        public DriverAssignment {
            vehicles = List.copyOf(vehicles);
        }
    }
}

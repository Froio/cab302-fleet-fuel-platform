package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DuplicateRegistrationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

import java.util.List;
import java.util.Locale;

public final class VehicleService {
    private final VehicleDao vehicleDao;
    private final AppSession session;

    public VehicleService() {
        this(new VehicleDao(), AppSession.getInstance());
    }

    public VehicleService(VehicleDao vehicleDao, AppSession session) {
        this.vehicleDao = vehicleDao;
        this.session = session;
    }

    public List<Vehicle> listVehicles() {
        requireManager();
        return vehicleDao.listAll();
    }

    public List<Vehicle> listVehiclesForCurrentDriver() {
        User currentUser = session.currentUser().orElseThrow(
                () -> new AuthorizationException("Please sign in as a driver."));
        if (currentUser.role() != Role.DRIVER) {
            throw new AuthorizationException("Driver access is required.");
        }
        return vehicleDao.listByDriverId(currentUser.id());
    }

    public Vehicle createVehicle(
            String registration,
            String make,
            String model,
            String fuelType,
            String currentOdometer
    ) {
        requireManager();
        String normalizedRegistration = requireText(registration, "Registration is required.")
                .toUpperCase(Locale.ROOT);
        String normalizedMake = requireText(make, "Make is required.");
        String normalizedModel = requireText(model, "Model is required.");
        String normalizedFuelType = requireText(fuelType, "Fuel type is required.");
        long odometer = parseOdometer(currentOdometer);

        if (vehicleDao.existsByRegistration(normalizedRegistration)) {
            throw new DuplicateRegistrationException();
        }

        try {
            return vehicleDao.insert(normalizedRegistration, normalizedMake, normalizedModel,
                    normalizedFuelType, odometer);
        } catch (DataAccessException exception) {
            if (exception.isConstraintViolation()) {
                throw new DuplicateRegistrationException();
            }
            throw exception;
        }
    }

    public Vehicle updateVehicle(
            int vehicleId,
            String registration,
            String make,
            String model,
            String fuelType,
            String currentOdometer
    ) {
        requireManager();
        String normalizedRegistration = requireText(registration, "Registration is required.")
                .toUpperCase(Locale.ROOT);
        String normalizedMake = requireText(make, "Make is required.");
        String normalizedModel = requireText(model, "Model is required.");
        String normalizedFuelType = requireText(fuelType, "Fuel type is required.");
        long odometer = parseOdometer(currentOdometer);

        if (vehicleDao.findById(vehicleId).isEmpty()) {
            throw new ValidationException("The selected vehicle no longer exists.");
        }
        if (vehicleDao.existsByRegistrationExcludingId(normalizedRegistration, vehicleId)) {
            throw new DuplicateRegistrationException();
        }

        try {
            if (!vehicleDao.update(vehicleId, normalizedRegistration, normalizedMake,
                    normalizedModel, normalizedFuelType, odometer)) {
                throw new ValidationException("The selected vehicle no longer exists.");
            }
        } catch (DataAccessException exception) {
            if (exception.isConstraintViolation()) {
                throw new DuplicateRegistrationException();
            }
            throw exception;
        }
        return vehicleDao.findById(vehicleId).orElseThrow(
                () -> new ValidationException("The selected vehicle no longer exists."));
    }

    public void deleteVehicle(int vehicleId) {
        requireManager();
        if (vehicleDao.findById(vehicleId).isEmpty() || !vehicleDao.delete(vehicleId)) {
            throw new ValidationException("The selected vehicle no longer exists.");
        }
    }

    private void requireManager() {
        if (!session.isManager()) {
            throw new AuthorizationException("Fleet manager access is required.");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private static long parseOdometer(String value) {
        String normalized = requireText(value, "Current odometer is required.");
        try {
            long odometer = Long.parseLong(normalized);
            if (odometer < 0) {
                throw new ValidationException("Current odometer cannot be negative.");
            }
            return odometer;
        } catch (NumberFormatException exception) {
            throw new ValidationException("Current odometer must be a non-negative whole number.");
        }
    }
}

package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.FuelLogDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.FuelHistoryEntry;
import com.fuelfleet.cab302fleetfuelplatform.model.FuelLog;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;

public final class FuelLogService {
    private final FuelLogDao fuelLogDao;
    private final VehicleDao vehicleDao;
    private final AppSession session;

    public FuelLogService() {
        this(new FuelLogDao(), new VehicleDao(), AppSession.getInstance());
    }

    public FuelLogService(FuelLogDao fuelLogDao, VehicleDao vehicleDao, AppSession session) {
        this.fuelLogDao = Objects.requireNonNull(fuelLogDao, "Fuel log DAO is required");
        this.vehicleDao = Objects.requireNonNull(vehicleDao, "Vehicle DAO is required");
        this.session = Objects.requireNonNull(session, "Session is required");
    }

    public List<Vehicle> listAssignedVehicles() {
        User driver = requireDriver();
        return List.copyOf(vehicleDao.listByDriverId(driver.id()));
    }

    public List<FuelHistoryEntry> listHistory() {
        User driver = requireDriver();
        List<Integer> vehicleIds = vehicleDao.listByDriverId(driver.id()).stream()
                .map(Vehicle::id)
                .toList();
        List<FuelLog> logs = fuelLogDao.listByVehicleIds(vehicleIds);
        Map<Integer, TankInterval> intervals = new HashMap<>();
        List<FuelHistoryEntry> history = new ArrayList<>(logs.size());
        for (FuelLog log : logs) {
            TankInterval interval = intervals.computeIfAbsent(log.vehicleId(), ignored -> new TankInterval());
            OptionalDouble efficiency = interval.accept(log);
            history.add(new FuelHistoryEntry(log, efficiency));
        }
        Collections.reverse(history);
        return List.copyOf(history);
    }

    public FuelLog save(
            int vehicleId,
            LocalDate date,
            String litresInput,
            String pricePerLitreInput,
            String odometerInput,
            boolean fullTank
    ) {
        User driver = requireDriver();
        Vehicle vehicle = vehicleDao.findById(vehicleId)
                .filter(candidate -> Objects.equals(candidate.assignedDriverId(), driver.id()))
                .orElseThrow(() -> new AuthorizationException(
                        "The selected vehicle is not assigned to the signed-in driver."));
        LocalDate validatedDate = validateDate(date);
        double litres = parsePositiveDouble(litresInput, "Litres");
        double pricePerLitre = parseNonNegativeDouble(pricePerLitreInput, "Price per litre");
        long odometer = parseOdometer(odometerInput);

        OptionalDouble previousOdometer = fuelLogDao.findLatestOdometer(vehicleId);
        if (previousOdometer.isPresent() && odometer <= previousOdometer.getAsDouble()) {
            throw new ValidationException("Odometer must be greater than the latest fuel log reading.");
        }
        if (previousOdometer.isEmpty() && odometer < vehicle.currentOdometer()) {
            throw new ValidationException("Odometer cannot be below the vehicle's current reading.");
        }

        double cost;
        try {
            cost = FuelCalculations.costPerFillUp(litres, pricePerLitre);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(exception.getMessage());
        }
        return fuelLogDao.save(vehicleId, validatedDate, litres, cost, odometer,
                vehicle.fuelType(), fullTank);
    }

    private User requireDriver() {
        User currentUser = session.currentUser().orElseThrow(
                () -> new AuthorizationException("Please sign in as a driver."));
        if (currentUser.role() != Role.DRIVER) {
            throw new AuthorizationException("Driver access is required.");
        }
        return currentUser;
    }

    private static LocalDate validateDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException("Date is required.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("Date cannot be in the future.");
        }
        return date;
    }

    private static double parsePositiveDouble(String input, String name) {
        double value = parseFiniteDouble(input, name);
        if (value <= 0) {
            throw new ValidationException(name + " must be positive.");
        }
        return value;
    }

    private static double parseNonNegativeDouble(String input, String name) {
        double value = parseFiniteDouble(input, name);
        if (value < 0) {
            throw new ValidationException(name + " cannot be negative.");
        }
        return value;
    }

    private static double parseFiniteDouble(String input, String name) {
        String normalized = requireInput(input, name);
        try {
            double value = Double.parseDouble(normalized);
            if (!Double.isFinite(value)) {
                throw new ValidationException(name + " must be finite.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new ValidationException(name + " must be a number.");
        }
    }

    private static long parseOdometer(String input) {
        String normalized = requireInput(input, "Odometer");
        try {
            long odometer = Long.parseLong(normalized);
            if (odometer < 0) {
                throw new ValidationException("Odometer cannot be negative.");
            }
            return odometer;
        } catch (NumberFormatException exception) {
            throw new ValidationException("Odometer must be a non-negative whole number.");
        }
    }

    private static String requireInput(String input, String name) {
        if (input == null || input.isBlank()) {
            throw new ValidationException(name + " is required.");
        }
        return input.trim();
    }

    private static final class TankInterval {
        private Double baselineOdometer;
        private Double lastOdometer;
        private double litres;

        private OptionalDouble accept(FuelLog log) {
            if (lastOdometer != null && log.odometer() <= lastOdometer) {
                baselineOdometer = null;
                litres = 0;
            }
            lastOdometer = log.odometer();
            if (baselineOdometer != null) {
                litres += log.litres();
            }

            OptionalDouble efficiency = OptionalDouble.empty();
            if (log.fullTank()) {
                if (baselineOdometer != null) {
                    efficiency = OptionalDouble.of(FuelCalculations.litresPer100Km(
                            litres, baselineOdometer, log.odometer()));
                }
                baselineOdometer = log.odometer();
                litres = 0;
            }
            return efficiency;
        }
    }
}

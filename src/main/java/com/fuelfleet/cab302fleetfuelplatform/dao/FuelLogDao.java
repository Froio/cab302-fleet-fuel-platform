package com.fuelfleet.cab302fleetfuelplatform.dao;

import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.FuelLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.StringJoiner;

public final class FuelLogDao {
    private static final String SELECT_LOG = """
            SELECT f.id, f.vehicle_id, v.registration, f.date, f.litres, f.cost,
                   f.odometer, f.fuel_type, f.full_tank
            FROM fuel_logs f
            JOIN vehicles v ON v.id = f.vehicle_id
            """;

    private final ConnectionProvider connectionProvider;

    public FuelLogDao() {
        this(DBManager::getConnection);
    }

    public FuelLogDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider, "Connection provider is required");
    }

    public List<FuelLog> listByVehicleIds(Collection<Integer> vehicleIds) {
        return loadHistory(vehicleIds).logs();
    }

    public HistoryLoad loadHistory(Collection<Integer> vehicleIds) {
        Objects.requireNonNull(vehicleIds, "Vehicle IDs are required");
        if (vehicleIds.isEmpty()) {
            return new HistoryLoad(List.of(), 0, Set.of());
        }
        StringJoiner placeholders = new StringJoiner(", ");
        for (Integer ignored : vehicleIds) {
            placeholders.add("?");
        }
        String sql = SELECT_LOG + " WHERE f.vehicle_id IN (" + placeholders + ")"
                + " ORDER BY f.date, f.odometer, f.id";
        List<FuelLog> logs = new ArrayList<>();
        Set<Integer> incompleteVehicles = new HashSet<>();
        Map<Integer, Double> lastOdometers = new HashMap<>();
        int skippedRows = 0;
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int position = 1;
            for (Integer vehicleId : vehicleIds) {
                if (vehicleId == null || vehicleId <= 0) {
                    throw new ValidationException("Vehicle ID must be positive");
                }
                statement.setInt(position++, vehicleId);
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    int vehicleId = rows.getInt("vehicle_id");
                    try {
                        FuelLog log = mapFuelLog(rows);
                        Double previous = lastOdometers.get(vehicleId);
                        if (previous != null && log.odometer() <= previous) {
                            throw new IllegalArgumentException("Fuel history odometers contradict date order");
                        }
                        lastOdometers.put(vehicleId, log.odometer());
                        logs.add(log);
                    } catch (IllegalArgumentException | java.time.DateTimeException | NullPointerException exception) {
                        skippedRows++;
                        incompleteVehicles.add(vehicleId);
                    }
                }
            }
            return new HistoryLoad(logs, skippedRows, incompleteVehicles);
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to list fuel logs", exception);
        }
    }

    public OptionalDouble findLatestOdometer(int vehicleId) {
        if (vehicleId <= 0) {
            throw new ValidationException("Vehicle ID must be positive");
        }
        try (Connection connection = connectionProvider.open()) {
            return findLatestOdometer(connection, vehicleId);
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load latest fuel odometer", exception);
        }
    }

    public FuelLog save(
            int vehicleId,
            LocalDate date,
            double litres,
            double cost,
            long odometer,
            String fuelType,
            boolean fullTank
    ) {
        return save(vehicleId, date, litres, cost, odometer, fuelType, fullTank, null);
    }

    public FuelLog save(int vehicleId, LocalDate date, double litres, double cost, long odometer,
                        String fuelType, boolean fullTank, Integer createdByUserId) {
        try (Connection connection = connectionProvider.open()) {
            boolean autoCommit = connection.getAutoCommit();
            RuntimeException primaryFailure = null;
            try {
                connection.setAutoCommit(false);
                VehicleSnapshot vehicle = loadVehicle(connection, vehicleId);
                if (createdByUserId != null && !Objects.equals(createdByUserId, vehicle.assignedDriverId())) {
                    throw new AuthorizationException("The selected vehicle is no longer assigned to the signed-in driver.");
                }
                validateInput(vehicleId, date, litres, cost, odometer, fuelType);
                OptionalDouble previousOdometer = findLatestOdometer(connection, vehicleId);
                if (previousOdometer.isPresent() && odometer <= previousOdometer.getAsDouble()) {
                    throw new ValidationException("Odometer must be greater than the latest fuel log reading");
                }
                if (odometer < vehicle.currentOdometer()) {
                    throw new ValidationException("Odometer cannot be below the vehicle's current reading");
                }
                validateAppendDate(connection, vehicleId, date);
                int logId = insertFuelLog(connection, vehicleId, date, litres, cost, odometer,
                        fuelType, fullTank, createdByUserId);
                updateVehicleOdometer(connection, vehicleId, odometer);
                FuelLog log = new FuelLog(logId, vehicleId, vehicle.registration(), date, litres, cost,
                        odometer, fuelType, fullTank);
                connection.commit();
                return log;
            } catch (SQLException exception) {
                rollback(connection, exception);
                primaryFailure = new DataAccessException("Unable to save fuel log", exception);
                throw primaryFailure;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                primaryFailure = exception;
                throw exception;
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException restorationFailure) {
                    if (primaryFailure != null) {
                        primaryFailure.addSuppressed(restorationFailure);
                    } else {
                        throw restorationFailure;
                    }
                }
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to save fuel log", exception);
        }
    }

    public void delete(int logId, int driverId) {
        if (logId <= 0) throw new ValidationException("Select a fuel log to delete.");
        try (Connection connection = connectionProvider.open()) {
            boolean autoCommit = connection.getAutoCommit();
            RuntimeException primaryFailure = null;
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT v.assigned_driver_id, f.created_by_user_id
                        FROM fuel_logs f JOIN vehicles v ON v.id = f.vehicle_id WHERE f.id = ?
                        """)) {
                    statement.setInt(1, logId);
                    try (ResultSet row = statement.executeQuery()) {
                        if (!row.next()) throw new ValidationException("The selected fuel log no longer exists.");
                        int assigned = row.getInt("assigned_driver_id");
                        if (row.wasNull() || assigned != driverId) throw new AuthorizationException(
                                "Only the currently assigned driver may delete a fuel log.");
                        int author = row.getInt("created_by_user_id");
                        if (row.wasNull()) throw new AuthorizationException(
                                "This legacy log has no recorded author and cannot be deleted by a driver.");
                        if (author != driverId) throw new AuthorizationException(
                                "Only the driver who recorded this fuel log may delete it.");
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        DELETE FROM fuel_logs WHERE id = ? AND created_by_user_id = ?
                        AND EXISTS (SELECT 1 FROM vehicles v
                                    WHERE v.id = fuel_logs.vehicle_id AND v.assigned_driver_id = ?)
                        """)) {
                    statement.setInt(1, logId);
                    statement.setInt(2, driverId);
                    statement.setInt(3, driverId);
                    if (statement.executeUpdate() != 1) throw new AuthorizationException(
                            "Fuel log ownership or vehicle assignment changed; refresh and try again.");
                }
                // Removing an entry corrects history; it cannot undo distance already travelled.
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                primaryFailure = new DataAccessException("Unable to delete fuel log", exception);
                throw primaryFailure;
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                primaryFailure = exception;
                throw exception;
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException restorationFailure) {
                    if (primaryFailure != null) primaryFailure.addSuppressed(restorationFailure);
                    else throw restorationFailure;
                }
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to delete fuel log", exception);
        }
    }

    private static VehicleSnapshot loadVehicle(Connection connection, int vehicleId) throws SQLException {
        if (vehicleId <= 0) {
            throw new ValidationException("Vehicle ID must be positive");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT registration, current_odometer, assigned_driver_id FROM vehicles WHERE id = ?")) {
            statement.setInt(1, vehicleId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new ValidationException("Vehicle does not exist");
                }
                int assignedId = rows.getInt("assigned_driver_id");
                Integer assignedDriverId = rows.wasNull() ? null : assignedId;
                return new VehicleSnapshot(rows.getString("registration"), rows.getLong("current_odometer"), assignedDriverId);
            }
        }
    }

    private static OptionalDouble findLatestOdometer(Connection connection, int vehicleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT MAX(odometer) AS odometer FROM fuel_logs WHERE vehicle_id = ? "
                        + "AND typeof(odometer) IN ('integer', 'real') "
                        + "AND odometer >= 0 AND odometer <= 1.7976931348623157e308")) {
            statement.setInt(1, vehicleId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return OptionalDouble.empty();
                }
                double odometer = rows.getDouble("odometer");
                return rows.wasNull() ? OptionalDouble.empty() : OptionalDouble.of(odometer);
            }
        }
    }

    private static void validateAppendDate(Connection connection, int vehicleId, LocalDate date) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT date FROM fuel_logs WHERE vehicle_id = ?")) {
            statement.setInt(1, vehicleId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    LocalDate existing;
                    try {
                        existing = LocalDate.parse(rows.getString("date"));
                    } catch (java.time.DateTimeException | NullPointerException exception) {
                        throw new ValidationException("Existing fuel history contains an invalid date; ask a manager to correct it.");
                    }
                    if (date.isBefore(existing)) {
                        throw new ValidationException("Date cannot be earlier than the latest fuel log date.");
                    }
                }
            }
        }
    }

    private static int insertFuelLog(
            Connection connection,
            int vehicleId,
            LocalDate date,
            double litres,
            double cost,
            long odometer,
            String fuelType,
            boolean fullTank,
            Integer createdByUserId
    ) throws SQLException {
        String sql = """
                INSERT INTO fuel_logs(vehicle_id, date, litres, cost, odometer, fuel_type, full_tank, created_by_user_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, vehicleId);
            statement.setString(2, date.toString());
            statement.setDouble(3, litres);
            statement.setDouble(4, cost);
            statement.setLong(5, odometer);
            statement.setString(6, fuelType.trim());
            statement.setBoolean(7, fullTank);
            if (createdByUserId == null) statement.setNull(8, java.sql.Types.INTEGER);
            else statement.setInt(8, createdByUserId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return a fuel log ID");
                }
                return keys.getInt(1);
            }
        }
    }

    private static void updateVehicleOdometer(Connection connection, int vehicleId, long odometer) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE vehicles SET current_odometer = ? WHERE id = ?")) {
            statement.setLong(1, odometer);
            statement.setInt(2, vehicleId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Vehicle odometer update did not affect one row");
            }
        }
    }

    private static void validateInput(int vehicleId, LocalDate date, double litres, double cost, long odometer, String fuelType) {
        if (vehicleId <= 0) {
            throw new ValidationException("Vehicle ID must be positive");
        }
        if (date == null) {
            throw new ValidationException("Date is required");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException("Date cannot be in the future");
        }
        if (!Double.isFinite(litres) || litres <= 0) {
            throw new ValidationException("Litres must be a positive finite value");
        }
        if (!Double.isFinite(cost) || cost < 0) {
            throw new ValidationException("Cost must be a non-negative finite value");
        }
        if (odometer < 0) {
            throw new ValidationException("Odometer must be non-negative");
        }
        if (fuelType == null || fuelType.isBlank()) {
            throw new ValidationException("Fuel type is required");
        }
    }

    private static FuelLog mapFuelLog(ResultSet rows) throws SQLException {
        return new FuelLog(
                rows.getInt("id"),
                rows.getInt("vehicle_id"),
                rows.getString("registration"),
                LocalDate.parse(rows.getString("date")),
                requiredNumber(rows, "litres"),
                requiredNumber(rows, "cost"),
                requiredNumber(rows, "odometer"),
                rows.getString("fuel_type"),
                fullTank(rows)
        );
    }

    private static double requiredNumber(ResultSet rows, String name) throws SQLException {
        Object value = rows.getObject(name);
        if (value == null) throw new IllegalArgumentException("Missing " + name);
        return Double.parseDouble(value.toString());
    }

    private static boolean fullTank(ResultSet rows) throws SQLException {
        double value = requiredNumber(rows, "full_tank");
        if (value != 0 && value != 1) throw new IllegalArgumentException("Invalid full tank flag");
        return value == 1;
    }

    public record HistoryLoad(List<FuelLog> logs, int skippedRows, Set<Integer> incompleteVehicleIds) {
        public HistoryLoad {
            logs = List.copyOf(logs);
            incompleteVehicleIds = Set.copyOf(incompleteVehicleIds);
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private record VehicleSnapshot(String registration, long currentOdometer, Integer assignedDriverId) {
    }
}

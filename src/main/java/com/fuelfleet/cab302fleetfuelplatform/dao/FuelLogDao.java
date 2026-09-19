package com.fuelfleet.cab302fleetfuelplatform.dao;

import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
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
        Objects.requireNonNull(vehicleIds, "Vehicle IDs are required");
        if (vehicleIds.isEmpty()) {
            return List.of();
        }
        StringJoiner placeholders = new StringJoiner(", ");
        for (Integer ignored : vehicleIds) {
            placeholders.add("?");
        }
        String sql = SELECT_LOG + " WHERE f.vehicle_id IN (" + placeholders + ")"
                + " ORDER BY f.date, f.odometer, f.id";
        List<FuelLog> logs = new ArrayList<>();
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
                    logs.add(mapFuelLog(rows));
                }
            }
            return List.copyOf(logs);
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
        try (Connection connection = connectionProvider.open()) {
            boolean autoCommit = connection.getAutoCommit();
            RuntimeException primaryFailure = null;
            try {
                connection.setAutoCommit(false);
                VehicleSnapshot vehicle = loadVehicle(connection, vehicleId);
                validateInput(vehicleId, date, litres, cost, odometer, fuelType);
                OptionalDouble previousOdometer = findLatestOdometer(connection, vehicleId);
                if (previousOdometer.isPresent() && odometer <= previousOdometer.getAsDouble()) {
                    throw new ValidationException("Odometer must be greater than the latest fuel log reading");
                }
                if (previousOdometer.isEmpty() && odometer < vehicle.currentOdometer()) {
                    throw new ValidationException("Odometer cannot be below the vehicle's current reading");
                }
                int logId = insertFuelLog(connection, vehicleId, date, litres, cost, odometer, fuelType, fullTank);
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

    private static VehicleSnapshot loadVehicle(Connection connection, int vehicleId) throws SQLException {
        if (vehicleId <= 0) {
            throw new ValidationException("Vehicle ID must be positive");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT registration, current_odometer FROM vehicles WHERE id = ?")) {
            statement.setInt(1, vehicleId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new ValidationException("Vehicle does not exist");
                }
                return new VehicleSnapshot(rows.getString("registration"), rows.getLong("current_odometer"));
            }
        }
    }

    private static OptionalDouble findLatestOdometer(Connection connection, int vehicleId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT MAX(odometer) AS odometer FROM fuel_logs WHERE vehicle_id = ?")) {
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

    private static int insertFuelLog(
            Connection connection,
            int vehicleId,
            LocalDate date,
            double litres,
            double cost,
            long odometer,
            String fuelType,
            boolean fullTank
    ) throws SQLException {
        String sql = """
                INSERT INTO fuel_logs(vehicle_id, date, litres, cost, odometer, fuel_type, full_tank)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, vehicleId);
            statement.setString(2, date.toString());
            statement.setDouble(3, litres);
            statement.setDouble(4, cost);
            statement.setLong(5, odometer);
            statement.setString(6, fuelType.trim());
            statement.setBoolean(7, fullTank);
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
                rows.getDouble("litres"),
                rows.getDouble("cost"),
                rows.getDouble("odometer"),
                rows.getString("fuel_type"),
                rows.getBoolean("full_tank")
        );
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private record VehicleSnapshot(String registration, long currentOdometer) {
    }
}

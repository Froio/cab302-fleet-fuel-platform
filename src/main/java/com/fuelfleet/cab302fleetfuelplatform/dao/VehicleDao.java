package com.fuelfleet.cab302fleetfuelplatform.dao;

import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleDao {
    private static final String SELECT_VEHICLE = """
            SELECT v.id, v.registration, v.make, v.model, v.fuel_type,
                   v.current_odometer, v.assigned_driver_id,
                   u.username AS assigned_driver_username
            FROM vehicles v
            LEFT JOIN users u ON u.id = v.assigned_driver_id
            """;

    private final ConnectionProvider connectionProvider;

    public VehicleDao() {
        this(DBManager::getConnection);
    }

    public VehicleDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public List<Vehicle> listAll() {
        List<Vehicle> vehicles = new ArrayList<>();
        try (Connection connection = connectionProvider.open();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(SELECT_VEHICLE + " ORDER BY v.registration COLLATE NOCASE")) {
            while (rows.next()) {
                vehicles.add(mapVehicle(rows));
            }
            return vehicles;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to list vehicles", exception);
        }
    }

    public Optional<Vehicle> findById(int vehicleId) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(SELECT_VEHICLE + " WHERE v.id = ?")) {
            statement.setInt(1, vehicleId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapVehicle(rows)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load vehicle", exception);
        }
    }

    public boolean existsByRegistration(String registration) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM vehicles WHERE registration = ? COLLATE NOCASE")) {
            statement.setString(1, registration);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to check vehicle registration", exception);
        }
    }

    public boolean existsByRegistrationExcludingId(String registration, int vehicleId) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM vehicles WHERE registration = ? COLLATE NOCASE AND id <> ?")) {
            statement.setString(1, registration);
            statement.setInt(2, vehicleId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to check vehicle registration", exception);
        }
    }

    public Vehicle insert(String registration, String make, String model, String fuelType, long currentOdometer) {
        String sql = """
                INSERT INTO vehicles (registration, make, model, fuel_type, current_odometer)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, registration);
            statement.setString(2, make);
            statement.setString(3, model);
            statement.setString(4, fuelType);
            statement.setLong(5, currentOdometer);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Database did not return a vehicle ID");
                }
                return new Vehicle(keys.getInt(1), registration, make, model, fuelType,
                        currentOdometer, null, null);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to create vehicle", exception);
        }
    }

    public boolean update(
            int vehicleId,
            String registration,
            String make,
            String model,
            String fuelType,
            long currentOdometer
    ) {
        String sql = """
                UPDATE vehicles
                SET registration = ?, make = ?, model = ?, fuel_type = ?, current_odometer = ?
                WHERE id = ?
                """;
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, registration);
            statement.setString(2, make);
            statement.setString(3, model);
            statement.setString(4, fuelType);
            statement.setLong(5, currentOdometer);
            statement.setInt(6, vehicleId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update vehicle", exception);
        }
    }

    public boolean delete(int vehicleId) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM vehicles WHERE id = ?")) {
            statement.setInt(1, vehicleId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to delete vehicle", exception);
        }
    }

    public boolean assignDriver(int vehicleId, Integer driverId) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE vehicles SET assigned_driver_id = ? WHERE id = ?")) {
            if (driverId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, driverId);
            }
            statement.setInt(2, vehicleId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update vehicle assignment", exception);
        }
    }

    public List<Vehicle> listByDriverId(int driverId) {
        List<Vehicle> vehicles = new ArrayList<>();
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_VEHICLE + " WHERE v.assigned_driver_id = ? ORDER BY v.registration COLLATE NOCASE")) {
            statement.setInt(1, driverId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    vehicles.add(mapVehicle(rows));
                }
            }
            return vehicles;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to list assigned vehicles", exception);
        }
    }

    private static Vehicle mapVehicle(ResultSet rows) throws SQLException {
        int driverId = rows.getInt("assigned_driver_id");
        Integer assignedDriverId = rows.wasNull() ? null : driverId;
        return new Vehicle(
                rows.getInt("id"),
                rows.getString("registration"),
                rows.getString("make"),
                rows.getString("model"),
                rows.getString("fuel_type"),
                rows.getLong("current_odometer"),
                assignedDriverId,
                rows.getString("assigned_driver_username")
        );
    }
}

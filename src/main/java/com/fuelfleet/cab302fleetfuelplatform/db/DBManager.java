package com.fuelfleet.cab302fleetfuelplatform.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public final class DBManager {
    private static final Path DB_DIRECTORY = Path.of("data");
    private static final String DEFAULT_URL = "jdbc:sqlite:data/fleet.db";

    private DBManager() {
    }

    public static Connection getConnection() throws SQLException {
        return provider(DEFAULT_URL).open();
    }

    public static ConnectionProvider provider(String jdbcUrl) {
        return () -> {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        };
    }

    public static void initialize() {
        try {
            Files.createDirectories(DB_DIRECTORY);
            initialize(DBManager::getConnection);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize the fleet database", exception);
        }
    }

    public static void initialize(ConnectionProvider connectionProvider) throws SQLException {
        try (Connection connection = connectionProvider.open()) {
            connection.setAutoCommit(false);
            try {
                createBaseTables(connection);
                migrateVehicleColumns(connection);
                createFuelLogs(connection);
                createUniqueIndexes(connection);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static void createBaseTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL,
                        password TEXT NOT NULL,
                        role TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS vehicles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        registration TEXT NOT NULL,
                        make TEXT NOT NULL,
                        model TEXT NOT NULL,
                        fuel_type TEXT NOT NULL DEFAULT 'Unknown',
                        current_odometer INTEGER NOT NULL DEFAULT 0 CHECK (current_odometer >= 0),
                        assigned_driver_id INTEGER REFERENCES users(id) ON DELETE SET NULL
                    )
                    """);
        }
    }

    private static void createFuelLogs(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS fuel_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        vehicle_id INTEGER NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
                        date TEXT NOT NULL,
                        litres REAL NOT NULL CHECK (litres > 0),
                        cost REAL NOT NULL CHECK (cost >= 0),
                        odometer REAL NOT NULL CHECK (odometer >= 0),
                        fuel_type TEXT NOT NULL DEFAULT 'Unknown',
                        full_tank INTEGER NOT NULL DEFAULT 0 CHECK (full_tank IN (0,1))
                    )
                    """);
            Set<String> columns = tableColumns(connection, "fuel_logs");
            if (!columns.contains("fuel_type"))
                statement.executeUpdate("ALTER TABLE fuel_logs ADD COLUMN fuel_type TEXT NOT NULL DEFAULT 'Unknown'");
            if (!columns.contains("full_tank"))
                statement.executeUpdate("ALTER TABLE fuel_logs ADD COLUMN full_tank INTEGER NOT NULL DEFAULT 0 CHECK (full_tank IN (0,1))");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS ix_fuel_logs_vehicle_date ON fuel_logs(vehicle_id, date)");
        }
    }

    private static void migrateVehicleColumns(Connection connection) throws SQLException {
        Set<String> columns = tableColumns(connection, "vehicles");
        try (Statement statement = connection.createStatement()) {
            if (!columns.contains("fuel_type")) {
                statement.executeUpdate("ALTER TABLE vehicles ADD COLUMN fuel_type TEXT NOT NULL DEFAULT 'Unknown'");
            }
            if (!columns.contains("current_odometer")) {
                statement.executeUpdate("ALTER TABLE vehicles ADD COLUMN current_odometer INTEGER NOT NULL DEFAULT 0 CHECK (current_odometer >= 0)");
            }
            if (!columns.contains("assigned_driver_id")) {
                statement.executeUpdate("ALTER TABLE vehicles ADD COLUMN assigned_driver_id INTEGER REFERENCES users(id) ON DELETE SET NULL");
            }
            statement.executeUpdate("UPDATE vehicles SET fuel_type = 'Unknown' WHERE fuel_type IS NULL OR TRIM(fuel_type) = ''");
            statement.executeUpdate("UPDATE vehicles SET current_odometer = 0 WHERE current_odometer IS NULL OR current_odometer < 0");
            statement.executeUpdate("UPDATE users SET role = 'driver' WHERE role IS NULL OR TRIM(role) = ''");
        }
    }

    private static Set<String> tableColumns(Connection connection, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rows.next()) {
                columns.add(rows.getString("name"));
            }
        }
        return columns;
    }

    private static void createUniqueIndexes(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username_nocase "
                    + "ON users(username COLLATE NOCASE)");
            statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS ux_vehicles_registration_nocase "
                    + "ON vehicles(registration COLLATE NOCASE)");
        }
    }

}

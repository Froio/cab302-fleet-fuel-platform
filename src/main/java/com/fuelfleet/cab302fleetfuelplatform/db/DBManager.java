package com.fuelfleet.cab302fleetfuelplatform.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBManager {
    private static final String DB_DIR = "data";
    private static final String DB_FILE = "data/fleet.db";

    static {
        try {
            Path dir = Path.of(DB_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + DB_FILE;
        return DriverManager.getConnection(url);
    }

    private static void initialize() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            // users table
            s.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, username TEXT UNIQUE, password TEXT, role TEXT)");
            // vehicles table
            s.executeUpdate("CREATE TABLE IF NOT EXISTS vehicles (id INTEGER PRIMARY KEY, registration TEXT, make TEXT, model TEXT)");

            // ensure default admin exists
            s.executeUpdate("INSERT OR IGNORE INTO users (id, username, password, role) VALUES (1, 'admin', 'password', 'manager')");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

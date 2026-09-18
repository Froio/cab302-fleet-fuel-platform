package com.fuelfleet.cab302fleetfuelplatform.dao;

import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.model.FuelRecord;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class AnalyticsDao {
    public record Records(List<FuelRecord> rows, int invalidRows) { }
    private final ConnectionProvider connections;
    public AnalyticsDao() { this(DBManager::getConnection); }
    public AnalyticsDao(ConnectionProvider connections) { this.connections = connections; }

    public Records load(Integer vehicleId) {
        String sql = "SELECT vehicle_id, date, litres, cost, odometer, fuel_type, full_tank FROM fuel_logs"
                + (vehicleId == null ? "" : " WHERE vehicle_id = ?") + " ORDER BY date, odometer, id";
        var records = new ArrayList<FuelRecord>();
        int invalid = 0;
        try (Connection connection = connections.open(); PreparedStatement query = connection.prepareStatement(sql)) {
            if (vehicleId != null) query.setInt(1, vehicleId);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    try {
                        records.add(new FuelRecord(rows.getInt("vehicle_id"), LocalDate.parse(rows.getString("date")),
                                number(rows,"litres"), number(rows,"cost"), number(rows,"odometer"),
                                rows.getString("fuel_type"), fullTank(rows)));
                    } catch (IllegalArgumentException | DateTimeParseException | NullPointerException exception) {
                        invalid++;
                    }
                }
            }
            return new Records(List.copyOf(records), invalid);
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load fuel records.", exception);
        }
    }
    private static double number(ResultSet rows, String column) throws SQLException {
        Object value = rows.getObject(column);
        if (!(value instanceof Number number)) throw new IllegalArgumentException("Invalid numeric data");
        return number.doubleValue();
    }
    private static boolean fullTank(ResultSet rows) throws SQLException {
        double value = number(rows,"full_tank");
        if (value != 0 && value != 1) throw new IllegalArgumentException("Invalid tank flag");
        return value == 1;
    }
}

package com.fuelfleet.cab302fleetfuelplatform.dao;

import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleDao {
    public List<Vehicle> listAll() {
        List<Vehicle> out = new ArrayList<>();
        try (Connection c = DBManager.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT id, registration, make, model FROM vehicles")) {
            while (rs.next()) {
                out.add(new Vehicle(rs.getInt("id"), rs.getString("registration"), rs.getString("make"), rs.getString("model")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    public void save(Vehicle v) {
        String sql = (v.getId() <= 0)
                ? "INSERT INTO vehicles (registration, make, model) VALUES (?, ?, ?)"
                : "UPDATE vehicles SET registration = ?, make = ?, model = ? WHERE id = ?";
        try (Connection c = DBManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getRegistration());
            ps.setString(2, v.getMake());
            ps.setString(3, v.getModel());
            if (v.getId() > 0) ps.setInt(4, v.getId());
            ps.executeUpdate();
            if (v.getId() <= 0) {
                ResultSet keys = ps.getGeneratedKeys();
                if (keys.next()) v.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Vehicle findById(int id) {
        try (Connection c = DBManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id, registration, make, model FROM vehicles WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Vehicle(rs.getInt("id"), rs.getString("registration"), rs.getString("make"), rs.getString("model"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}

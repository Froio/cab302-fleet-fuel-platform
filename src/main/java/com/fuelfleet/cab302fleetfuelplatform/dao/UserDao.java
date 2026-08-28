package com.fuelfleet.cab302fleetfuelplatform.dao;

import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDao {
    public boolean authenticate(String username, String password) {
        try (Connection c = DBManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE username = ? AND password = ?")) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

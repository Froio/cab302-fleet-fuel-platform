package com.fuelfleet.cab302fleetfuelplatform.dao;

import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private static final String NORMALIZED_ROLE_SQL =
            "TRIM(role, ' ' || CHAR(9) || CHAR(10) || CHAR(11) || CHAR(12) || CHAR(13))";

    private final ConnectionProvider connectionProvider;

    public UserDao() {
        this(DBManager::getConnection);
    }

    public UserDao(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public Optional<Credentials> findCredentialsByUsername(String username) {
        String sql = "SELECT id, username, password, role FROM users WHERE username = ? COLLATE NOCASE";
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                User user = new User(
                        rows.getInt("id"),
                        rows.getString("username"),
                        Role.fromDatabase(rows.getString("role"))
                );
                return Optional.of(new Credentials(user, rows.getString("password")));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load user credentials", exception);
        }
    }

    public List<User> listAll() {
        return listUsers("SELECT id, username, role FROM users ORDER BY username COLLATE NOCASE", null);
    }

    public List<User> listByRole(Role role) {
        return listUsers("SELECT id, username, role FROM users WHERE role = ? COLLATE NOCASE "
                + "ORDER BY username COLLATE NOCASE", role.toDatabase());
    }

    public Optional<User> findById(int id) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, username, role FROM users WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(mapUser(rows)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to load user", exception);
        }
    }

    public boolean existsByUsername(String username) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM users WHERE username = ? COLLATE NOCASE")) {
            statement.setString(1, username);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to check username", exception);
        }
    }

    public boolean existsByRole(Role role) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM users WHERE " + NORMALIZED_ROLE_SQL + " = ? COLLATE NOCASE LIMIT 1")) {
            statement.setString(1, role.toDatabase());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to check user role", exception);
        }
    }

    public User insert(String username, String encodedPassword, Role role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, encodedPassword);
            statement.setString(3, role.toDatabase());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Database did not return a user ID");
                }
                return new User(keys.getInt(1), username, role);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to create user", exception);
        }
    }

    public Optional<User> insertFirstManager(String username, String encodedPassword) {
        String sql = """
                INSERT INTO users (username, password, role)
                SELECT ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM users WHERE %s = ? COLLATE NOCASE
                )
                """.formatted(NORMALIZED_ROLE_SQL);
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, encodedPassword);
            statement.setString(3, Role.MANAGER.toDatabase());
            statement.setString(4, Role.MANAGER.toDatabase());
            if (statement.executeUpdate() != 1) {
                return Optional.empty();
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Database did not return a user ID");
                }
                return Optional.of(new User(keys.getInt(1), username, Role.MANAGER));
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to create the first fleet manager", exception);
        }
    }

    public boolean delete(int userId) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
            statement.setInt(1, userId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to delete user", exception);
        }
    }

    public void updatePassword(int userId, String encodedPassword) {
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET password = ? WHERE id = ?")) {
            statement.setString(1, encodedPassword);
            statement.setInt(2, userId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to update password", exception);
        }
    }

    public record Credentials(User user, String encodedPassword) {
    }

    private List<User> listUsers(String sql, String role) {
        List<User> users = new ArrayList<>();
        try (Connection connection = connectionProvider.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (role != null) {
                statement.setString(1, role);
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    users.add(mapUser(rows));
                }
            }
            return users;
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to list users", exception);
        }
    }

    private static User mapUser(ResultSet rows) throws SQLException {
        return new User(
                rows.getInt("id"),
                rows.getString("username"),
                Role.fromDatabase(rows.getString("role"))
        );
    }
}


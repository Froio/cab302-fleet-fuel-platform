package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DuplicateUsernameException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.PasswordHasher;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

import java.util.List;

public final class UserService {
    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final AppSession session;

    public UserService() {
        this(new UserDao(), new PasswordHasher(), AppSession.getInstance());
    }

    public UserService(UserDao userDao, PasswordHasher passwordHasher, AppSession session) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.session = session;
    }

    public List<User> listUsers() {
        requireManager();
        return userDao.listAll();
    }

    public List<User> listDrivers() {
        requireManager();
        return userDao.listByRole(Role.DRIVER);
    }

    public User createUser(String username, String password, Role role) {
        requireManager();
        String normalizedUsername = requireText(username, "Username is required.");
        requireText(password, "Password is required.");
        if (role == null) {
            throw new ValidationException("Role is required.");
        }
        if (userDao.existsByUsername(normalizedUsername)) {
            throw new DuplicateUsernameException();
        }

        try {
            return userDao.insert(normalizedUsername, passwordHasher.hash(password), role);
        } catch (DataAccessException exception) {
            if (exception.isConstraintViolation()) {
                throw new DuplicateUsernameException();
            }
            throw exception;
        }
    }

    public void deleteUser(int userId) {
        requireManager();
        User currentUser = session.currentUser().orElseThrow(
                () -> new AuthorizationException("Please sign in as a fleet manager."));
        if (currentUser.id() == userId) {
            throw new ValidationException("You cannot delete the account you are currently using.");
        }
        if (userDao.findById(userId).isEmpty()) {
            throw new ValidationException("The selected user no longer exists.");
        }
        if (!userDao.delete(userId)) {
            throw new ValidationException("The selected user no longer exists.");
        }
    }

    private void requireManager() {
        if (!session.isManager()) {
            throw new AuthorizationException("Fleet manager access is required.");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }
}

package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DuplicateUsernameException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.PasswordHasher;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

public final class ManagerRegistrationService {
    private static final String REGISTRATION_CLOSED_MESSAGE =
            "A Fleet Manager account already exists. Ask a manager to create your account.";

    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final AppSession session;

    public ManagerRegistrationService() {
        this(new UserDao(), new PasswordHasher(), AppSession.getInstance());
    }

    public ManagerRegistrationService(
            UserDao userDao,
            PasswordHasher passwordHasher,
            AppSession session
    ) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.session = session;
    }

    public boolean isRegistrationAvailable() {
        return !userDao.existsByRole(Role.MANAGER);
    }

    public User registerFirstManager(String username, String password, String passwordConfirmation) {
        String normalizedUsername = requireText(username, "Username is required.");
        requirePassword(password, "Password is required.");
        requirePassword(passwordConfirmation, "Password confirmation is required.");

        if (!password.equals(passwordConfirmation)) {
            throw new ValidationException("Passwords do not match.");
        }
        if (!isRegistrationAvailable()) {
            throw new ValidationException(REGISTRATION_CLOSED_MESSAGE);
        }
        if (userDao.existsByUsername(normalizedUsername)) {
            throw new DuplicateUsernameException();
        }

        User manager;
        try {
            manager = userDao.insertFirstManager(
                            normalizedUsername,
                            passwordHasher.hash(password))
                    .orElseThrow(() -> new ValidationException(REGISTRATION_CLOSED_MESSAGE));
        } catch (DataAccessException exception) {
            if (exception.isConstraintViolation()) {
                throw new DuplicateUsernameException();
            }
            throw exception;
        }
        session.signIn(manager);
        return manager;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
        return value.trim();
    }

    private static void requirePassword(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}

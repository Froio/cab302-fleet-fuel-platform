package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.PasswordHasher;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

public final class AuthenticationService {
    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final AppSession session;

    public AuthenticationService() {
        this(new UserDao(), new PasswordHasher(), AppSession.getInstance());
    }

    public AuthenticationService(UserDao userDao, PasswordHasher passwordHasher, AppSession session) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.session = session;
    }

    public Optional<User> login(String username, String password) {
        session.signOut();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        Optional<UserDao.Credentials> credentials = userDao.findCredentialsByUsername(username.trim());
        if (credentials.isEmpty()) {
            return Optional.empty();
        }

        UserDao.Credentials stored = credentials.get();
        boolean authenticated = passwordHasher.isEncoded(stored.encodedPassword())
                ? passwordHasher.matches(password, stored.encodedPassword())
                : matchesLegacy(password, stored.encodedPassword());
        if (!authenticated) {
            return Optional.empty();
        }

        if (!passwordHasher.isEncoded(stored.encodedPassword())) {
            userDao.updatePassword(stored.user().id(), passwordHasher.hash(password));
        }
        session.signIn(stored.user());
        return Optional.of(stored.user());
    }

    private static boolean matchesLegacy(String supplied, String stored) {
        if (stored == null) {
            return false;
        }
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8)
        );
    }
}

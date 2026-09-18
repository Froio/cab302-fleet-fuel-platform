package com.fuelfleet.cab302fleetfuelplatform.session;

import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;

import java.util.Optional;

public final class AppSession {
    private static final AppSession INSTANCE = new AppSession();

    private User currentUser;

    public static AppSession getInstance() {
        return INSTANCE;
    }

    public void signIn(User user) {
        currentUser = user;
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isManager() {
        return currentUser != null && currentUser.role() == Role.MANAGER;
    }

    public void signOut() {
        currentUser = null;
    }
}

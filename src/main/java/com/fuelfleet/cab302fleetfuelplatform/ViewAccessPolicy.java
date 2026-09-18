package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;

import java.util.Optional;
import java.util.Set;

final class ViewAccessPolicy {
    private static final String LOGIN_VIEW = "login-view.fxml";
    private static final Set<String> DRIVER_VIEWS = Set.of(
            "driver-dashboard.fxml",
            "fuel-logging.fxml"
    );
    private static final Set<String> MANAGER_VIEWS = Set.of(
            "manager-dashboard.fxml",
            "user-management.fxml",
            "vehicle-list.fxml",
            "vehicle-edit.fxml",
            "vehicle-assignment.fxml",
            "reports.fxml"
    );

    private ViewAccessPolicy() {
    }

    static Set<String> managerViews() {
        return MANAGER_VIEWS;
    }

    static String resolve(String requestedView, Optional<User> currentUser) {
        if (MANAGER_VIEWS.contains(requestedView)) {
            return currentUser.filter(user -> user.role() == Role.MANAGER)
                    .map(user -> requestedView)
                    .orElse(LOGIN_VIEW);
        }
        if (DRIVER_VIEWS.contains(requestedView)) {
            return currentUser.filter(user -> user.role() == Role.DRIVER)
                    .map(user -> requestedView)
                    .orElse(LOGIN_VIEW);
        }
        return requestedView;
    }
}

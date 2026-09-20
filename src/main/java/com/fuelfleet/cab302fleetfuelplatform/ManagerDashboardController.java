package com.fuelfleet.cab302fleetfuelplatform;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

public class ManagerDashboardController {
    @FXML
    private Label infoLabel;

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
        }
    }

    @FXML
    private void onUsers() {
        HelloApplication.switchScene("user-management.fxml");
    }

    @FXML
    private void onVehicles() {
        HelloApplication.switchScene("vehicle-list.fxml");
    }

    @FXML
    private void onAssignments() {
        HelloApplication.switchScene("vehicle-assignment.fxml");
    }

    @FXML
    private void onDrivers() {
        HelloApplication.switchScene("driver-list.fxml");
    }

    @FXML
    private void onReports() {
        HelloApplication.switchScene("reports.fxml");
    }

    @FXML
    private void onLogout() {
        AppSession.getInstance().signOut();
        HelloApplication.switchScene("login-view.fxml");
    }
}
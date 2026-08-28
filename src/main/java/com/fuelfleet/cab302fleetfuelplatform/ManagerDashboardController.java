package com.fuelfleet.cab302fleetfuelplatform;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ManagerDashboardController {
    @FXML
    private Label infoLabel;

    @FXML
    private void onVehicles() {
        HelloApplication.switchScene("vehicle-list.fxml");
    }

    @FXML
    private void onReports() {
        infoLabel.setText("Reports view not implemented yet.");
    }

    @FXML
    private void onLogout() {
        HelloApplication.switchScene("login-view.fxml");
    }
}

package com.fuelfleet.cab302fleetfuelplatform;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.service.AuthenticationService;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;

    private final AuthenticationService authenticationService = new AuthenticationService();

    @FXML
    private void onLogin() {
        statusLabel.setText("");
        try {
            authenticationService.login(usernameField.getText(), passwordField.getText())
                    .ifPresentOrElse(
                            user -> HelloApplication.switchScene(user.role() == Role.MANAGER
                                    ? "manager-dashboard.fxml"
                                    : "driver-dashboard.fxml"),
                            () -> statusLabel.setText("Invalid username or password.")
                    );
        } catch (DataAccessException exception) {
            statusLabel.setText("Unable to access the database. Please try again.");
        }
    }

    @FXML
    private void onRegister() {
        HelloApplication.switchScene("manager-registration.fxml");
    }
}

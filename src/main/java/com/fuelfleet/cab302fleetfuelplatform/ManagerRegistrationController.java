package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.service.ManagerRegistrationService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class ManagerRegistrationController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button createButton;
    @FXML
    private Label statusLabel;

    private final ManagerRegistrationService registrationService =
            new ManagerRegistrationService();

    @FXML
    private void initialize() {
        try {
            if (!registrationService.isRegistrationAvailable()) {
                createButton.setDisable(true);
                showError("A Fleet Manager account already exists. Ask a manager to create your account.");
            }
        } catch (DataAccessException exception) {
            createButton.setDisable(true);
            showError("Unable to check registration availability because the database operation failed.");
        }
    }

    @FXML
    private void onRegister() {
        try {
            registrationService.registerFirstManager(
                    usernameField.getText(),
                    passwordField.getText(),
                    confirmPasswordField.getText()
            );
            HelloApplication.switchScene("manager-dashboard.fxml");
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        } catch (DataAccessException exception) {
            showError("Unable to create the account because the database operation failed.");
        }
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("login-view.fxml");
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #b42318;");
        statusLabel.setText(message);
    }
}

package com.fuelfleet.cab302fleetfuelplatform;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import com.fuelfleet.cab302fleetfuelplatform.service.UserService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;

public class ProfileEditController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button saveButton;
    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        try {
            var profile = userService.currentProfile();
            nameField.setText(profile.name());
            emailField.setText(profile.contactEmail());
            statusLabel.setText(profile.name().isBlank() && profile.contactEmail().isBlank()
                    ? "No contact details have been recorded for your account yet." : "");
        } catch (AuthorizationException exception) {
            HelloApplication.switchScene("login-view.fxml");
        } catch (DataAccessException exception) {
            saveButton.setDisable(true);
            statusLabel.setText("Unable to load your profile. Return to the dashboard and try again.");
        }
    }

    @FXML
    private void onSave() {
        try {
            var profile = userService.updateCurrentProfile(nameField.getText(), emailField.getText());
            nameField.setText(profile.name());
            emailField.setText(profile.contactEmail());
            statusLabel.setText("Profile saved.");
        } catch (AuthorizationException exception) {
            HelloApplication.switchScene("login-view.fxml");
        } catch (ValidationException exception) {
            statusLabel.setText(exception.getMessage());
        } catch (DataAccessException exception) {
            statusLabel.setText("Unable to save your profile. Please try again.");
        }
    }

    @FXML
    private void onBack() {
        var session = AppSession.getInstance();
        HelloApplication.switchScene(session.currentUser().isEmpty() ? "login-view.fxml"
                : session.isManager() ? "manager-dashboard.fxml" : "driver-dashboard.fxml");
    }
}

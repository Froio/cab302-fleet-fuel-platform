package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.service.UserService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ProfileEditController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private Label statusLabel;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        try {
            UserDao.Profile profile = userService.getMyProfile();

            nameField.setText(profile.fullName());
            emailField.setText(profile.contactEmail());

            showSuccess("Profile loaded successfully.");

        } catch (RuntimeException exception) {
            showError(exception.getMessage());
        }
    }

        @FXML
        private void onSave () {
            try {
                userService.updateMyProfile(
                        nameField.getText(),
                        emailField.getText()
                );

                showSuccess("profile updated successfully");
            } catch (RuntimeException exception) {
                showError(exception.getMessage());
            }
        }

        private void showError (String message){
            statusLabel.setStyle("-fx-text-fill: #b42318");
            statusLabel.setText(message);
        }

        private void showSuccess (String message){
            statusLabel.setStyle("-fx-text-fill: #067647;");
            statusLabel.setText(message);
        }

        @FXML
        private void onBack () {
            String dashdoard = AppSession.getInstance().isManager()
                    ? "manager-dashboard.fxml"
                    : "driver-dashboard.fxml";

            HelloApplication.switchScene(dashdoard);
        }
    }

















package com.fuelfleet.cab302fleetfuelplatform;

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

    @FXML
    private void initialize() {
        nameField.setText("Sample User");
        emailField.setText("sample.user@example.com");
        statusLabel.setText("Placeholder data - profile saving to be connected.");
    }

    @FXML
    private void onSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: #b42318;");
            statusLabel.setText("Name is required.");
            return;
        }
        if (!email.contains("@") || email.startsWith("@") || email.endsWith("@")) {
            statusLabel.setStyle("-fx-text-fill: #b42318;");
            statusLabel.setText("Enter a valid contact email.");
            return;
        }
        statusLabel.setStyle("-fx-text-fill: #067647;");
        statusLabel.setText("Profile updated (mockup - not stored yet).");
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }
}
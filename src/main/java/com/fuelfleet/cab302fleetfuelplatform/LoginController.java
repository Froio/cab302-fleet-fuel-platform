package com.fuelfleet.cab302fleetfuelplatform;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;

    private final UserDao userDao = new UserDao();

    @FXML
    private void onLogin() {
        String u = usernameField.getText();
        String p = passwordField.getText();
        if (userDao.authenticate(u, p)) {
            HelloApplication.switchScene("manager-dashboard.fxml");
        } else {
            statusLabel.setText("Invalid credentials");
        }
    }
}

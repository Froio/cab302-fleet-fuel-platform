package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.service.UserService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class UserManagementController {
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, Number> idColumn;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<Role> roleCombo;
    @FXML
    private Label statusLabel;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }

        idColumn.setCellValueFactory(row -> new ReadOnlyIntegerWrapper(row.getValue().id()));
        usernameColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().username()));
        roleColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().role().toString()));
        roleCombo.setItems(FXCollections.observableArrayList(Role.DRIVER, Role.MANAGER));
        roleCombo.setValue(Role.DRIVER);
        reloadUsers();
    }

    @FXML
    private void onCreate() {
        try {
            User created = userService.createUser(
                    usernameField.getText(),
                    passwordField.getText(),
                    roleCombo.getValue()
            );
            usernameField.clear();
            passwordField.clear();
            roleCombo.setValue(Role.DRIVER);
            reloadUsers();
            showSuccess("Created " + created.role() + " account " + created.username() + ".");
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        } catch (DataAccessException exception) {
            showError("Unable to create the account because the database operation failed.");
        }
    }

    @FXML
    private void onDelete() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select an account to delete.");
            return;
        }
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete account '" + selected.username() + "'? Assigned vehicles will become unassigned.",
                ButtonType.CANCEL,
                ButtonType.OK
        );
        confirmation.setHeaderText("Confirm account deletion");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            userService.deleteUser(selected.id());
            reloadUsers();
            showSuccess("Deleted account " + selected.username() + ".");
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        } catch (DataAccessException exception) {
            showError("Unable to delete the account because the database operation failed.");
        }
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }

    private void reloadUsers() {
        try {
            userTable.setItems(FXCollections.observableArrayList(userService.listUsers()));
        } catch (DataAccessException exception) {
            showError("Unable to load accounts from the database.");
        }
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #b42318;");
        statusLabel.setText(message);
    }

    private void showSuccess(String message) {
        statusLabel.setStyle("-fx-text-fill: #067647;");
        statusLabel.setText(message);
    }
}

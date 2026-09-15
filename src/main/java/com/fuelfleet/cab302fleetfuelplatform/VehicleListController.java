package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.service.VehicleService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;

public class VehicleListController {
    @FXML
    private TableView<Vehicle> vehicleTable;
    @FXML
    private TableColumn<Vehicle, Number> idColumn;
    @FXML
    private TableColumn<Vehicle, String> registrationColumn;
    @FXML
    private TableColumn<Vehicle, String> makeColumn;
    @FXML
    private TableColumn<Vehicle, String> modelColumn;
    @FXML
    private TableColumn<Vehicle, String> fuelTypeColumn;
    @FXML
    private TableColumn<Vehicle, Number> odometerColumn;
    @FXML
    private TableColumn<Vehicle, String> driverColumn;
    @FXML
    private Label statusLabel;

    private final VehicleService vehicleService = new VehicleService();

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }
        idColumn.setCellValueFactory(row -> new ReadOnlyIntegerWrapper(row.getValue().id()));
        registrationColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().registration()));
        makeColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().make()));
        modelColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().model()));
        fuelTypeColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().fuelType()));
        odometerColumn.setCellValueFactory(row -> new ReadOnlyLongWrapper(row.getValue().currentOdometer()));
        driverColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().assignedDriverDisplay()));
        reloadVehicles();
    }

    @FXML
    private void onAdd() {
        HelloApplication.switchScene("vehicle-edit.fxml");
    }

    @FXML
    private void onEdit() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a vehicle to edit.");
            return;
        }
        HelloApplication.switchScene(
                "vehicle-edit.fxml",
                VehicleEditController.class,
                controller -> controller.editVehicle(selected)
        );
    }

    @FXML
    private void onDelete() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Select a vehicle to delete.");
            return;
        }
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete vehicle '" + selected.registration() + "'?",
                ButtonType.CANCEL,
                ButtonType.OK
        );
        confirmation.setHeaderText("Confirm vehicle deletion");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            vehicleService.deleteVehicle(selected.id());
            reloadVehicles();
            showSuccess("Deleted vehicle " + selected.registration() + ".");
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        } catch (DataAccessException exception) {
            showError("Unable to delete the vehicle because the database operation failed.");
        }
    }

    @FXML
    private void onAssignments() {
        HelloApplication.switchScene("vehicle-assignment.fxml");
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }

    private void reloadVehicles() {
        try {
            vehicleTable.setItems(FXCollections.observableArrayList(vehicleService.listVehicles()));
        } catch (DataAccessException exception) {
            showError("Unable to load vehicles from the database.");
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

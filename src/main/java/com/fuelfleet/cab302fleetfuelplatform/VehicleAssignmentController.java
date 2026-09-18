package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.service.VehicleAssignmentService;
import com.fuelfleet.cab302fleetfuelplatform.service.VehicleService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class VehicleAssignmentController {
    @FXML
    private TableView<Vehicle> assignmentTable;
    @FXML
    private TableColumn<Vehicle, String> registrationColumn;
    @FXML
    private TableColumn<Vehicle, String> vehicleColumn;
    @FXML
    private TableColumn<Vehicle, String> driverColumn;
    @FXML
    private ComboBox<Vehicle> vehicleCombo;
    @FXML
    private ComboBox<User> driverCombo;
    @FXML
    private Label statusLabel;

    private final VehicleService vehicleService = new VehicleService();
    private final VehicleAssignmentService assignmentService = new VehicleAssignmentService();

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }
        registrationColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().registration()));
        vehicleColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().make() + " " + row.getValue().model()));
        driverColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().assignedDriverDisplay()));
        assignmentTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> selectAssignment(selected));
        reloadData(null);
    }

    @FXML
    private void onAssign() {
        Vehicle vehicle = vehicleCombo.getValue();
        User driver = driverCombo.getValue();
        if (vehicle == null || driver == null) {
            showError("Select both a vehicle and a driver.");
            return;
        }
        try {
            assignmentService.assignDriver(vehicle.id(), driver.id());
            reloadData(vehicle.id());
            showSuccess("Assigned " + driver.username() + " to " + vehicle.registration() + ".");
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        } catch (DataAccessException exception) {
            showError("Unable to save the assignment because the database operation failed.");
        }
    }

    @FXML
    private void onUnassign() {
        Vehicle vehicle = vehicleCombo.getValue();
        if (vehicle == null) {
            showError("Select a vehicle to unassign.");
            return;
        }
        try {
            assignmentService.unassignDriver(vehicle.id());
            reloadData(vehicle.id());
            showSuccess("Vehicle " + vehicle.registration() + " is now unassigned.");
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        } catch (DataAccessException exception) {
            showError("Unable to clear the assignment because the database operation failed.");
        }
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }

    private void reloadData(Integer selectedVehicleId) {
        try {
            var vehicles = vehicleService.listVehicles();
            var drivers = assignmentService.listDrivers();
            assignmentTable.setItems(FXCollections.observableArrayList(vehicles));
            vehicleCombo.setItems(FXCollections.observableArrayList(vehicles));
            driverCombo.setItems(FXCollections.observableArrayList(drivers));
            if (selectedVehicleId != null) {
                vehicles.stream()
                        .filter(vehicle -> vehicle.id() == selectedVehicleId)
                        .findFirst()
                        .ifPresent(vehicle -> {
                            vehicleCombo.setValue(vehicle);
                            assignmentTable.getSelectionModel().select(vehicle);
                        });
            }
        } catch (DataAccessException exception) {
            showError("Unable to load vehicles and drivers from the database.");
        }
    }

    private void selectAssignment(Vehicle vehicle) {
        if (vehicle == null) {
            return;
        }
        vehicleCombo.setValue(vehicle);
        if (vehicle.assignedDriverId() == null) {
            driverCombo.getSelectionModel().clearSelection();
            driverCombo.setValue(null);
            return;
        }
        driverCombo.getItems().stream()
                .filter(driver -> driver.id() == vehicle.assignedDriverId())
                .findFirst()
                .ifPresent(driverCombo::setValue);
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

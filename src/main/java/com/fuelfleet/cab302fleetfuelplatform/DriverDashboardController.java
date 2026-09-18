package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.service.VehicleService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class DriverDashboardController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private TableView<Vehicle> vehicleTable;
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

    private final VehicleService vehicleService = new VehicleService();
    private final AppSession session = AppSession.getInstance();

    @FXML
    private void initialize() {
        var currentUser = session.currentUser();
        if (currentUser.isEmpty() || currentUser.get().role() != Role.DRIVER) {
            session.signOut();
            HelloApplication.switchScene("login-view.fxml");
            return;
        }

        welcomeLabel.setText("Welcome, " + currentUser.get().username());
        registrationColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().registration()));
        makeColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().make()));
        modelColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().model()));
        fuelTypeColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().fuelType()));
        odometerColumn.setCellValueFactory(row -> new ReadOnlyLongWrapper(row.getValue().currentOdometer()));
        reloadVehicles();
    }

    private void reloadVehicles() {
        try {
            vehicleTable.setItems(FXCollections.observableArrayList(
                    vehicleService.listVehiclesForCurrentDriver()));
            statusLabel.setText(vehicleTable.getItems().isEmpty()
                    ? "No vehicle is currently assigned to you."
                    : "");
        } catch (DataAccessException exception) {
            statusLabel.setText("Unable to load assigned vehicles.");
        }
    }

    @FXML
    private void onAddFuelLog() {
        HelloApplication.switchScene("fuel-logging.fxml");
    }

    @FXML
    private void onLogout() {
        session.signOut();
        HelloApplication.switchScene("login-view.fxml");
    }
}

package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;

public class DriverListController {
    public record DriverRow(String name, String vehicle, String status) {}

    @FXML
    private TableView<DriverRow> driverTable;
    @FXML
    private TableColumn<DriverRow, String> nameColumn;
    @FXML
    private TableColumn<DriverRow, String> vehicleColumn;
    @FXML
    private TableColumn<DriverRow, String> statusColumn;
    @FXML
    private Label statusLabel;

    private final UserDao userDao = new UserDao();
    private final VehicleDao vehicleDao = new VehicleDao();

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }
        nameColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().name()));
        vehicleColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().vehicle()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().status()));
        loadDrivers();
    }

    private void loadDrivers() {
        try {
            List<User> drivers = userDao.listByRole(Role.DRIVER);
            List<Vehicle> vehicles = vehicleDao.listAll();
            List<DriverRow> rows = new ArrayList<>();
            for (User driver : drivers) {
                Vehicle assigned = vehicles.stream()
                        .filter(v -> v.assignedDriverId() != null && v.assignedDriverId() == driver.id())
                        .findFirst()
                        .orElse(null);
                rows.add(new DriverRow(
                        driver.username(),
                        assigned == null ? "-" : assigned.registration(),
                        assigned == null ? "Unassigned" : "Assigned"
                ));
            }
            driverTable.setItems(FXCollections.observableArrayList(rows));
            statusLabel.setText(rows.isEmpty() ? "No drivers found." : rows.size() + " driver(s).");
        } catch (Exception exception) {
            statusLabel.setText("Unable to load drivers: " + exception.getMessage());
        }
    }

    @FXML
    private void onManageDrivers() {
        HelloApplication.switchScene("user-management.fxml");
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }
}
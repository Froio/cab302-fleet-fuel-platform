package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

public class UnassignedVehiclesController {
    public record VehicleRow(String registration, String makeModel, String status) {}

    @FXML
    private TableView<VehicleRow> vehicleTable;
    @FXML
    private TableColumn<VehicleRow, String> registrationColumn;
    @FXML
    private TableColumn<VehicleRow, String> makeModelColumn;
    @FXML
    private TableColumn<VehicleRow, String> statusColumn;
    @FXML
    private CheckBox unassignedOnlyBox;
    @FXML
    private Label statusLabel;

    private final VehicleDao vehicleDao = new VehicleDao();
    private List<VehicleRow> allRows = List.of();

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }
        registrationColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().registration()));
        makeModelColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().makeModel()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().status()));
        loadVehicles();
    }

    private void loadVehicles() {
        try {
            List<Vehicle> vehicles = vehicleDao.listAll();
            allRows = vehicles.stream()
                    .map(v -> new VehicleRow(
                            v.registration(),
                            v.make() + " " + v.model(),
                            v.assignedDriverId() == null ? "UNASSIGNED" : "Assigned"
                    ))
                    .toList();
            onFilter();
            statusLabel.setText(allRows.size() + " vehicle(s).");
        } catch (Exception exception) {
            statusLabel.setText("Unable to load vehicles: " + exception.getMessage());
        }
    }

    @FXML
    private void onFilter() {
        List<VehicleRow> rows = allRows;
        if (unassignedOnlyBox.isSelected()) {
            rows = allRows.stream().filter(row -> row.status().equals("UNASSIGNED")).toList();
        }
        vehicleTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }
}
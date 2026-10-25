package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import com.fuelfleet.cab302fleetfuelplatform.service.FleetDirectoryService;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;


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

    private final FleetDirectoryService directoryService = new FleetDirectoryService();

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }
        registrationColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().registration()));
        makeModelColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().makeModel()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().status()));
        vehicleTable.setPlaceholder(new Label("No matching vehicles."));
        onFilter();
    }

    @FXML
    private void onFilter() {
        try {
            var rows = directoryService.listVehicles(unassignedOnlyBox.isSelected()).stream()
                    .map(vehicle -> new VehicleRow(vehicle.registration(), vehicle.make() + " " + vehicle.model(),
                            vehicle.assignedDriverId() == null ? "Unassigned" : "Assigned to " + vehicle.assignedDriverDisplay()))
                    .toList();
            vehicleTable.setItems(FXCollections.observableArrayList(rows));
            statusLabel.setText(rows.size() + " vehicle(s) shown.");
        } catch (AuthorizationException exception) {
            HelloApplication.switchScene("login-view.fxml");
        } catch (DataAccessException exception) {
            vehicleTable.getItems().clear();
            statusLabel.setText("Unable to load vehicle assignments. Try Refresh.");
        }
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }
}

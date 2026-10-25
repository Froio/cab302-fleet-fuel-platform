package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import com.fuelfleet.cab302fleetfuelplatform.service.FleetDirectoryService;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.util.ArrayList;

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
    private final FleetDirectoryService directoryService = new FleetDirectoryService();

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }
        nameColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().name()));
        vehicleColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().vehicle()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().status()));
        driverTable.setPlaceholder(new Label("No driver accounts found."));
        onRefresh();
    }

    @FXML
    private void onRefresh() {
        try {
            var drivers = directoryService.listDrivers();
            var rows = new ArrayList<DriverRow>();
            for (var assignment : drivers) {
                if (assignment.vehicles().isEmpty()) {
                    rows.add(new DriverRow(assignment.driver().username(), "—", "Unassigned"));
                } else {
                    for (var vehicle : assignment.vehicles()) {
                        rows.add(new DriverRow(assignment.driver().username(), vehicle.registration(), "Assigned"));
                    }
                }
            }
            driverTable.setItems(FXCollections.observableArrayList(rows));
            statusLabel.setText(drivers.size() + " driver account(s). Each assigned vehicle has its own row.");
        } catch (AuthorizationException exception) {
            HelloApplication.switchScene("login-view.fxml");
        } catch (DataAccessException exception) {
            driverTable.getItems().clear();
            statusLabel.setText("Unable to load drivers and assignments. Try Refresh.");
        }
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }
}

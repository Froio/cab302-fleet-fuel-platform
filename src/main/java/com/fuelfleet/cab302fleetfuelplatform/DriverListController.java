package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

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

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }
        nameColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().name()));
        vehicleColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().vehicle()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().status()));
        driverTable.setItems(FXCollections.observableArrayList(
                new DriverRow("Sample Driver 1", "ABC123", "Assigned"),
                new DriverRow("Sample Driver 2", "-", "Unassigned"),
                new DriverRow("Sample Driver 3", "XYZ789", "Assigned")
        ));
        statusLabel.setText("Placeholder data - live driver list to be connected.");
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }
}
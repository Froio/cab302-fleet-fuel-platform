package com.fuelfleet.cab302fleetfuelplatform;

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

    private final List<VehicleRow> allRows = List.of(
            new VehicleRow("ABC123", "Toyota Hilux", "Assigned"),
            new VehicleRow("DEF456", "Mazda BT-50", "UNASSIGNED"),
            new VehicleRow("XYZ789", "Ford Ranger", "Assigned"),
            new VehicleRow("GHI321", "Isuzu D-Max", "UNASSIGNED")
    );

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }
        registrationColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().registration()));
        makeModelColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().makeModel()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().status()));
        onFilter();
        statusLabel.setText("Placeholder data - live vehicle assignments to be connected.");
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
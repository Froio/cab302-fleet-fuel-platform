package com.fuelfleet.cab302fleetfuelplatform;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;

public class VehicleListController {
    @FXML
    private ListView<String> vehicleList;

    private final VehicleDao vehicleDao = new VehicleDao();

    @FXML
    private void initialize() {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (Vehicle v : vehicleDao.listAll()) {
            items.add(String.format("%d: %s - %s %s", v.getId(), v.getRegistration(), v.getMake(), v.getModel()));
        }
        vehicleList.setItems(items);
    }

    @FXML
    private void onAdd() {
        HelloApplication.switchScene("vehicle-edit.fxml");
    }

    @FXML
    private void onEdit() {
        HelloApplication.switchScene("vehicle-edit.fxml");
    }

    @FXML
    private void onBack() {
        HelloApplication.switchScene("manager-dashboard.fxml");
    }
}

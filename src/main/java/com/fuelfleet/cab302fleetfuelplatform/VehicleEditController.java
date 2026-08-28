package com.fuelfleet.cab302fleetfuelplatform;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;

public class VehicleEditController {
    @FXML
    private TextField regField;
    @FXML
    private TextField makeField;
    @FXML
    private TextField modelField;

    private final VehicleDao vehicleDao = new VehicleDao();

    @FXML
    private void onSave() {
        Vehicle v = new Vehicle();
        v.setRegistration(regField.getText());
        v.setMake(makeField.getText());
        v.setModel(modelField.getText());
        vehicleDao.save(v);
        HelloApplication.switchScene("vehicle-list.fxml");
    }

    @FXML
    private void onCancel() {
        HelloApplication.switchScene("vehicle-list.fxml");
    }
}

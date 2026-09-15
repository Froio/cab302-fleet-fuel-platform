package com.fuelfleet.cab302fleetfuelplatform;

import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.service.VehicleService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;

public class VehicleEditController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label formDescriptionLabel;
    @FXML
    private TextField regField;
    @FXML
    private TextField makeField;
    @FXML
    private TextField modelField;
    @FXML
    private ComboBox<String> fuelTypeCombo;
    @FXML
    private TextField odometerField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button saveButton;

    private final VehicleService vehicleService = new VehicleService();
    private Integer editingVehicleId;

    @FXML
    private void initialize() {
        if (!AppSession.getInstance().isManager()) {
            HelloApplication.switchScene("login-view.fxml");
            return;
        }
        fuelTypeCombo.setItems(FXCollections.observableArrayList(
                "Petrol", "Diesel", "Hybrid", "Electric", "LPG"));
        fuelTypeCombo.setEditable(true);
        odometerField.setText("0");
    }

    void editVehicle(Vehicle vehicle) {
        editingVehicleId = vehicle.id();
        titleLabel.setText("Edit Vehicle");
        formDescriptionLabel.setText("Update the selected vehicle. Registration numbers must remain unique.");
        saveButton.setText("Save Changes");
        regField.setText(vehicle.registration());
        makeField.setText(vehicle.make());
        modelField.setText(vehicle.model());
        fuelTypeCombo.setValue(vehicle.fuelType());
        odometerField.setText(Long.toString(vehicle.currentOdometer()));
    }

    @FXML
    private void onSave() {
        try {
            if (editingVehicleId == null) {
                vehicleService.createVehicle(
                        regField.getText(),
                        makeField.getText(),
                        modelField.getText(),
                        fuelTypeCombo.getEditor().getText(),
                        odometerField.getText()
                );
            } else {
                vehicleService.updateVehicle(
                        editingVehicleId,
                        regField.getText(),
                        makeField.getText(),
                        modelField.getText(),
                        fuelTypeCombo.getEditor().getText(),
                        odometerField.getText()
                );
            }
            HelloApplication.switchScene("vehicle-list.fxml");
        } catch (ValidationException exception) {
            showError(exception.getMessage());
        } catch (DataAccessException exception) {
            showError("Unable to save the vehicle because the database operation failed.");
        }
    }

    @FXML
    private void onCancel() {
        HelloApplication.switchScene("vehicle-list.fxml");
    }

    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #b42318;");
        statusLabel.setText(message);
    }
}

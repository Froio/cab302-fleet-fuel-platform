package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.service.MaintenanceService;
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
    @FXML private Label dateAddedLabel;
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
    private TextField nextServiceField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button saveButton;

    private final VehicleService vehicleService = new VehicleService();
    private final MaintenanceService maintenanceService = new MaintenanceService();
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
        dateAddedLabel.setText(vehicle.dateAddedDisplay());
        titleLabel.setText("Edit Vehicle");
        formDescriptionLabel.setText("Update the selected vehicle. Registration numbers must remain unique.");
        saveButton.setText("Save Changes");
        regField.setText(vehicle.registration());
        makeField.setText(vehicle.make());
        modelField.setText(vehicle.model());
        fuelTypeCombo.setValue(vehicle.fuelType());
        odometerField.setText(Long.toString(vehicle.currentOdometer()));
        Long nextService = maintenanceService.getNextServiceOdometer(
                vehicle.id()
        );
        nextServiceField.setText(
                nextService == null ? "" : Long.toString(nextService)
        );
    }

    @FXML
    private void onSave() {
        try {
            Long nextService = parseNextServiceOdometer();
            if (editingVehicleId == null) {
                Vehicle newVehicle = vehicleService.createVehicle(

                        regField.getText(),
                        makeField.getText(),
                        modelField.getText(),
                        fuelTypeCombo.getEditor().getText(),
                        odometerField.getText()
                );

                if (nextService != null) {
                    maintenanceService.scheduleService(
                            newVehicle.id(), nextService
                    );

                }



            } else {
                vehicleService.updateVehicle(
                        editingVehicleId,
                        regField.getText(),
                        makeField.getText(),
                        modelField.getText(),
                        fuelTypeCombo.getEditor().getText(),
                        odometerField.getText()
                );

                if (nextService == null) {
                    maintenanceService.clearServiceSchedule(editingVehicleId);

                } else {
                    maintenanceService.scheduleService(
                            editingVehicleId, nextService
                    );
                }
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

    private Long parseNextServiceOdometer() {
        String text = nextServiceField.getText().trim();

        if (text.isEmpty()){
            return null;
        }

        try {
            long reading = Long.parseLong(text);

            if (reading <0) {
                throw new ValidationException(
                        "Next service odometer cannot be negative."
                );

            }

            return reading;
        } catch (NumberFormatException exception) {
            throw new ValidationException(
                    "Next service odometer must be a whole number."
            );
        }
    }



    private void showError(String message) {
        statusLabel.setStyle("-fx-text-fill: #b42318;");
        statusLabel.setText(message);
    }
}
package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.FuelHistoryEntry;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.service.FuelCalculations;
import com.fuelfleet.cab302fleetfuelplatform.service.FuelLogService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public final class FuelLoggingController {
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<Vehicle> vehicleComboBox;
    @FXML
    private TextField litresField;
    @FXML
    private TextField pricePerLitreField;
    @FXML
    private Label totalCostLabel;
    @FXML
    private TextField odometerField;
    @FXML
    private CheckBox fullTankCheckBox;
    @FXML
    private Label statusLabel;
    @FXML
    private TableView<FuelHistoryEntry> historyTable;
    @FXML
    private TableColumn<FuelHistoryEntry, String> dateColumn;
    @FXML
    private TableColumn<FuelHistoryEntry, String> registrationColumn;
    @FXML
    private TableColumn<FuelHistoryEntry, String> litresColumn;
    @FXML
    private TableColumn<FuelHistoryEntry, String> costColumn;
    @FXML
    private TableColumn<FuelHistoryEntry, String> odometerColumn;
    @FXML
    private TableColumn<FuelHistoryEntry, String> efficiencyColumn;
    @FXML
    private TableColumn<FuelHistoryEntry, String> fullTankColumn;

    private final FuelLogOperations operations;
    private final AppSession session;
    private final Consumer<String> sceneSwitcher;
    private final FormAccess injectedForm;

    public FuelLoggingController() {
        this(new FuelLogService());
    }

    FuelLoggingController(FuelLogService service) {
        this(new ServiceOperations(service), AppSession.getInstance(), HelloApplication::switchScene, null);
    }

    FuelLoggingController(
            FuelLogOperations operations,
            AppSession session,
            Consumer<String> sceneSwitcher,
            FormAccess injectedForm
    ) {
        this.operations = Objects.requireNonNull(operations, "Fuel log operations are required");
        this.session = Objects.requireNonNull(session, "Session is required");
        this.sceneSwitcher = Objects.requireNonNull(sceneSwitcher, "Scene switcher is required");
        this.injectedForm = injectedForm;
    }

    @FXML
    void initialize() {
        FormAccess form = form();
        form.setDate(LocalDate.now());
        if (injectedForm == null) {
            configureHistoryTable();
            litresField.textProperty().addListener((observable, oldValue, newValue) -> updateCostPreview());
            pricePerLitreField.textProperty().addListener((observable, oldValue, newValue) -> updateCostPreview());
        }
        updateCostPreview();
        try {
            form.showData(loadData(null));
        } catch (AuthorizationException exception) {
            returnToLogin();
        } catch (DataAccessException exception) {
            form.setStatus(exception.getMessage());
        }
    }

    @FXML
    void onSave() {
        FormAccess form = form();
        Vehicle selectedVehicle = form.selectedVehicle();
        if (selectedVehicle == null) {
            form.setStatus("Select an assigned vehicle.");
            return;
        }
        try {
            operations.save(
                    selectedVehicle.id(),
                    form.date(),
                    form.litres(),
                    form.pricePerLitre(),
                    form.odometer(),
                    form.fullTank()
            );
        } catch (AuthorizationException exception) {
            returnToLogin();
            return;
        } catch (ValidationException | DataAccessException exception) {
            form.setStatus(exception.getMessage());
            return;
        }
        form.clearNumericInputs();
        try {
            form.showData(loadData(selectedVehicle.id()));
            form.setStatus("Fuel log saved.");
        } catch (AuthorizationException exception) {
            returnToLogin();
        } catch (DataAccessException exception) {
            form.setStatus("Fuel log saved, but history refresh failed: " + exception.getMessage());
        }
    }

    @FXML
    private void onBack() {
        sceneSwitcher.accept("driver-dashboard.fxml");
    }

    private void configureHistoryTable() {
        dateColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().log().date().toString()));
        registrationColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().log().vehicleRegistration()));
        litresColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                String.format(Locale.ROOT, "%,.2f", row.getValue().log().litres())));
        costColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                String.format(Locale.ROOT, "$%,.2f", row.getValue().log().cost())));
        odometerColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                String.format(Locale.ROOT, "%,.0f", row.getValue().log().odometer())));
        efficiencyColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().litresPer100Km().isPresent()
                        ? String.format(Locale.ROOT, "%.2f", row.getValue().litresPer100Km().getAsDouble())
                        : "—"));
        fullTankColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
                row.getValue().log().fullTank() ? "Yes" : "No"));
    }

    private ScreenData loadData(Integer preferredVehicleId) {
        List<Vehicle> vehicles = operations.listAssignedVehicles();
        List<FuelHistoryEntry> history = operations.listHistory();
        Vehicle selectedVehicle = vehicles.stream()
                .filter(vehicle -> Objects.equals(vehicle.id(), preferredVehicleId))
                .findFirst()
                .orElseGet(() -> vehicles.isEmpty() ? null : vehicles.getFirst());
        return new ScreenData(vehicles, history, selectedVehicle);
    }

    private void updateCostPreview() {
        FormAccess form = form();
        String litresText = form.litres();
        String priceText = form.pricePerLitre();
        if ((litresText == null || litresText.isBlank())
                && (priceText == null || priceText.isBlank())) {
            form.setTotalCost("$0.00");
            return;
        }
        try {
            double cost = FuelCalculations.costPerFillUp(
                    Double.parseDouble(litresText.trim()),
                    Double.parseDouble(priceText.trim())
            );
            form.setTotalCost(String.format(Locale.ROOT, "$%,.2f", cost));
        } catch (IllegalArgumentException exception) {
            form.setTotalCost("—");
        }
    }

    private FormAccess form() {
        return injectedForm == null ? new JavaFxForm() : injectedForm;
    }

    private void returnToLogin() {
        session.signOut();
        sceneSwitcher.accept("login-view.fxml");
    }

    interface FuelLogOperations {
        List<Vehicle> listAssignedVehicles();

        List<FuelHistoryEntry> listHistory();

        void save(
                int vehicleId,
                LocalDate date,
                String litres,
                String pricePerLitre,
                String odometer,
                boolean fullTank
        );
    }

    interface FormAccess {
        LocalDate date();

        void setDate(LocalDate date);

        Vehicle selectedVehicle();

        String litres();

        String pricePerLitre();

        String odometer();

        boolean fullTank();

        void setTotalCost(String totalCost);

        void showData(ScreenData screenData);

        void clearNumericInputs();

        void setStatus(String status);
    }

    record ScreenData(
            List<Vehicle> vehicles,
            List<FuelHistoryEntry> history,
            Vehicle selectedVehicle
    ) {
        ScreenData {
            vehicles = List.copyOf(vehicles);
            history = List.copyOf(history);
        }
    }

    private static final class ServiceOperations implements FuelLogOperations {
        private final FuelLogService service;

        private ServiceOperations(FuelLogService service) {
            this.service = Objects.requireNonNull(service, "Fuel log service is required");
        }

        @Override
        public List<Vehicle> listAssignedVehicles() {
            return service.listAssignedVehicles();
        }

        @Override
        public List<FuelHistoryEntry> listHistory() {
            return service.listHistory();
        }

        @Override
        public void save(
                int vehicleId,
                LocalDate date,
                String litres,
                String pricePerLitre,
                String odometer,
                boolean fullTank
        ) {
            service.save(vehicleId, date, litres, pricePerLitre, odometer, fullTank);
        }
    }

    private final class JavaFxForm implements FormAccess {
        @Override
        public LocalDate date() {
            return datePicker.getValue();
        }

        @Override
        public void setDate(LocalDate date) {
            datePicker.setValue(date);
        }

        @Override
        public Vehicle selectedVehicle() {
            return vehicleComboBox.getValue();
        }

        @Override
        public String litres() {
            return litresField.getText();
        }

        @Override
        public String pricePerLitre() {
            return pricePerLitreField.getText();
        }

        @Override
        public String odometer() {
            return odometerField.getText();
        }

        @Override
        public boolean fullTank() {
            return fullTankCheckBox.isSelected();
        }

        @Override
        public void setTotalCost(String totalCost) {
            totalCostLabel.setText(totalCost);
        }

        @Override
        public void showData(ScreenData screenData) {
            historyTable.setItems(FXCollections.observableArrayList(screenData.history()));
            vehicleComboBox.setItems(FXCollections.observableArrayList(screenData.vehicles()));
            vehicleComboBox.setValue(screenData.selectedVehicle());
        }

        @Override
        public void clearNumericInputs() {
            litresField.clear();
            pricePerLitreField.clear();
            odometerField.clear();
        }

        @Override
        public void setStatus(String status) {
            statusLabel.setText(status);
        }
    }
}

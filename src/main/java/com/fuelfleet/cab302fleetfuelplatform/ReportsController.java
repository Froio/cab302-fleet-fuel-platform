package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.service.ReportingService;
import com.fuelfleet.cab302fleetfuelplatform.service.VehicleService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.util.Locale;

public final class ReportsController {
    @FXML private ComboBox<Vehicle> vehicle;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label summary;
    @FXML private Label status;
    @FXML private LineChart<String,Number> costChart;
    @FXML private LineChart<String,Number> emissionsChart;
    @FXML private LineChart<String,Number> efficiencyChart;
    private final ReportingService reporting = new ReportingService();

    @FXML private void initialize() {
        try {
            vehicle.getItems().setAll(new VehicleService().listVehicles());
            onRefresh();
        } catch (RuntimeException exception) { status.setText(exception.getMessage()); }
    }
    @FXML private void onAllVehicles() { vehicle.getSelectionModel().clearSelection(); onRefresh(); }
    @FXML private void onBack() { HelloApplication.switchScene("manager-dashboard.fxml"); }
    @FXML private void onRefresh() {
        costChart.getData().clear();
        emissionsChart.getData().clear();
        efficiencyChart.getData().clear();
        summary.setText("");
        try {
            var selected = vehicle.getValue();
            var report = reporting.load(selected == null ? null : selected.id(), date(fromDate), date(toDate));
            if (report.months().isEmpty()) {
                status.setText("No valid fuel records for this selection. Record fill-ups first."
                        + invalidMessage(report.invalidRows()));
                return;
            }
            var costs = series("Fuel expenditure");
            // Separate series around missing values to avoid drawing a line through unavailable data.
            XYChart.Series<String,Number> emissions = null;
            XYChart.Series<String,Number> efficiency = null;
            double litres = 0, cost = 0, co2 = 0;
            int unknown = 0, efficiencyPoints = 0;
            for (var month : report.months()) {
                String label = month.month().toString();
                costs.getData().add(new XYChart.Data<>(label,month.cost()));
                if (month.unestimatedEntries() == 0) {
                    if (emissions == null) { emissions = series("Estimated CO₂"); emissionsChart.getData().add(emissions); }
                    emissions.getData().add(new XYChart.Data<>(label,month.co2Kg()));
                } else { emissions = null; }
                if (month.efficiency().isPresent()) {
                    if (efficiency == null) { efficiency = series("Tank-to-tank efficiency"); efficiencyChart.getData().add(efficiency); }
                    efficiency.getData().add(new XYChart.Data<>(label,month.efficiency().getAsDouble()));
                    efficiencyPoints++;
                } else { efficiency = null; }
                litres += month.litres(); cost += month.cost(); co2 += month.co2Kg(); unknown += month.unestimatedEntries();
            }
            costChart.getData().add(costs);
            summary.setText(String.format(Locale.ROOT,"Fuel: %,.2f L   |   Cost: $%,.2f AUD   |   Estimated CO₂: %,.2f kg%s",
                    litres,cost,co2,unknown > 0 ? " (known fuels only; incomplete)" : ""));
            status.setText((unknown > 0 ? unknown + " entries have unsupported/unknown fuel; affected CO₂ months are omitted. " : "")
                    + (efficiencyPoints == 0 ? "Efficiency needs two full-tank readings with increasing odometers. " : "")
                    + invalidMessage(report.invalidRows()));
        } catch (RuntimeException exception) {
            status.setText("Cannot load report: " + exception.getMessage());
        }
    }
    private static String invalidMessage(int count) {
        return count == 0 ? "" : " " + count + " invalid stored records excluded across the selected vehicle history. Correct them before relying on this report.";
    }
    private static LocalDate date(DatePicker picker) {
        String text = picker.getEditor().getText();
        if (text == null || text.isBlank()) { picker.setValue(null); return null; }
        try {
            LocalDate value = picker.getConverter().fromString(text);
            picker.setValue(value);
            return value;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Enter a valid date using the date picker.");
        }
    }
    private static XYChart.Series<String,Number> series(String name) {
        var series = new XYChart.Series<String,Number>(); series.setName(name); return series;
    }
}

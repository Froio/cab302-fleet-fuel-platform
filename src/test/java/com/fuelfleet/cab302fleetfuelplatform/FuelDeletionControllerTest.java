package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.*;
import com.fuelfleet.cab302fleetfuelplatform.db.*;
import com.fuelfleet.cab302fleetfuelplatform.model.*;
import com.fuelfleet.cab302fleetfuelplatform.service.FuelLogService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FuelDeletionControllerTest {
    @TempDir Path directory;
    ConnectionProvider db;
    FuelLogService service;
    FuelLoggingController controller;
    TestForm form;
    Vehicle vehicle;
    AppSession session;

    @BeforeEach void setup() throws Exception {
        db = DBManager.provider("jdbc:sqlite:" + directory.resolve("controller.db"));
        DBManager.initialize(db);
        User driver = new UserDao(db).insert("driver", "unused", Role.DRIVER);
        session = new AppSession(); session.signIn(driver);
        VehicleDao vehicles = new VehicleDao(db);
        vehicle = vehicles.insert("CAR1", "Make", "Model", "Petrol", 0);
        vehicles.assignDriver(vehicle.id(), driver.id());
        service = new FuelLogService(new FuelLogDao(db), vehicles, session);
        service.save(vehicle.id(), LocalDate.of(2026, 10, 1), "10", "2", "100", true);
        form = new TestForm();
        controller = new FuelLoggingController(new Operations(), session,
                target -> fail("Unexpected navigation to " + target), form);
        controller.initialize();
        form.selected = form.data.history().getFirst();
    }

    @Test void cancellationKeepsSelectedLogAndCurrentReading() {
        form.confirm = false;
        delete();
        assertEquals(1, service.listHistory().size());
        assertTrue(form.askedForConfirmation);
    }

    @Test void confirmedDeletionRefreshesHistoryAndPreservesOdometer() {
        form.confirm = true;
        delete();
        assertTrue(service.listHistory().isEmpty());
        assertTrue(form.data.history().isEmpty());
        assertEquals(100, new VehicleDao(db).findById(vehicle.id()).orElseThrow().currentOdometer());
        assertTrue(form.status.toLowerCase().contains("deleted"));
    }

    @Test void missingSelectionPromptsWithoutDeleting() {
        form.selected = null;
        delete();
        assertTrue(form.status.toLowerCase().contains("select"));
        assertEquals(1, service.listHistory().size());
        assertFalse(form.askedForConfirmation);
    }

    @Test void deniedDeletionShowsReasonAndKeepsDriverSignedIn() throws Exception {
        try (var c = db.open(); var s = c.createStatement()) {
            s.executeUpdate("UPDATE fuel_logs SET created_by_user_id=NULL");
        }
        form.confirm = true;
        delete();
        assertTrue(form.status.toLowerCase().contains("author"));
        assertTrue(session.currentUser().isPresent());
        assertEquals(1, service.listHistory().size());
    }

    @Test void skippedRowsDisplayPersistentWarning() throws Exception {
        try (var c = db.open(); var s = c.createStatement()) {
            s.executeUpdate("UPDATE fuel_logs SET cost='broken'");
        }
        controller.initialize();
        assertTrue(form.warning.contains("1"));
        assertTrue(form.warning.toLowerCase().contains("unavailable"));
        assertTrue(form.data.history().isEmpty());
    }

    private void delete() {
        controller.onDelete();
    }

    private class Operations implements FuelLoggingController.FuelLogOperations {
        public List<Vehicle> listAssignedVehicles() { return service.listAssignedVehicles(); }
        public List<FuelHistoryEntry> listHistory() { return service.listHistory(); }
        public String exportHistoryToCsv() { return service.exportHistoryToCsv(); }
        public void save(int id, LocalDate date, String litres, String price, String odometer, boolean full) {
            service.save(id, date, litres, price, odometer, full);
        }
        public void delete(int id) { service.delete(id); }
        public int skippedHistoryRowCount() { return service.skippedHistoryRowCount(); }
    }

    private class TestForm implements FuelLoggingController.FormAccess {
        FuelLoggingController.ScreenData data;
        FuelHistoryEntry selected;
        boolean confirm;
        boolean askedForConfirmation;
        String status = "";
        String warning = "";
        public LocalDate date() { return LocalDate.now(); }
        public void setDate(LocalDate value) { }
        public Vehicle selectedVehicle() { return vehicle; }
        public String litres() { return ""; }
        public String pricePerLitre() { return ""; }
        public String odometer() { return ""; }
        public boolean fullTank() { return true; }
        public void setTotalCost(String total) { }
        public void showData(FuelLoggingController.ScreenData value) { data = value; }
        public void clearNumericInputs() { }
        public void setStatus(String value) { status = value; }
        public void setHistoryWarning(String value) { warning = value; }
        public FuelHistoryEntry selectedHistoryEntry() { return selected; }
        public boolean confirmDeletion(FuelHistoryEntry entry) { askedForConfirmation = true; return confirm; }
    }
}

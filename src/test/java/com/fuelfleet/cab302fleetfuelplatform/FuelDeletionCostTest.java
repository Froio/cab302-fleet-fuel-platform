package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.*;
import com.fuelfleet.cab302fleetfuelplatform.db.*;
import com.fuelfleet.cab302fleetfuelplatform.exception.*;
import com.fuelfleet.cab302fleetfuelplatform.model.*;
import com.fuelfleet.cab302fleetfuelplatform.service.FuelLogService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.OptionalDouble;
import static org.junit.jupiter.api.Assertions.*;

class FuelDeletionCostTest {
    @TempDir Path directory;
    ConnectionProvider db;
    VehicleDao vehicles;
    FuelLogDao logs;
    FuelLogService service;
    AppSession session;
    User driver;
    Vehicle vehicle;
    final LocalDate day = LocalDate.of(2026, 10, 1);

    @BeforeEach void setup() throws Exception {
        db = DBManager.provider("jdbc:sqlite:" + directory.resolve("fuel.db"));
        DBManager.initialize(db);
        driver = new UserDao(db).insert("driver", "unused-test-password", Role.DRIVER);
        session = new AppSession(); session.signIn(driver);
        vehicles = new VehicleDao(db); logs = new FuelLogDao(db);
        vehicle = vehicles.insert("CAR1", "Make", "Model", "Petrol", 0);
        vehicles.assignDriver(vehicle.id(), driver.id());
        service = new FuelLogService(logs, vehicles, session);
    }

    @Test void completeIntervalCostIncludesAllPartialFillsButNotBaselineFill() {
        save(0, "100", "10", "9", true);
        save(1, "150", "10", "2", false);
        save(2, "200", "20", "2", true);
        var history = service.listHistory();
        assertEquals(0.60, cost(history.getFirst()).orElseThrow(), 0.000001);
        assertEquals(30.0, history.getFirst().litresPer100Km().orElseThrow(), 0.000001);
        assertTrue(cost(history.get(1)).isEmpty());
        assertTrue(cost(history.get(2)).isEmpty());
    }

    @Test void deletingOwnLogRecalculatesIntervalsWithoutReducingVehicleOdometer() {
        save(0, "100", "10", "2", true);
        FuelLog partial = save(1, "150", "10", "2", false);
        FuelLog latest = save(2, "200", "20", "2", true);
        delete(partial.id());
        assertEquals(2, service.listHistory().size());
        assertEquals(0.40, cost(service.listHistory().getFirst()).orElseThrow(), 0.000001);
        delete(latest.id());
        assertEquals(200, vehicles.findById(vehicle.id()).orElseThrow().currentOdometer());
        assertTrue(cost(service.listHistory().getFirst()).isEmpty());
    }

    @Test void anotherDriverCannotDeleteEvenWhenVehicleHasBeenReassignedToThem() {
        FuelLog own = save(0, "100", "10", "2", true);
        User other = new UserDao(db).insert("other", "unused", Role.DRIVER);
        vehicles.assignDriver(vehicle.id(), other.id());
        session.signIn(other);
        assertThrows(AuthorizationException.class, () -> delete(own.id()));
        assertEquals(1, service.listHistory().size());
    }

    @Test void originalAuthorCannotDeleteAfterLosingAssignment() {
        FuelLog own = save(0, "100", "10", "2", true);
        vehicles.assignDriver(vehicle.id(), null);
        assertThrows(AuthorizationException.class, () -> delete(own.id()));
        assertEquals(1, logs.listByVehicleIds(java.util.List.of(vehicle.id())).size());
    }

    @Test void legacyUnknownAuthorIsNotGuessedAsTheAssignedDriver() {
        FuelLog legacy = logs.save(vehicle.id(), day, 10, 20, 100, "Petrol", true);
        var failure = assertThrows(AuthorizationException.class, () -> delete(legacy.id()));
        assertTrue(failure.getMessage().toLowerCase().contains("author"));
        assertEquals(1, service.listHistory().size());
    }

    @Test void deleteRequiresSignedInDriver() {
        FuelLog own = save(0, "100", "10", "2", true);
        session.signOut();
        assertThrows(AuthorizationException.class, () -> delete(own.id()));
        session.signIn(new User(999, "manager", Role.MANAGER));
        assertThrows(AuthorizationException.class, () -> delete(own.id()));
    }

    @Test void corruptHistoryHasVisibleSkippedCountAndNoCostMetric() throws Exception {
        save(0, "100", "10", "2", true);
        save(1, "150", "10", "2", false);
        save(2, "200", "20", "2", true);
        try (var c = db.open(); var s = c.createStatement()) {
            s.executeUpdate("UPDATE fuel_logs SET cost='corrupt' WHERE odometer=150");
        }
        var history = service.listHistory();
        assertEquals(2, history.size());
        assertEquals(1, service.skippedHistoryRowCount());
        assertTrue(history.stream().allMatch(entry -> cost(entry).isEmpty()));
    }

    @Test void overflowingIntervalCostIsUnavailableInsteadOfInfinite() {
        save(0, "100", "1", "1", true);
        save(1, "150", "1", "1.0E308", false);
        save(2, "200", "1", "1.0E308", true);
        assertTrue(service.listHistory().getFirst().costPerKm().isEmpty());
    }

    @Test void overflowingIntervalFuelDoesNotCrashHistory() {
        save(0, "100", "1", "0", true);
        save(1, "150", "1.0E308", "0", false);
        save(2, "200", "1.0E308", "0", true);
        var history = assertDoesNotThrow(service::listHistory);
        assertTrue(history.getFirst().litresPer100Km().isEmpty());
    }

    private FuelLog save(int offset, String odometer, String litres, String price, boolean full) {
        return service.save(vehicle.id(), day.plusDays(offset), litres, price, odometer, full);
    }

    private void delete(int id) {
        service.delete(id);
    }

    private OptionalDouble cost(FuelHistoryEntry entry) {
        return entry.costPerKm();
    }
}

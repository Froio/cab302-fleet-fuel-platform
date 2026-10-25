package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.*;
import com.fuelfleet.cab302fleetfuelplatform.db.*;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.*;
import com.fuelfleet.cab302fleetfuelplatform.service.*;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class FuelIntegrityTest {
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

    @Test void saveCannotRollbackAnAdvancedVehicleReadingEvenWhenLogsExist() {
        logs.save(vehicle.id(), day, 20, 40, 100, "Petrol", true);
        vehicles.update(vehicle.id(), "CAR1", "Make", "Model", "Petrol", 500);
        assertThrows(ValidationException.class,
                () -> service.save(vehicle.id(), day.plusDays(1), "20", "2", "200", true));
        assertEquals(500, vehicles.findById(vehicle.id()).orElseThrow().currentOdometer());
    }

    @Test void daoCannotRollbackAnAdvancedVehicleReading() {
        logs.save(vehicle.id(), day, 20, 40, 100, "Petrol", true);
        vehicles.update(vehicle.id(), "CAR1", "Make", "Model", "Petrol", 500);
        assertThrows(ValidationException.class,
                () -> logs.save(vehicle.id(), day.plusDays(1), 20, 40, 200, "Petrol", true));
        assertEquals(1, logs.listByVehicleIds(java.util.List.of(vehicle.id())).size());
    }

    @Test void daoRejectsEarlierDateWithLaterOdometerAtomically() {
        logs.save(vehicle.id(), day.plusDays(1), 20, 40, 100, "Petrol", true);
        assertThrows(ValidationException.class,
                () -> logs.save(vehicle.id(), day, 20, 40, 200, "Petrol", true));
        assertEquals(100, vehicles.findById(vehicle.id()).orElseThrow().currentOdometer());
        assertEquals(1, logs.listByVehicleIds(java.util.List.of(vehicle.id())).size());
    }

    @Test void managerCannotEditOdometerBelowFuelHistoryMaximum() {
        logs.save(vehicle.id(), day, 20, 40, 100, "Petrol", true);
        session.signIn(new User(999, "manager", Role.MANAGER));
        assertThrows(ValidationException.class, () -> new VehicleService(vehicles, session)
                .updateVehicle(vehicle.id(), "CAR1", "Make", "Model", "Petrol", "99"));
        assertEquals(100, vehicles.findById(vehicle.id()).orElseThrow().currentOdometer());
    }

    @Test void csvEscapesRegistrationQuotesCommasAndUsesDecimalPoint() {
        vehicles.update(vehicle.id(), "A,\"B\"", "Make", "Model", "Petrol", 0);
        logs.save(vehicle.id(), day, 20, 40, 100, "Petrol", true);
        logs.save(vehicle.id(), day.plusDays(1), 20, 40, 200, "Petrol", true);
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            String csv = service.exportHistoryToCsv();
            assertTrue(csv.contains("\"A,\"\"B\"\"\""), csv);
            assertTrue(csv.contains(",20.00"), csv);
            assertFalse(csv.contains(",20,00"), csv);
        } finally { Locale.setDefault(original); }
    }

    @Test void malformedStoredRowDoesNotCrashOrBridgeEfficiencyAcrossMissingFuel() throws Exception {
        logs.save(vehicle.id(), day, 20, 40, 100, "Petrol", true);
        logs.save(vehicle.id(), day.plusDays(1), 10, 20, 150, "Petrol", false);
        logs.save(vehicle.id(), day.plusDays(2), 20, 40, 200, "Petrol", true);
        try (var c = db.open(); var s = c.createStatement()) {
            s.executeUpdate("UPDATE fuel_logs SET date='invalid-date' WHERE odometer=150");
        }
        var history = assertDoesNotThrow(service::listHistory);
        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(entry -> entry.litresPer100Km().isEmpty()));
    }
}

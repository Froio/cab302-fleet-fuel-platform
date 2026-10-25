package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.FuelLogDao;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.service.UserService;
import com.fuelfleet.cab302fleetfuelplatform.service.MaintenanceService;
import com.fuelfleet.cab302fleetfuelplatform.service.MaintenanceStatus;
import com.fuelfleet.cab302fleetfuelplatform.service.FuelLogService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MergeCompatibilityTest {
    @TempDir Path directory;

    @Test void standaloneRevisedProfileMigratesToTeamSchemaOnlyOnce() throws Exception {
        var db = DBManager.provider("jdbc:sqlite:" + directory.resolve("revised-profile.db"));
        try (var connection = db.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE users(id INTEGER PRIMARY KEY, username TEXT, password TEXT, role TEXT, name TEXT, contact_email TEXT)");
            statement.executeUpdate("INSERT INTO users VALUES(1,'driver','original-hash','driver','Saved Driver','saved@example.test')");
        }
        DBManager.initialize(db);
        var users = new UserDao(db);
        assertEquals("Saved Driver", users.findProfileById(1).orElseThrow().fullName());
        assertTrue(users.updateProfile(1, "Updated Driver", "updated@example.test"));
        DBManager.initialize(db);
        assertEquals("Updated Driver", users.findProfileById(1).orElseThrow().fullName());
        assertEquals("updated@example.test", users.findProfileById(1).orElseThrow().contactEmail());
        assertEquals("original-hash", users.findCredentialsByUsername("driver").orElseThrow().encodedPassword());
    }

    @Test void existingCanonicalProfileIsNotOverwrittenByStaleNameColumn() throws Exception {
        var db = DBManager.provider("jdbc:sqlite:" + directory.resolve("both-profile-columns.db"));
        try (var connection = db.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE users(id INTEGER PRIMARY KEY, username TEXT, password TEXT, role TEXT, full_name TEXT, name TEXT, contact_email TEXT)");
            statement.executeUpdate("INSERT INTO users VALUES(1,'manager','hash','manager','Current Name','Stale Name','current@example.test')");
        }
        DBManager.initialize(db);
        DBManager.initialize(db);
        assertEquals("Current Name", new UserDao(db).findProfileById(1).orElseThrow().fullName());
    }

    @Test void unknownLegacyRoleIsNotReplacedDuringCombinedMigration() throws Exception {
        var db = DBManager.provider("jdbc:sqlite:" + directory.resolve("unknown-role.db"));
        try (var connection = db.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE users(id INTEGER PRIMARY KEY, username TEXT, password TEXT, role TEXT)");
            statement.executeUpdate("INSERT INTO users VALUES(1,'unknown','hash',NULL)");
        }
        DBManager.initialize(db);
        try (var connection = db.open(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT role FROM users WHERE id=1")) {
            assertTrue(rows.next());
            assertNull(rows.getString("role"));
        }
    }

    @Test void bothProfileApisShareTheSameSavedDataAndValidation() throws Exception {
        var db = DBManager.provider("jdbc:sqlite:" + directory.resolve("profile-apis.db"));
        DBManager.initialize(db);
        var users = new UserDao(db);
        var manager = users.insert("manager", "hash", Role.MANAGER);
        var session = new AppSession();
        session.signIn(manager);
        var service = new UserService(users, new PasswordHasher(), session);
        service.updateMyProfile("Team Profile", "team@example.test");
        assertEquals("Team Profile", service.currentProfile().name());
        service.updateCurrentProfile("  Revised Profile  ", " revised@example.test ");
        assertEquals("Revised Profile", service.getMyProfile().fullName());
        assertEquals("revised@example.test", service.getMyProfile().contactEmail());
        assertThrows(ValidationException.class,
                () -> service.updateCurrentProfile("N".repeat(101), "valid@example.test"));
        DBManager.initialize(db);
        assertEquals("Revised Profile", service.currentProfile().name());
    }

    @Test void fuelSaveDeleteAndRejectedVehicleEditPreserveMaintenanceSchedule() throws Exception {
        var db = DBManager.provider("jdbc:sqlite:" + directory.resolve("maintenance-fuel.db"));
        DBManager.initialize(db);
        var users = new UserDao(db);
        var manager = users.insert("manager", "hash", Role.MANAGER);
        var driver = users.insert("driver", "hash", Role.DRIVER);
        var vehicles = new VehicleDao(db);
        var vehicle = vehicles.insert("MERGE-ONE", "Make", "Model", "Petrol", 0);
        vehicles.assignDriver(vehicle.id(), driver.id());
        var session = new AppSession();
        session.signIn(manager);
        var maintenance = new MaintenanceService(vehicles, session);
        maintenance.scheduleService(vehicle.id(), 250);
        session.signIn(driver);
        var fuel = new FuelLogService(new FuelLogDao(db), vehicles, session);
        var log = fuel.save(vehicle.id(), LocalDate.now().minusDays(1), "20", "2", "300", true);
        assertEquals(MaintenanceStatus.OVERDUE, maintenance.getMaintenanceStatus(vehicle.id()));
        assertThrows(ValidationException.class,
                () -> vehicles.update(vehicle.id(), "MERGE-ONE", "Make", "Model", "Petrol", 299));
        fuel.delete(log.id());
        DBManager.initialize(db);
        assertEquals(250, maintenance.getNextServiceOdometerForCurrentDriver(vehicle.id()));
        assertEquals(300, vehicles.findById(vehicle.id()).orElseThrow().currentOdometer());
        assertEquals(MaintenanceStatus.OVERDUE, maintenance.getMaintenanceStatus(vehicle.id()));
    }
}

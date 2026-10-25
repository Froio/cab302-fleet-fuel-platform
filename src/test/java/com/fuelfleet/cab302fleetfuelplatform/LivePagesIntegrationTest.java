package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import com.fuelfleet.cab302fleetfuelplatform.service.UserService;
import com.fuelfleet.cab302fleetfuelplatform.service.FleetDirectoryService;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LivePagesIntegrationTest {
    @TempDir Path directory;

    @Test void legacyProfileMigrationIsRepeatableAndPreservesCredentialsAndRole() throws Exception {
        ConnectionProvider db = DBManager.provider("jdbc:sqlite:" + directory.resolve("legacy.db"));
        try (var connection = db.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE users(id INTEGER PRIMARY KEY, username TEXT, password TEXT, role TEXT)");
            statement.executeUpdate("INSERT INTO users VALUES(7, 'legacy', 'unchanged-hash', '  MANAGER  ')");
        }
        DBManager.initialize(db);
        DBManager.initialize(db);
        try (var connection = db.open(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM users WHERE id=7")) {
            assertTrue(rows.next());
            assertEquals("unchanged-hash", rows.getString("password"));
            assertEquals("  MANAGER  ", rows.getString("role"));
            assertDoesNotThrow(() -> assertEquals("", rows.getString("full_name")), "Legacy users need blank profile columns");
            assertEquals("", rows.getString("contact_email"));
        }
    }

    @Test void supportRoutesRejectDriversAndAnonymousUsersButProfileAcceptsEitherSignedInRole() {
        User driver = new User(1, "driver", Role.DRIVER);
        User manager = new User(2, "manager", Role.MANAGER);
        for (String route : new String[]{"driver-list.fxml", "unassigned-vehicles.fxml"}) {
            assertEquals("login-view.fxml", ViewAccessPolicy.resolve(route, Optional.empty()));
            assertEquals("login-view.fxml", ViewAccessPolicy.resolve(route, Optional.of(driver)));
            assertEquals(route, ViewAccessPolicy.resolve(route, Optional.of(manager)));
        }
        assertEquals("login-view.fxml", ViewAccessPolicy.resolve("profile-edit.fxml", Optional.empty()));
        assertEquals("profile-edit.fxml", ViewAccessPolicy.resolve("profile-edit.fxml", Optional.of(driver)));
        assertEquals("profile-edit.fxml", ViewAccessPolicy.resolve("profile-edit.fxml", Optional.of(manager)));
    }

    @Test void profileMigrationDoesNotInventRolesForIncompleteLegacyAccounts() throws Exception {
        var db = DBManager.provider("jdbc:sqlite:" + directory.resolve("incomplete-users.db"));
        try (var connection = db.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE users(id INTEGER PRIMARY KEY,username TEXT,password TEXT,role TEXT)");
            statement.executeUpdate("INSERT INTO users VALUES(1,'missing','hash',NULL),(2,'blank','hash','  ')");
        }
        DBManager.initialize(db); DBManager.initialize(db);
        try (var connection = db.open(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT role FROM users ORDER BY id")) {
            assertTrue(rows.next()); assertNull(rows.getString("role"));
            assertTrue(rows.next()); assertEquals("  ", rows.getString("role"));
        }
    }

    @Test void fuelAuthorMigrationLeavesLegacyAuthorUnknownAndPreservesHistory() throws Exception {
        var db = DBManager.provider("jdbc:sqlite:" + directory.resolve("fuel-legacy.db"));
        DBManager.initialize(db);
        try (var connection = db.open(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE fuel_logs");
            statement.executeUpdate("INSERT INTO vehicles(id,registration,make,model) VALUES(1,'LEGACY','Make','Model')");
            statement.executeUpdate("CREATE TABLE fuel_logs(id INTEGER PRIMARY KEY,vehicle_id INTEGER,date TEXT,litres REAL,cost REAL,odometer REAL)");
            statement.executeUpdate("INSERT INTO fuel_logs VALUES(9,1,'2026-10-01',20,40,100)");
        }
        DBManager.initialize(db); DBManager.initialize(db);
        try (var connection = db.open(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM fuel_logs WHERE id=9")) {
            assertTrue(rows.next());
            assertEquals(40, rows.getDouble("cost"));
            assertDoesNotThrow(() -> assertNull(rows.getObject("created_by_user_id")), "Historical author must remain unknown");
        }
    }

    @Test void liveDirectoryIncludesDriversWithMultipleVehiclesAndRefreshesAfterReassignment() throws Exception {
        var db = DBManager.provider("jdbc:sqlite:" + directory.resolve("directory.db"));
        DBManager.initialize(db);
        var users = new UserDao(db);
        var vehicles = new VehicleDao(db);
        var manager = users.insert("manager", "hash", Role.MANAGER);
        var driver = users.insert("driver", "hash", Role.DRIVER);
        var available = users.insert("available", "hash", Role.DRIVER);
        var a = vehicles.insert("A1", "Make", "One", "Petrol", 0);
        var b = vehicles.insert("B2", "Make", "Two", "Petrol", 0);
        var c = vehicles.insert("C3", "Make", "Three", "Petrol", 0);
        vehicles.assignDriver(a.id(), driver.id());
        vehicles.assignDriver(b.id(), driver.id());
        var session = new AppSession(); session.signIn(manager);
        var service = new FleetDirectoryService(users, vehicles, session);
        var rows = service.listDrivers();
        assertEquals(2, rows.size());
        var assigned = rows.stream().filter(row -> row.driver().id() == driver.id()).findFirst().orElseThrow();
        assertEquals(List.of("A1", "B2"), assigned.vehicles().stream().map(Vehicle::registration).toList());
        assertTrue(assigned.vehicles().stream().allMatch(vehicle -> vehicle.assignedDriverId() == driver.id()));
        var unassigned = rows.stream().filter(row -> row.driver().id() == available.id()).findFirst().orElseThrow();
        assertEquals(List.of(), unassigned.vehicles());
        assertEquals(List.of(c), service.listVehicles(true));
        assertEquals(3, service.listVehicles(false).size());
        vehicles.assignDriver(c.id(), driver.id());
        assertEquals(List.of(), service.listVehicles(true));
        assertEquals(3, service.listDrivers().stream().filter(row -> row.driver().id() == driver.id()).findFirst().orElseThrow().vehicles().size());
        session.signIn(driver);
        assertThrows(AuthorizationException.class, service::listDrivers);
        assertThrows(AuthorizationException.class, () -> service.listVehicles(false));
        session.signOut();
        assertThrows(AuthorizationException.class, service::listDrivers);
    }

    @Test void profilePersistsForCurrentUserOnlyAndDoesNotAlterCredentials() throws Exception {
        var db = DBManager.provider("jdbc:sqlite:" + directory.resolve("profile.db"));
        DBManager.initialize(db);
        var users = new UserDao(db);
        var driver = users.insert("driver", "original-hash", Role.DRIVER);
        var other = users.insert("other", "other-hash", Role.MANAGER);
        var session = new AppSession(); session.signIn(driver);
        var service = new UserService(users, new PasswordHasher(), session);
        assertEquals("", service.currentProfile().name());
        service.updateCurrentProfile("  Driver Name  ", " driver@example.com ");
        DBManager.initialize(db);
        var reopened = new UserService(new UserDao(db), new PasswordHasher(), session);
        var profile = reopened.currentProfile();
        assertEquals("Driver Name", profile.name());
        assertEquals("driver@example.com", profile.contactEmail());
        assertEquals("original-hash", users.findCredentialsByUsername("driver").orElseThrow().encodedPassword());
        assertEquals(Role.DRIVER, users.findById(driver.id()).orElseThrow().role());
        assertThrows(ValidationException.class, () -> service.updateCurrentProfile("Name", "a@b@c"));
        session.signIn(other);
        assertEquals("", service.currentProfile().name());
        service.updateCurrentProfile("Manager Name", "manager@example.com");
        assertEquals("Manager Name", service.currentProfile().name());
        session.signIn(driver);
        assertEquals("Driver Name", service.currentProfile().name());
        session.signOut();
        assertThrows(AuthorizationException.class, service::currentProfile);
        assertThrows(AuthorizationException.class, () -> service.updateCurrentProfile("Name", "a@b.com"));
    }
}

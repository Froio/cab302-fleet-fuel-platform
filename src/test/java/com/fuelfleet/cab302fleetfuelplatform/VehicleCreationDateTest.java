package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class VehicleCreationDateTest {

    @TempDir
    Path directory;

    ConnectionProvider connections;
    VehicleDao vehicleDao;
    VehicleService vehicleService;
    AppSession session;

    @BeforeEach
    void setup() throws Exception{
        connections = DBManager.provider(
                "jdbc:sqlite:" + directory.resolve("vehicle-date-test.db")

        );

        DBManager.initialize(connections);

        vehicleDao = new VehicleDao(connections);
        session = new AppSession();

        session.signIn(new User(1, "testmanager", Role.MANAGER));

        vehicleService = new VehicleService(vehicleDao, session);

    }

    @Test
    void newVehicleGetsCreationDateAutomatically(){

        Vehicle vehicle = vehicleService.createVehicle(
                "DATE001", "Toyota", "Corolla", "Petrol", "1000"
        );

        assertNotNull(vehicle.createdAt());
        assertFalse(vehicle.createdAt().isBlank());

        assertDoesNotThrow(() -> Instant.parse(vehicle.createdAt()));
    }

    @Test
    void creationDatePersistsAfterDatabaseReload() {

        Vehicle created = vehicleService.createVehicle(
                "DATE002", "Honda", "Civic", "Petrol", "2000"
        );

        Vehicle reloaded = vehicleDao.findById(created.id())
                .orElseThrow();

        assertEquals(created.createdAt(), reloaded.createdAt());
    }

    @Test
    void editingVehicleDoesNotChangeCreationDate() {

        Vehicle created = vehicleService.createVehicle(
                "DATE003", "Ford", "Focus", "Petrol", "3000"
        );

        String originalDate = created.createdAt();

        vehicleService.updateVehicle(
                created.id(),
                "DATE003",
                "Ford",
                "Focus",
                "Petrol",
                "5000"
        );

        Vehicle updated = vehicleDao.findById(created.id())
                .orElseThrow();

        assertEquals(originalDate, updated.createdAt());
        assertEquals(5000L, updated.currentOdometer());

    }

    @Test
    void missingCreationDateDisplayUnknown() {

        Vehicle legacyVehicle = new Vehicle(
                1,
                "OLD001",
                "Toyota",
                "Camry",
                "Petrol",
                10000,
                null,
                null
        );

        assertEquals("Unkown", legacyVehicle.createdAtDisplay());
    }
}
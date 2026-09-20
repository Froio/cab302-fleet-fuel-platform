package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DuplicateRegistrationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;





class VehicleServiceTest {

    @TempDir
    Path directory;
    ConnectionProvider connections;
    AppSession session;
    VehicleService vehicleService;
    @BeforeEach
    void setup() throws Exception {
        connections = DBManager.provider("jdbc:sqlite:" + directory.resolve("test.db"));
        DBManager.initialize(connections);
        session = new AppSession();
      
        VehicleDao vehicleDao = new VehicleDao(connections);
        vehicleService = new VehicleService(vehicleDao, session);
    }


  
    @Test
    void testCreateVehicle() {
        session.signIn(new User(1, "manager", Role.MANAGER));
        var vehicle = vehicleService.createVehicle("L0B4N4", "Hyundai", "i30 N", "Petrol", "1000");
        assertEquals("L0B4N4", vehicle.registration());
    }    @Test
    void testCreateVehicleNoLoginThrowsError() {
        assertThrows(AuthorizationException.class, () -> {
        vehicleService.createVehicle("L0B4N4", "Hyundai", "i30 N", "Petrol", "1000");
        });
    }
    @Test
    void testCreateVehicleAsDriverThrowsError() {
        session.signIn(new User(2, "driver1", Role.DRIVER));
        assertThrows(AuthorizationException.class, () -> {
            vehicleService.createVehicle("L0B4N4", "Hyundai", "i30 N", "Petrol", "1000");
        });


      
    }

    @Test
    void testDuplicateRegoThrowsError() {
        session.signIn(new User(1, "manager", Role.MANAGER));
        vehicleService.createVehicle("L0B4N4", "Hyundai", "i30 N", "Petrol", "1000");

        assertThrows(DuplicateRegistrationException.class, () -> {
            vehicleService.createVehicle("l0b4n4", "Honda", "Civic", "Petrol", "500");
        });
    }    @Test
    void testMissingMakeThrowsError() {
        session.signIn(new User(1, "manager", Role.MANAGER));
        assertThrows(ValidationException.class, () -> {
            vehicleService.createVehicle("L0B4N4", "", "i30 N", "Petrol", "1000");
        });
    }

    @Test
  
  
  
  void testUpdateVehicle() {
        session.signIn(new User(1, "manager", Role.MANAGER));
        var vehicle = vehicleService.createVehicle("L0B4N4", "Hyundai", "i30 N", "Petrol", "1000");
        var updated = vehicleService.updateVehicle(vehicle.id(), "L0B4N4", "i30 N", "Hyundai", "Petrol", "2000");
        assertEquals("Hyundai", updated.make());
    }    @Test
    void testDeleteVehicle() {
        session.signIn(new User(1, "manager", Role.MANAGER));
        var vehicle = vehicleService.createVehicle("L0B4N4", "Hyundai", "i30 N", "Petrol", "1000");
        vehicleService.deleteVehicle(vehicle.id());
        assertTrue(vehicleService.listVehicles().isEmpty());
    }



  
}

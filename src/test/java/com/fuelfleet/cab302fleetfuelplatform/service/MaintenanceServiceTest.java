package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.UserDao;
import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceServiceTest {

    @TempDir
    Path directory;

    ConnectionProvider connections;
    VehicleDao vehicleDao;
    AppSession session;
    MaintenanceService maintenanceService;
    Vehicle vehicle;

    @BeforeEach
    void setup() throws Exception {
        connections = DBManager.provider(
                "jdbc:sqlite:" + directory.resolve("maintenance-test.db")
        );

        DBManager.initialize(connections);

        vehicleDao = new VehicleDao(connections);
        session = new AppSession();

        maintenanceService = new MaintenanceService(vehicleDao,session);

        vehicle = vehicleDao. insert(
                "MAINT123",
                "Toyota",
                "Corolla",
                "Petrol",
                45000
        );
    }

    @Test
    void managerCanScheduleMaintenance() {
        session.signIn(new User(1, "manager", Role.MANAGER));

        maintenanceService.scheduleService(vehicle.id(), 50000);

        assertEquals(
                50000L,
                vehicleDao.findNextServiceOdometer(vehicle.id()).orElseThrow()
        );
    }

    @Test
    void maintenanceSchedulePersistsInDatabase() {
        session.signIn(new User(1, "manager", Role.MANAGER));

        maintenanceService.scheduleService(vehicle.id(), 50000);

        VehicleDao anotherDao = new VehicleDao(
                DBManager.provider(
                        "jdbc:sqlite:" + directory.resolve("maintenance-test.db")
                )
                );
                assertEquals(
                        50000L,
                        anotherDao.findNextServiceOdometer(vehicle.id()).orElseThrow()
                );

    }

    @Test
    void driverCannotScheduleMaintenance() {
        session.signIn(new User(2, "driver", Role.DRIVER));

        assertThrows(
                AuthorizationException.class,
                () -> maintenanceService.scheduleService(vehicle.id(), 50000)
        );
    }

    @Test
    void loggedOutUserCannotScheduleMaintenance() {
        assertThrows(
                AuthorizationException.class,
                () -> maintenanceService.scheduleService(vehicle.id(), 50000)
        );
    }

    @Test
    void negativeServiceOdometerIsRejected() {
        session.signIn(new User(1, "manager", Role.MANAGER));

        assertThrows(
                ValidationException.class,
                () -> maintenanceService.scheduleService(vehicle.id(), -100)
        );
    }

    @Test
    void managerCanViewMaintenanceStatus() {
        session.signIn(new User(1, "manager", Role.MANAGER));

        maintenanceService.scheduleService(vehicle.id(), 45500);

        assertEquals(
                MaintenanceStatus.DUE_SOON,
                maintenanceService.getMaintenanceStatus(vehicle.id())
        );
    }

    @Test
    void driverCannotViewUnnassingedVehicle() {
        session.signIn(new User(2, "driver", Role.DRIVER));

        assertThrows(
                AuthorizationException.class,
        () -> maintenanceService.getMaintenanceStatus(vehicle.id())
        );

    }

    @Test
    void managerCannotScheduleMissingVehicle() {
        session.signIn(new User(1, "manager", Role.MANAGER));

        assertThrows(
                ValidationException.class,
                () -> maintenanceService.scheduleService(999999, 50000)
        );

    }

    @Test
    void unscheduledVehicleHasCorrectStatus() {
        session.signIn(new User(1, "manager", Role.MANAGER));

        assertEquals(
                MaintenanceStatus.NOT_SCHEDULED,
                maintenanceService.getMaintenanceStatus(vehicle.id())
        );
    }

    @Test
    void assingedDriverCanViewMaintenance() {
        UserDao userDao = new UserDao(connections);
        User driver = userDao.insert(
                "maintenanceDriver",
        "test-password-hash",
            Role.DRIVER
        );

        session.signIn(new User(1, "manager", Role.MANAGER));
        maintenanceService.scheduleService(vehicle.id(), 50000);

        vehicleDao.assignDriver(vehicle.id(), driver.id());

        session.signIn(driver);

        assertEquals(
                50000L,
                maintenanceService.getNextServiceOdometerForCurrentDriver(
                        vehicle.id()
                )
        );

        assertEquals(
                MaintenanceStatus.ON_TRACK,
                maintenanceService.getMaintenanceStatus(vehicle.id())
        );
    }

    @Test
    void unnassignedDriverCannotViewServiceReading() {
        session.signIn(new User(2, "driver", Role.DRIVER));

        assertThrows(
                AuthorizationException.class,
                () -> maintenanceService.getNextServiceOdometerForCurrentDriver(
                        vehicle.id()
                )
        );
    }

    @Test
    void statusChangesWhenOdometerIncreases() {
        session.signIn(new User(1, "manager", Role.MANAGER));

        maintenanceService.scheduleService(vehicle.id(), 50000);

        assertEquals(
                MaintenanceStatus.ON_TRACK,
                maintenanceService.getMaintenanceStatus(vehicle.id())
        );

        VehicleService service = new VehicleService(vehicleDao, session);

        service.updateVehicle(
                vehicle.id(),
                "MAINT123",
                "Toyota",
                "Corolla",
                "Petrol",
                "49500"

        );

        assertEquals(
                MaintenanceStatus.DUE_SOON,
                maintenanceService.getMaintenanceStatus(vehicle.id())
        );
    }

    @Test
    void managerCanClearMaintenanceSchedule() {
        session.signIn(new User(1, "manager", Role.MANAGER));

        maintenanceService.scheduleService(vehicle.id(), 50000);

        assertEquals(
                MaintenanceStatus.ON_TRACK,
                maintenanceService.getMaintenanceStatus(vehicle.id())
        );

        maintenanceService.clearServiceSchedule(vehicle.id());

        assertTrue(
                vehicleDao.findNextServiceOdometer(vehicle.id()).isEmpty()
        );

        assertEquals(
                MaintenanceStatus.NOT_SCHEDULED,
                maintenanceService.getMaintenanceStatus(vehicle.id())
        );
    }

    }

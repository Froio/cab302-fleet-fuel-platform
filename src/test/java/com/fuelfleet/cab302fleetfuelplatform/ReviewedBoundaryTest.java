package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.*;
import com.fuelfleet.cab302fleetfuelplatform.db.*;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.*;
import com.fuelfleet.cab302fleetfuelplatform.service.ReportingService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReviewedBoundaryTest {
    @TempDir Path directory;
    ConnectionProvider db;
    VehicleDao vehicles;
    Vehicle vehicle;

    @BeforeEach void setup() throws Exception {
        db=DBManager.provider("jdbc:sqlite:"+directory.resolve("review.db"));
        DBManager.initialize(db);
        vehicles=new VehicleDao(db);
        vehicle=vehicles.insert("REVIEW","Make","Model","Petrol",0);
    }

    private void corruptOdometerHistory() throws Exception {
        try(var connection=db.open();var statement=connection.prepareStatement("""
                INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type,full_tank)
                VALUES(?,?,20,40,500,'Petrol',1),(?,?,20,40,'broken','Petrol',1)
                """)) {
            statement.setInt(1,vehicle.id());statement.setString(2,LocalDate.now().minusDays(1).toString());
            statement.setInt(3,vehicle.id());statement.setString(4,LocalDate.now().minusDays(1).toString());
            statement.executeUpdate();
        }
    }

    @Test void malformedOdometerDoesNotBlockAnOtherwiseValidVehicleEdit() throws Exception {
        corruptOdometerHistory();
        assertTrue(assertDoesNotThrow(()->vehicles.update(vehicle.id(),"REVIEW","Updated","Model","Petrol",600)));
        assertEquals(600,vehicles.findById(vehicle.id()).orElseThrow().currentOdometer());
    }

    @Test void malformedTextCannotHideTheHighestValidFuelReading() throws Exception {
        corruptOdometerHistory();
        var fuel=new FuelLogDao(db);
        assertThrows(ValidationException.class,()->fuel.save(vehicle.id(),LocalDate.now(),20,40,450,"Petrol",true));
        assertEquals(0,vehicles.findById(vehicle.id()).orElseThrow().currentOdometer());
    }

    @Test void futureStoredDateIsUnavailableInBothReportAndComparison() throws Exception {
        try(var connection=db.open();var statement=connection.prepareStatement("""
                INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type,full_tank)
                VALUES(?,?,20,40,100,'Petrol',1)
                """)) {
            statement.setInt(1,vehicle.id());statement.setString(2,LocalDate.now().plusDays(1).toString());
            statement.executeUpdate();
        }
        var session=new AppSession();session.signIn(new User(1,"manager",Role.MANAGER));
        var reports=new ReportingService(new AnalyticsDao(db),vehicles,session);
        var report=reports.load(vehicle.id(),null,null);
        assertEquals(1,report.invalidRows());
        assertTrue(report.months().isEmpty());
        var comparison=reports.compareVehicleEmissions(vehicle.id(),null,null).getFirst();
        assertFalse(comparison.available());
        assertFalse(comparison.complete());
        assertEquals(1,comparison.invalidRows());
    }
}

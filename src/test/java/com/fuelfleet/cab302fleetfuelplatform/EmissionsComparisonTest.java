package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.AnalyticsDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.db.ConnectionProvider;
import com.fuelfleet.cab302fleetfuelplatform.db.DBManager;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.service.ReportingService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EmissionsComparisonTest {
    @TempDir Path directory;
    private ConnectionProvider connections;
    private AppSession session;
    private ReportingService service;

    @BeforeEach void setup() throws Exception {
        connections = DBManager.provider("jdbc:sqlite:" + directory.resolve("comparison.db"));
        DBManager.initialize(connections);
        session = new AppSession();
        session.signIn(new User(1,"manager", Role.MANAGER));
        service = new ReportingService(new AnalyticsDao(connections), new VehicleDao(connections), session);
        execute("INSERT INTO vehicles(id,registration,make,model) VALUES(1,'AAA','Test','Car'),(2,'BBB','Test','Van'),(3,'CCC','Test','Truck')");
    }

    @Test void unknownFuelCannotLookLikeCleanZeroEmissions() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'2026-09-01',10,20,1000,'Unknown')");
        assertTrue(Double.isNaN(row(1).totalEmissionsKg()));
    }

    @Test void vehicleWithoutFuelRecordsIsUnavailable() {
        assertTrue(Double.isNaN(row(1).totalEmissionsKg()));
    }

    @Test void malformedOnlyHistoryIsUnavailable() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'invalid',10,20,1000,'Petrol')");
        assertTrue(Double.isNaN(row(1).totalEmissionsKg()));
    }

    @Test void overflowCannotEscapeAsAnInfiniteEstimate() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'2026-09-01',4e307,1,1000,'Diesel'),(1,'2026-09-02',4e307,1,1001,'Diesel')");
        assertTrue(Double.isNaN(row(1).totalEmissionsKg()));
    }

    @Test void comparisonReadsLatestPersistedRecordsAndOrdersKnownTotalsDescending() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'2026-09-01',3.785411784,20,1000,'Petrol'),(2,'2026-09-01',7.570823568,40,1000,'Diesel')");
        assertEquals(2, service.compareVehicleEmissions().getFirst().vehicle().id());
        assertEquals(8.887, row(1).totalEmissionsKg(), 0.00001);
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'2026-09-02',18.92705892,100,1100,'Petrol')");
        assertEquals(1, service.compareVehicleEmissions().getFirst().vehicle().id());
        assertEquals(53.322, row(1).totalEmissionsKg(), 0.00001);
    }

    @Test void comparisonRequiresManagerAccess() {
        session.signOut();
        assertThrows(AuthorizationException.class, service::compareVehicleEmissions);
        session.signIn(new User(2,"driver",Role.DRIVER));
        assertThrows(AuthorizationException.class, service::compareVehicleEmissions);
    }

    @Test void comparisonUsesInclusiveDatesAndSelectedVehicle() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'2026-08-31',3.785411784,20,1000,'Petrol'),(1,'2026-09-01',3.785411784,20,1100,'Petrol'),(1,'2026-09-30',3.785411784,20,1200,'Petrol'),(1,'2026-10-01',3.785411784,20,1300,'Petrol'),(2,'2026-09-01',3.785411784,20,1000,'Diesel')");
        var selected = service.compareVehicleEmissions(1,LocalDate.of(2026,9,1),LocalDate.of(2026,9,30));
        assertEquals(1, selected.size());
        assertEquals(1, selected.getFirst().vehicle().id());
        assertEquals(17.774, selected.getFirst().totalEmissionsKg(), 0.00001);
        assertEquals(2, selected.getFirst().estimatedEntries());
        assertTrue(selected.getFirst().complete());
    }

    @Test void partialKnownEstimateDisclosesUnknownAndInvalidHistory() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'2026-09-01',3.785411784,20,1000,'Petrol'),(1,'2026-09-02',10,20,1100,'Electric'),(1,'invalid',10,20,1200,'Diesel')");
        var result = row(1);
        assertEquals(8.887,result.totalEmissionsKg(),0.00001);
        assertTrue(result.available());
        assertFalse(result.complete());
        assertEquals(1,result.estimatedEntries());
        assertEquals(1,result.unestimatedEntries());
        assertEquals(1,result.invalidRows());
        assertTrue(result.status().startsWith("Incomplete"));
    }

    @Test void unknownEntriesOutsidePeriodDoNotInvalidateTheSelectedPeriod() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'2026-08-31',10,20,1000,'Unknown'),(1,'2026-09-01',3.785411784,20,1100,'Petrol')");
        var result = service.compareVehicleEmissions(1,LocalDate.of(2026,9,1),null).getFirst();
        assertTrue(result.complete());
        assertEquals(0,result.unestimatedEntries());
    }

    @Test void malformedHistoryIsDisclosedEvenWhenItsDateCannotBeFiltered() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'invalid',10,20,1000,'Petrol'),(1,'2026-09-01',3.785411784,20,1100,'Petrol')");
        var result = service.compareVehicleEmissions(1,LocalDate.of(2026,9,1),null).getFirst();
        assertFalse(result.complete());
        assertEquals(1,result.invalidRows());
        assertTrue(result.status().contains("history"));
    }

    @Test void completeEstimatesPrecedePartialAndUnavailableRows() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'2026-09-01',100,200,1000,'Petrol'),(1,'2026-09-02',10,20,1100,'Unknown'),(2,'2026-09-01',3.785411784,20,1000,'Diesel')");
        var result = service.compareVehicleEmissions();
        assertEquals(2,result.get(0).vehicle().id());
        assertEquals(1,result.get(1).vehicle().id());
        assertEquals(3,result.get(2).vehicle().id());
    }

    @Test void singleUnrepresentableEstimateIsUnavailableRatherThanCrashing() throws Exception {
        execute("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type) VALUES(1,'2026-09-01',1e308,1,1000,'Diesel')");
        var result = row(1);
        assertFalse(result.available());
        assertEquals(1,result.unestimatedEntries());
        assertTrue(result.status().startsWith("Unavailable"));
    }

    @Test void filteredComparisonValidatesFiltersAndRole() {
        assertThrows(IllegalArgumentException.class, () -> service.compareVehicleEmissions(0,null,null));
        assertThrows(IllegalArgumentException.class, () -> service.compareVehicleEmissions(null,LocalDate.of(2026,9,2),LocalDate.of(2026,9,1)));
        session.signIn(new User(2,"driver",Role.DRIVER));
        assertThrows(AuthorizationException.class, () -> service.compareVehicleEmissions(1,null,null));
    }

    private ReportingService.VehicleEmissions row(int id) {
        return service.compareVehicleEmissions().stream().filter(value -> value.vehicle().id() == id).findFirst().orElseThrow();
    }

    private void execute(String sql) throws SQLException {
        try (var connection = connections.open(); var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}

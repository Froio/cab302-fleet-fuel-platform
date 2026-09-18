package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.AnalyticsDao;
import com.fuelfleet.cab302fleetfuelplatform.db.*;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.model.*;
import com.fuelfleet.cab302fleetfuelplatform.service.ReportingService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class ReportingIntegrationTest {
    @TempDir Path directory;
    ConnectionProvider connections;
    AppSession session = new AppSession();
    @BeforeEach void setup() throws Exception {
        connections = DBManager.provider("jdbc:sqlite:" + directory.resolve("test.db"));
        DBManager.initialize(connections);
        try (var c = connections.open(); var s = c.createStatement()) {
            s.executeUpdate("INSERT INTO vehicles(id,registration,make,model) VALUES(1,'TEST1','Test','Car'),(2,'TEST2','Test','Van')");
            s.executeUpdate("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer,fuel_type,full_tank) VALUES(1,'2026-09-01',40,80,1000,'Petrol',1),(1,'2026-09-10',30,60,1300,'Petrol',1),(2,'2026-09-10',20,50,2000,'Diesel',1)");
        }
    }
    @Test void persistsAndFiltersStoredRecords() throws Exception {
        DBManager.initialize(connections);
        var dao = new AnalyticsDao(connections);
        assertEquals(3, dao.load(null).rows().size());
        assertEquals(2, dao.load(1).rows().size());
        session.signIn(new User(1,"manager",Role.MANAGER));
        var report = new ReportingService(dao,session).load(1,null,null);
        assertEquals(140,report.months().getFirst().cost(),0.001);
        assertEquals(10,report.months().getFirst().efficiency().orElseThrow(),0.001);
    }
    @Test void blocksAnonymousAndDriverAccess() {
        var service = new ReportingService(new AnalyticsDao(connections),session);
        assertThrows(AuthorizationException.class, () -> service.load(null,null,null));
        session.signIn(new User(1,"driver",Role.DRIVER));
        assertThrows(AuthorizationException.class, () -> service.load(null,null,null));
        assertEquals("login-view.fxml",ViewAccessPolicy.resolve("reports.fxml",session.currentUser()));
        assertEquals("login-view.fxml",ViewAccessPolicy.resolve("reports.fxml",Optional.empty()));
        assertEquals("reports.fxml",ViewAccessPolicy.resolve("reports.fxml",Optional.of(new User(1,"manager",Role.MANAGER))));
    }
    @Test void excludesMalformedStoredDataWithVisibleCount() throws Exception {
        try (var c=connections.open();var s=c.createStatement()) {
            s.executeUpdate("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer) VALUES(1,'not-a-date',10,20,1500),(1,'2026-09-12','bad',20,1500)");
        }
        var data = new AnalyticsDao(connections).load(1);
        assertEquals(2,data.invalidRows());
        assertEquals(2,data.rows().size());
        session.signIn(new User(1,"manager",Role.MANAGER));
        assertTrue(new ReportingService(new AnalyticsDao(connections),session).load(1,null,null)
                .months().getFirst().efficiency().isEmpty());
    }
    @Test void migratesPlannedSchemaWithoutInventingHistoricalFuelOrFullTanks() throws Exception {
        try (var c=connections.open();var s=c.createStatement()) {
            s.executeUpdate("DROP TABLE fuel_logs");
            s.executeUpdate("CREATE TABLE fuel_logs(id INTEGER PRIMARY KEY,vehicle_id INTEGER,date TEXT,litres REAL,cost REAL,odometer REAL)");
            s.executeUpdate("INSERT INTO fuel_logs VALUES(1,1,'2026-09-01',10,20,100)");
        }
        DBManager.initialize(connections);
        DBManager.initialize(connections);
        var row = new AnalyticsDao(connections).load(1).rows().getFirst();
        assertEquals("Unknown",row.fuelType());
        assertFalse(row.fullTank());
    }
    @Test void freshSchemaEnforcesForeignKeysAndSupportsExistingVehicleDeletion() throws Exception {
        try (var c=connections.open();var s=c.createStatement()) {
            assertThrows(java.sql.SQLException.class, () -> s.executeUpdate("INSERT INTO fuel_logs(vehicle_id,date,litres,cost,odometer) VALUES(999,'2026-09-01',10,20,100)"));
            s.executeUpdate("DELETE FROM vehicles WHERE id=1");
        }
        assertTrue(new AnalyticsDao(connections).load(1).rows().isEmpty());
        assertEquals(1,new AnalyticsDao(connections).load(null).rows().size());
    }
}

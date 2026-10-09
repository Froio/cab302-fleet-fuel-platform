package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.dao.*;
import com.fuelfleet.cab302fleetfuelplatform.db.*;
import com.fuelfleet.cab302fleetfuelplatform.model.*;
import com.fuelfleet.cab302fleetfuelplatform.service.ReportingService;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDate;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class Week11IntegrationTest {
    @TempDir Path directory;
    ConnectionProvider db;
    VehicleDao vehicles;
    AppSession session;
    ReportingService reports;
    @BeforeEach void setup() throws Exception {
        db=DBManager.provider("jdbc:sqlite:"+directory.resolve("fleet.db"));
        DBManager.initialize(db);
        vehicles=new VehicleDao(db);
        session=new AppSession(); session.signIn(new User(1,"Manager",Role.MANAGER));
        reports=new ReportingService(new AnalyticsDao(db),vehicles,session);
    }
    @Test void recordsDateAutomaticallyAndPreservesItAcrossEditsFuelAndReinitialization() throws Exception {
        var before=LocalDate.now();
        var v=vehicles.insert("NEW1","Make","Model","Petrol",0);
        assertNotNull(v.dateAdded());
        assertFalse(v.dateAdded().isBefore(before));
        assertFalse(v.dateAdded().isAfter(LocalDate.now()));
        vehicles.update(v.id(),"EDIT1","Other","Van","Diesel",1);
        new FuelLogDao(db).save(v.id(),LocalDate.now(),20,40,100,"Diesel",true);
        vehicles.assignDriver(v.id(),null);
        DBManager.initialize(db);
        assertEquals(v.dateAdded(),new VehicleDao(db).findById(v.id()).orElseThrow().dateAdded());
        try(var c=db.open();var s=c.createStatement()) {
            assertThrows(java.sql.SQLException.class,()->s.executeUpdate("UPDATE vehicles SET date_added='2000-01-01' WHERE id="+v.id()));
        }
    }
    @Test void migratesOldVehiclesWithoutInventingADate() throws Exception {
        try(var c=db.open();var s=c.createStatement()) {
            s.executeUpdate("DROP TABLE fuel_logs"); s.executeUpdate("DROP TABLE vehicles");
            s.executeUpdate("CREATE TABLE vehicles(id INTEGER PRIMARY KEY,registration TEXT,make TEXT,model TEXT)");
            s.executeUpdate("INSERT INTO vehicles VALUES(1,'OLD1','Make','Model')");
        }
        DBManager.initialize(db); DBManager.initialize(db);
        var old=vehicles.findById(1).orElseThrow();
        assertNull(old.dateAdded()); assertTrue(old.dateAddedDisplay().contains("Not recorded"));
        assertNotNull(vehicles.insert("NEW2","Make","Model","Petrol",0).dateAdded());
    }
    @Test void aggregatesPerVehicleAndRefreshesAfterInsertUpdateDelete() throws Exception {
        var a=vehicles.insert("A","Make","Car","Petrol",0);
        var b=vehicles.insert("B","Make","Van","Diesel",0);
        var fuel=new FuelLogDao(db);
        fuel.save(a.id(),LocalDate.of(2026,10,1),1,0.1,100,"Petrol",true);
        fuel.save(a.id(),LocalDate.of(2026,10,2),1,0.2,200,"Petrol",true);
        fuel.save(b.id(),LocalDate.of(2026,10,2),10,20,100,"Diesel",true);
        assertEquals(0,new BigDecimal("20.3").compareTo(reports.expenditure(null,null,null).total()));
        assertEquals(0,new BigDecimal("0.3").compareTo(reports.expenditure(a.id(),null,null).total()));
        assertEquals(2,reports.expenditure(a.id(),null,null).vehicles().getFirst().entries());
        fuel.save(b.id(),LocalDate.of(2026,10,3),10,30,200,"Diesel",true);
        assertEquals(0,new BigDecimal("50.3").compareTo(reports.expenditure(null,null,null).total()));
        try(var c=db.open();var s=c.createStatement()) {
            s.executeUpdate("UPDATE fuel_logs SET cost=25 WHERE vehicle_id="+b.id()+" AND date='2026-10-02'");
        }
        assertEquals(0,new BigDecimal("55.3").compareTo(reports.expenditure(null,null,null).total()));
        try(var c=db.open();var s=c.createStatement()) { s.executeUpdate("DELETE FROM fuel_logs WHERE vehicle_id="+b.id()); }
        assertEquals(0,new BigDecimal("0.3").compareTo(reports.expenditure(null,null,null).total()));
    }
    @Test void filtersDatesInclusivelyAndShowsVehiclesWithNoCosts() {
        var v=vehicles.insert("A","Make","Car","Petrol",0);
        assertEquals(BigDecimal.ZERO,reports.expenditure(null,null,null).total());
        assertEquals(0,reports.expenditure(null,null,null).vehicles().getFirst().entries());
        new FuelLogDao(db).save(v.id(),LocalDate.of(2026,10,1),10,10,100,"Petrol",true);
        new FuelLogDao(db).save(v.id(),LocalDate.of(2026,10,2),10,20,200,"Petrol",true);
        var day=LocalDate.of(2026,10,2);
        assertEquals(0,new BigDecimal("20").compareTo(reports.expenditure(null,day,day).total()));
        assertThrows(IllegalArgumentException.class,()->reports.expenditure(null,day,day.minusDays(1)));
    }
    @Test void expenditureRequiresManager() {
        session.signOut(); assertThrows(AuthorizationException.class,()->reports.expenditure(null,null,null));
        session.signIn(new User(1,"Driver",Role.DRIVER));
        assertThrows(AuthorizationException.class,()->reports.expenditure(null,null,null));
    }
}

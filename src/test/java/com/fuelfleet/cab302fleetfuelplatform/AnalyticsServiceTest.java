package com.fuelfleet.cab302fleetfuelplatform;

import com.fuelfleet.cab302fleetfuelplatform.model.FuelRecord;
import com.fuelfleet.cab302fleetfuelplatform.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AnalyticsServiceTest {
    private final AnalyticsService service = new AnalyticsService();
    private FuelRecord row(int vehicle, String date, double litres, double cost, double odo, String fuel, boolean full) {
        return new FuelRecord(vehicle, LocalDate.parse(date), litres, cost, odo, fuel, full);
    }
    @Test void estimatesPetrolAndDieselUsingDocumentedFactors() {
        assertEquals(8.887, service.emissions(3.785411784, "Petrol").orElseThrow(), 0.000001);
        assertEquals(10.180, service.emissions(3.785411784, "Diesel").orElseThrow(), 0.000001);
    }
    @Test void unknownFuelIsUnavailableRatherThanZero() {
        assertTrue(service.emissions(20, "Hybrid").isEmpty());
        assertTrue(service.emissions(20, "Unknown").isEmpty());
    }
    @Test void rejectsInvalidFuel() {
        for (double litres : new double[]{-1, 0, Double.NaN, Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> service.emissions(litres, "Petrol"));
    }
    @Test void groupsByMonthFiltersInclusivelyAndCountsUnestimatedEntries() {
        var rows = List.of(row(1,"2026-08-31",10,20,100,"Petrol",true),
            row(1,"2026-09-01",20,40,300,"Petrol",true),
            row(2,"2026-09-30",10,30,500,"Unknown",true),
            row(1,"2026-10-01",30,60,600,"Diesel",true));
        var report = service.summarize(rows, LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-30"));
        assertEquals(1, report.size());
        var month = report.getFirst();
        assertEquals("2026-09", month.month().toString());
        assertEquals(70, month.cost(), 0.001);
        assertEquals(30, month.litres(), 0.001);
        assertEquals(20 * 8.887 / 3.785411784, month.co2Kg(), 0.001);
        assertEquals(1, month.unestimatedEntries());
        assertEquals(10, month.efficiency().orElseThrow(), 0.001);
    }
    @Test void partialFillUpsAccumulateUntilNextFullTank() {
        var rows = List.of(row(1,"2026-09-01",40,80,1000,"Petrol",true),
            row(1,"2026-09-05",10,20,1100,"Petrol",false),
            row(1,"2026-09-10",20,40,1300,"Petrol",true));
        assertEquals(10, service.summarize(rows,null,null).getFirst().efficiency().orElseThrow(), 0.001);
    }
    @Test void neverCombinesOdometersAcrossVehiclesOrInventsEfficiency() {
        var rows = List.of(row(1,"2026-09-01",40,80,1000,"Petrol",true),
            row(2,"2026-09-05",10,20,2000,"Diesel",true));
        assertTrue(service.summarize(rows,null,null).getFirst().efficiency().isEmpty());
    }
    @Test void rejectsReversedDatesAndHandlesEmptyData() {
        assertTrue(service.summarize(List.of(),null,null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.summarize(List.of(),LocalDate.of(2026,9,2),LocalDate.of(2026,9,1)));
    }
    @Test void resetAndDuplicateOdometersDoNotCreateFalseEfficiency() {
        var rows = List.of(row(1,"2026-09-01",40,80,1000,"Petrol",true),
            row(1,"2026-09-05",10,20,900,"Petrol",true),
            row(1,"2026-09-10",20,40,900,"Petrol",true));
        assertTrue(service.summarize(rows,null,null).getFirst().efficiency().isEmpty());
    }
    @Test void fleetEfficiencyUsesDistanceWeightingInsteadOfAverageOfRates() {
        var rows = List.of(row(1,"2026-09-01",40,80,1000,"Petrol",true),
            row(1,"2026-09-10",10,20,1100,"Petrol",true),
            row(2,"2026-09-01",40,80,1000,"Diesel",true),
            row(2,"2026-09-10",10,20,1200,"Diesel",true));
        assertEquals(20.0/300*100, service.summarize(rows,null,null).getFirst().efficiency().orElseThrow(), 0.001);
    }
    @Test void rejectsInvalidRecordValues() {
        assertThrows(IllegalArgumentException.class, () -> row(1,"2026-09-01",10,-1,100,"Petrol",true));
        assertThrows(IllegalArgumentException.class, () -> row(1,"2026-09-01",10,20,Double.NaN,"Petrol",true));
    }
}

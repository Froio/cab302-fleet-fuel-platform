package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.AnalyticsDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ReportingService {
    public record Report(List<AnalyticsService.Month> months, int invalidRows) { }
    public record VehicleEmissions(Vehicle vehicle, double totalEmissionsKg) {
    }
    private final AnalyticsDao dao;
    private final VehicleDao vehicleDao;
    private final AppSession session;
    public ReportingService() { this(new AnalyticsDao(), new VehicleDao(), AppSession.getInstance()); }
    public ReportingService(AnalyticsDao dao, AppSession session) {
        this(dao, new VehicleDao(), session);
    }

    public ReportingService(AnalyticsDao dao, VehicleDao vehicleDao, AppSession session) {
        this.dao = dao;
        this.vehicleDao = vehicleDao;
        this.session = session;
    }
    public Report load(Integer vehicleId, LocalDate from, LocalDate to) {
        if (!session.isManager()) throw new AuthorizationException("Fleet manager access is required.");
        if (vehicleId != null && vehicleId <= 0) throw new IllegalArgumentException("Select a valid vehicle");
        var records = dao.load(vehicleId);
        var months = new AnalyticsService().summarize(records.rows(),from,to);
        // Missing/corrupt fill-ups can invalidate every later tank interval.
        if (records.invalidRows() > 0) months = months.stream().map(month ->
                new AnalyticsService.Month(month.month(), month.litres(), month.cost(), month.co2Kg(),
                        month.unestimatedEntries(), java.util.OptionalDouble.empty())).toList();
        return new Report(months, records.invalidRows());
    }

    public record VehicleExpenditure(Vehicle vehicle, java.math.BigDecimal cost, int entries) { }
    public record Expenditure(List<VehicleExpenditure> vehicles, java.math.BigDecimal total, int invalidRows) { }

    /** Reads fresh persisted entries on every call; totals use decimal arithmetic. */
    public Expenditure expenditure(Integer vehicleId, LocalDate from, LocalDate to) {
        if (!session.isManager()) throw new AuthorizationException("Fleet manager access is required.");
        if (vehicleId != null && vehicleId <= 0) throw new IllegalArgumentException("Select a valid vehicle");
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("Start date must be on or before end date");
        var stored = dao.load(vehicleId);
        var sums = new java.util.HashMap<Integer, java.math.BigDecimal>();
        var counts = new java.util.HashMap<Integer, Integer>();
        for (var row : stored.rows()) {
            if (from != null && row.date().isBefore(from) || to != null && row.date().isAfter(to)) continue;
            sums.merge(row.vehicleId(),java.math.BigDecimal.valueOf(row.cost()),java.math.BigDecimal::add);
            counts.merge(row.vehicleId(),1,Integer::sum);
        }
        var results = vehicleDao.listAll().stream()
                .filter(v -> vehicleId == null || v.id() == vehicleId)
                .map(v -> new VehicleExpenditure(v,sums.getOrDefault(v.id(),java.math.BigDecimal.ZERO),
                        counts.getOrDefault(v.id(),0))).toList();
        var total = results.stream().map(VehicleExpenditure::cost)
                .reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
        return new Expenditure(results,total,stored.invalidRows());
    }

    public List<VehicleEmissions> compareVehicleEmissions() {
        if (!session.isManager()) {
            throw new AuthorizationException("Fleet manager access is required.");
        }
        List<Vehicle> allVehicles = vehicleDao.listAll();
        AnalyticsService analytics = new AnalyticsService();
        List<VehicleEmissions> results = new ArrayList<>();
       
        for (Vehicle v : allVehicles) {
            var records = dao.load(v.id());


            
            double total = 0;

            for (var row : records.rows()) {
                var emissionsResult = analytics.emissions(row.litres(), row.fuelType());
                if (emissionsResult.isPresent()) {
                    total = total + emissionsResult.getAsDouble();
                }
            }

            

            results.add(new VehicleEmissions(v, total));
        }

        // sort so highest emissions vehicle shows first
        results.sort(Comparator.comparingDouble(VehicleEmissions::totalEmissionsKg).reversed());

        return results;
    }
}

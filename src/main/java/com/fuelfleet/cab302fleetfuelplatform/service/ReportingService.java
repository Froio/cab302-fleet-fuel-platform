package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.AnalyticsDao;
import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ReportingService {
    public record Report(List<AnalyticsService.Month> months, int invalidRows) { }
    /** NaN means unavailable, never a clean zero. Partial estimates retain explicit coverage. */
    public record VehicleEmissions(Vehicle vehicle, double totalEmissionsKg, int estimatedEntries,
                                   int unestimatedEntries, int invalidRows, boolean totalOutOfRange) {
        public VehicleEmissions(Vehicle vehicle, double totalEmissionsKg) {
            this(vehicle,totalEmissionsKg,Double.isFinite(totalEmissionsKg) ? 1 : 0,0,0,false);
        }
        public boolean available() { return Double.isFinite(totalEmissionsKg); }
        public boolean complete() { return available() && unestimatedEntries == 0 && invalidRows == 0; }
        public String status() {
            if (totalOutOfRange) return "Unavailable — total exceeds supported range" + missingData();
            if (estimatedEntries == 0) return "Unavailable — no estimable entries in period" + missingData();
            return complete() ? "Complete estimate" : "Incomplete — known entries only" + missingData();
        }
        private String missingData() {
            return (unestimatedEntries > 0 ? "; " + unestimatedEntries + " unsupported/out-of-range entries" : "")
                    + (invalidRows > 0 ? "; " + invalidRows + " invalid history rows (any date)" : "");
        }
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
        return compareVehicleEmissions(null,null,null);
    }

    /** Fresh records, inclusive date filters, and conservative disclosure of invalid history. */
    public List<VehicleEmissions> compareVehicleEmissions(Integer vehicleId, LocalDate from, LocalDate to) {
        if (!session.isManager()) throw new AuthorizationException("Fleet manager access is required.");
        if (vehicleId != null && vehicleId <= 0) throw new IllegalArgumentException("Select a valid vehicle");
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("Start date must be on or before end date");
        AnalyticsService analytics = new AnalyticsService();
        List<VehicleEmissions> results = new ArrayList<>();
        for (Vehicle v : vehicleDao.listAll()) {
            if (vehicleId != null && v.id() != vehicleId) continue;
            var records = dao.load(v.id());
            BigDecimal total = BigDecimal.ZERO;
            int estimated = 0, unestimated = 0;
            for (var row : records.rows()) {
                if (from != null && row.date().isBefore(from) || to != null && row.date().isAfter(to)) continue;
                try {
                    var emissions = analytics.emissions(row.litres(),row.fuelType());
                    if (emissions.isPresent()) {
                        total = total.add(BigDecimal.valueOf(emissions.getAsDouble()));
                        estimated++;
                    } else unestimated++;
                } catch (IllegalArgumentException exception) {
                    // Valid stored litres can still overflow a tailpipe estimate.
                    unestimated++;
                }
            }
            boolean outOfRange = !Double.isFinite(total.doubleValue());
            double value = estimated == 0 || outOfRange ? Double.NaN : total.doubleValue();
            results.add(new VehicleEmissions(v,value,estimated,unestimated,records.invalidRows(),outOfRange));
        }
        // Compare complete estimates together; never promote missing data as a clean vehicle.
        results.sort(Comparator.comparingInt(ReportingService::comparisonGroup)
                .thenComparing(Comparator.comparingDouble((VehicleEmissions row) ->
                        row.available() ? row.totalEmissionsKg() : 0).reversed())
                .thenComparing(row -> row.vehicle().registration(),String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(results);
    }

    public static int comparisonGroup(VehicleEmissions row) {
        return row.complete() ? 0 : row.available() ? 1 : 2;
    }
}

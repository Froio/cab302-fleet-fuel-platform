package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.AnalyticsDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;
import java.time.LocalDate;
import java.util.List;

public final class ReportingService {
    public record Report(List<AnalyticsService.Month> months, int invalidRows) { }
    private final AnalyticsDao dao;
    private final AppSession session;
    public ReportingService() { this(new AnalyticsDao(), AppSession.getInstance()); }
    public ReportingService(AnalyticsDao dao, AppSession session) { this.dao = dao; this.session = session; }
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
}

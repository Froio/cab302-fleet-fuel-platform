package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.model.FuelRecord;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/** Pure calculations; see docs/reporting.md for factors and tank-to-tank assumptions. */
public final class AnalyticsService {
    public record Month(YearMonth month, double litres, double cost, double co2Kg,
                        int unestimatedEntries, OptionalDouble efficiency) { }

    public OptionalDouble emissions(double litres, String fuelType) {
        if (!Double.isFinite(litres) || litres <= 0) {
            throw new IllegalArgumentException("Litres must be finite and positive");
        }
        double factor = switch (fuelType == null ? "" : fuelType.trim().toLowerCase(Locale.ROOT)) {
            case "petrol", "gasoline" -> 8.887 / 3.785411784;
            case "diesel" -> 10.180 / 3.785411784;
            default -> Double.NaN;
        };
        if (Double.isNaN(factor)) return OptionalDouble.empty();
        double result = litres * factor;
        if (!Double.isFinite(result)) throw new IllegalArgumentException("Emissions exceed supported range");
        return OptionalDouble.of(result);
    }

    public List<Month> summarize(List<FuelRecord> records, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start date must be on or before end date");
        }
        var months = new TreeMap<YearMonth, Accumulator>();
        var tanks = new HashMap<Integer, Tank>();
        var ordered = new ArrayList<>(records);
        ordered.sort(Comparator.comparing(FuelRecord::date).thenComparingDouble(FuelRecord::odometer));
        for (FuelRecord row : ordered) {
            if (to != null && row.date().isAfter(to)) continue;
            boolean included = from == null || !row.date().isBefore(from);
            Accumulator month = included ? months.computeIfAbsent(YearMonth.from(row.date()), key -> new Accumulator()) : null;
            if (month != null) {
                month.litres += row.litres();
                month.cost += row.cost();
                OptionalDouble co2 = emissions(row.litres(), row.fuelType());
                if (co2.isPresent()) month.co2 += co2.getAsDouble();
                else month.unestimated++;
            }
            Tank tank = tanks.computeIfAbsent(row.vehicleId(), key -> new Tank());
            // A reset or duplicate reading breaks the interval; never divide by zero.
            if (tank.lastOdometer != null && row.odometer() <= tank.lastOdometer) {
                tank.baseline = null;
                tank.litres = 0;
            }
            tank.lastOdometer = row.odometer();
            if (tank.baseline != null) tank.litres += row.litres();
            if (row.fullTank()) {
                if (tank.baseline != null && month != null) {
                    month.distance += row.odometer() - tank.baseline;
                    month.intervalLitres += tank.litres;
                }
                tank.baseline = row.odometer();
                tank.litres = 0;
            }
        }
        return months.entrySet().stream().map(entry -> {
            Accumulator a = entry.getValue();
            for (double value : new double[]{a.litres,a.cost,a.co2,a.distance,a.intervalLitres})
                if (!Double.isFinite(value)) throw new IllegalArgumentException("Report totals exceed supported range");
            double rate = a.distance > 0 ? a.intervalLitres / a.distance * 100 : Double.NaN;
            if (Double.isInfinite(rate)) throw new IllegalArgumentException("Efficiency exceeds supported range");
            return new Month(entry.getKey(),a.litres,a.cost,a.co2,a.unestimated,
                    Double.isNaN(rate) ? OptionalDouble.empty() : OptionalDouble.of(rate));
        }).toList();
    }
    private static final class Accumulator {
        double litres, cost, co2, distance, intervalLitres;
        int unestimated;
    }
    private static final class Tank {
        Double baseline, lastOdometer;
        double litres;
    }
}

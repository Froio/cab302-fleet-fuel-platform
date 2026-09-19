package com.fuelfleet.cab302fleetfuelplatform.service;

public final class FuelCalculations {
    private FuelCalculations() {
    }

    public static double costPerFillUp(double litres, double pricePerLitre) {
        requireFinite(litres, "Litres");
        requireFinite(pricePerLitre, "Price per litre");
        if (litres <= 0) {
            throw new IllegalArgumentException("Litres must be positive");
        }
        if (pricePerLitre < 0) {
            throw new IllegalArgumentException("Price per litre must not be negative");
        }
        double result = litres * pricePerLitre;
        requireFinite(result, "Cost");
        return result;
    }

    public static double litresPer100Km(double litresUsed, double previousOdometer, double currentOdometer) {
        requireFinite(litresUsed, "Litres used");
        requireFinite(previousOdometer, "Previous odometer");
        requireFinite(currentOdometer, "Current odometer");
        if (litresUsed <= 0) {
            throw new IllegalArgumentException("Litres used must be positive");
        }
        if (previousOdometer < 0 || currentOdometer < 0) {
            throw new IllegalArgumentException("Odometer readings must not be negative");
        }
        if (currentOdometer <= previousOdometer) {
            throw new IllegalArgumentException("Current odometer must exceed previous odometer");
        }
        return litresPer100KmForDistance(litresUsed, currentOdometer - previousOdometer);
    }

    public static double litresPer100KmForDistance(double litresUsed, double distanceKm) {
        requireFinite(litresUsed, "Litres used");
        requireFinite(distanceKm, "Distance");
        if (litresUsed <= 0) {
            throw new IllegalArgumentException("Litres used must be positive");
        }
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        double result = litresUsed / distanceKm * 100;
        requireFinite(result, "Efficiency");
        return result;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}

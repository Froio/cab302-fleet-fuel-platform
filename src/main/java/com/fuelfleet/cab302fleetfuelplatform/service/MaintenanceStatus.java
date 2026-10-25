package com.fuelfleet.cab302fleetfuelplatform.service;

public enum MaintenanceStatus {
    NOT_SCHEDULED,
    ON_TRACK,
    DUE_SOON,
    DUE_NOW,
    OVERDUE;

    public static MaintenanceStatus calculate(
            long currentOdometer,
            Long nextServiceOdometer

    ) {
        if (currentOdometer < 0) {
            throw new IllegalArgumentException(
                    "Current odometer cannot be negative"
            );
        }
        if (nextServiceOdometer == null) {
            return NOT_SCHEDULED;

        }
        if (nextServiceOdometer <0) {
            throw new IllegalArgumentException(
                    "Next service odometer cannot be negative"
            );
        }

        if (currentOdometer > nextServiceOdometer) {
            return OVERDUE;
        }

        if (nextServiceOdometer == currentOdometer ) {
            return DUE_NOW;
        }

        if (nextServiceOdometer - currentOdometer <= 1000) {
            return DUE_SOON;
        }

        return ON_TRACK;
    }
}

package com.fuelfleet.cab302fleetfuelplatform.service;

public final class StandardMaintenancePolicy
        implements MaintenancePolicy {

  private final long dueSoonThreshold;

  public StandardMaintenancePolicy() {
  this(1000);
  }
  public StandardMaintenancePolicy(long dueSoonThreshold) {
      if (dueSoonThreshold < 0) {
          throw new IllegalArgumentException(
                  "Due-soon threshold cannot be negative."
          );
      }

  this.dueSoonThreshold = dueSoonThreshold;

}

@Override
public MaintenanceStatus calculate(
        long currentOdometer,
        Long nextServiceOdometer
) {
    if (nextServiceOdometer == null) {
        return MaintenanceStatus.NOT_SCHEDULED;
    }

    long remaining = nextServiceOdometer - currentOdometer;

    if (remaining < 0) {
        return MaintenanceStatus.OVERDUE;
    }

    if (remaining == 0) {
        return MaintenanceStatus.DUE_NOW;
    }

    if (remaining <= dueSoonThreshold) {
        return MaintenanceStatus.DUE_SOON;
    }
    return MaintenanceStatus.ON_TRACK;
}
}
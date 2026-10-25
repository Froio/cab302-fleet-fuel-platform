package com.fuelfleet.cab302fleetfuelplatform.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StandardMaintenancePolicyTest {

    @Test
    void defaultPolicyMarksVehicleDueSoonWithin1000Km() {
        MaintenancePolicy policy = new StandardMaintenancePolicy();

        assertEquals(
                MaintenanceStatus.DUE_SOON,
                policy.calculate(10_000,10_750L)
        );
    }

    @Test
    void customerPolicyUsesDifferentThreshold() {
        MaintenancePolicy policy = new StandardMaintenancePolicy(500);

        assertEquals(
                MaintenanceStatus.ON_TRACK,
                policy.calculate(10_000,10_750L)
        );

        assertEquals(
                MaintenanceStatus.DUE_SOON,
                policy.calculate(10_000,10_250L)
        );
    }

    @Test
    void policyHandlesUnscheduledVehicle() {
        MaintenancePolicy policy = new StandardMaintenancePolicy();

        assertEquals(
                MaintenanceStatus.NOT_SCHEDULED,
                policy.calculate(10_000, null)
        );
    }

    @Test
    void policyHandlesServiceDueNow() {
        MaintenancePolicy policy = new StandardMaintenancePolicy();
        assertEquals(
                MaintenanceStatus.DUE_NOW,
                policy.calculate(10_000, 10_000L)
        );
    }

    @Test
    void policyHandlesOverdueVehicle() {
        MaintenancePolicy policy = new StandardMaintenancePolicy();

        assertEquals(
                MaintenanceStatus.OVERDUE,
                policy.calculate(10_000, 9_900L)
        );
    }

    @Test
    void negativeThresholdIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StandardMaintenancePolicy(-1)


        );
    }
}
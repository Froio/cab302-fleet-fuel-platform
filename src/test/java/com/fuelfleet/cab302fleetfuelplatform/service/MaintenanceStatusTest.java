package com.fuelfleet.cab302fleetfuelplatform.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceStatusTest {

    @Test
    void noServiceScheduled() {
        assertEquals(
                MaintenanceStatus.NOT_SCHEDULED,
                MaintenanceStatus.calculate(45000,null)
        );
    }

    @Test

    void vehicleIsOnTrack() {
        assertEquals(
                MaintenanceStatus.ON_TRACK,
                MaintenanceStatus.calculate(45000, 50000L)
        );

    }
    @Test
    void vehicleIsDueSoon(){
        assertEquals(
                MaintenanceStatus.DUE_SOON,
                MaintenanceStatus.calculate(49500, 50000L)
        );
    }

    @Test
    void exactly1000KmRemainingIsDueSoon(){
        assertEquals(
                MaintenanceStatus.DUE_SOON,
                MaintenanceStatus.calculate(49000, 50000L)
        );

    }
    @Test
    void vehicleIsDueNow() {
        assertEquals(
                MaintenanceStatus.DUE_NOW,
                MaintenanceStatus.calculate(50000, 50000L)
        );

    }

    @Test
    void vehicleIsOverdue() {
        assertEquals(
                MaintenanceStatus.OVERDUE,
                MaintenanceStatus.calculate(51200, 50000L)
        );

    }

    @Test
    void negativeOdometerIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MaintenanceStatus.calculate(-1, 50000L)

        );

    }

    @Test
    void negativeServiceReadingIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MaintenanceStatus.calculate(45000, -1L)

        );
    }


}
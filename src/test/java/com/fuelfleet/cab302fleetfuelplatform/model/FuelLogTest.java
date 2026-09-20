package com.fuelfleet.cab302fleetfuelplatform.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FuelLogTest {
    @Test
    void futureDateIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new FuelLog(1, 1, "ABC123", LocalDate.now().plusDays(1),
                        50.0, 100.0, 1000.0, "Diesel", true));
    }
}
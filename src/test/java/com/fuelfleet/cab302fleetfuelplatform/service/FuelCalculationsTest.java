package com.fuelfleet.cab302fleetfuelplatform.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FuelCalculationsTest {

    @Test
    void testCostPerFillUp() {
        double result = FuelCalculations.costPerFillUp(40.0, 1.85);
        assertEquals(74.0, result, 0.001);
    }    @Test
    void testCostPerFillUpWithZeroLitresThrowsError() {
        assertThrows(IllegalArgumentException.class, () -> {
            FuelCalculations.costPerFillUp(0, 1.85);
        });
    }    @Test
    void testCostPerFillUpWithNegativeLitresThrowsError() {
        assertThrows(IllegalArgumentException.class, () -> {
            FuelCalculations.costPerFillUp(-5, 1.85);
     
        });
    }



  

    @Test
    void testCostPerFillUpWithNegativePriceThrowsError() {
        assertThrows(IllegalArgumentException.class, () -> {
            FuelCalculations.costPerFillUp(40.0, -1.0);
        });
    }

    @Test
    void testLitresPer100Km() {
        // 40 litres used, odometer went from 1000 to 1500 (500km)
        double result = FuelCalculations.litresPer100Km(40.0, 1000, 1500);
        assertEquals(8.0, result, 0.001);
    }

  

    @Test
    void testLitresPer100KmSameOdometerThrowsError() {
        assertThrows(IllegalArgumentException.class, () -> {
            FuelCalculations.litresPer100Km(40.0, 1000, 1000);
        });
    }

    @Test
    void testLitresPer100KmOdometerGoesBackwardsThrowsError() {
        assertThrows(IllegalArgumentException.class, () -> {
            FuelCalculations.litresPer100Km(40.0, 1500, 1000);
        });
    }    @Test
    void testLitresPer100KmForDistance() {
      
        double result = FuelCalculations.litresPer100KmForDistance(25.0, 250);
        assertEquals(10.0, result, 0.001);
    }
  

    @Test
    void testLitresPer100KmForDistanceWithZeroDistanceThrowsError() {
  
      assertThrows(IllegalArgumentException.class, () -> {
            FuelCalculations.litresPer100KmForDistance(40.0, 0);
        });
    }
}

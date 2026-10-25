package com.fuelfleet.cab302fleetfuelplatform;

import  com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.service.MaintenanceService;
import com.fuelfleet.cab302fleetfuelplatform.service.MaintenanceVehicleRepository;
import com.fuelfleet.cab302fleetfuelplatform.service.StandardMaintenancePolicy;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceRepositoryTest {

    @Test
    void fakeRespositoryCanBeInjectedIntoMaintenanceService() {
        MaintenanceVehicleRepository fakeRepository =
                new MaintenanceVehicleRepository() {
                    @Override
                    public Optional<Vehicle> findById(int vehicleId) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<Long> findNextServiceOdometer(int vehicleId) {
                        return Optional.empty();
                    }

                    @Override
                    public boolean updateNextServiceOdometer(int vehicleId, Long nextServiceOdometer) {
                        return false;
                    }
                };

        MaintenanceService service = new MaintenanceService(
                fakeRepository,
                com.fuelfleet.cab302fleetfuelplatform.session.AppSession.getInstance(),
                new StandardMaintenancePolicy()
                );
        assertNotNull(service);
    }
}

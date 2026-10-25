package com.fuelfleet.cab302fleetfuelplatform.service;

import com.fuelfleet.cab302fleetfuelplatform.dao.VehicleDao;
import com.fuelfleet.cab302fleetfuelplatform.exception.AuthorizationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DataAccessException;
import com.fuelfleet.cab302fleetfuelplatform.exception.DuplicateRegistrationException;
import com.fuelfleet.cab302fleetfuelplatform.exception.ValidationException;
import com.fuelfleet.cab302fleetfuelplatform.model.Vehicle;
import com.fuelfleet.cab302fleetfuelplatform.model.Role;
import com.fuelfleet.cab302fleetfuelplatform.model.User;
import com.fuelfleet.cab302fleetfuelplatform.session.AppSession;

public final class MaintenanceService {

        private final MaintenanceVehicleRepository vehicleRepository;
        private final AppSession session;
        private final MaintenancePolicy maintenancePolicy;

        public MaintenanceService() {
            this(new VehicleDao(), AppSession.getInstance(),new StandardMaintenancePolicy());

}

public MaintenanceService (VehicleDao vehicleDao, AppSession session) {
            this(
                    vehicleDao,
                    session,
                    new StandardMaintenancePolicy()
            );
}
public MaintenanceService(
        VehicleDao vehicleDao,
        AppSession session,
        MaintenancePolicy maintenancePolicy
) {
            this(
                    new DaoMaintenanceVehicleRepository(vehicleDao),
                    session,
                    maintenancePolicy
            );
}

public MaintenanceService(
        MaintenanceVehicleRepository vehicleRepository,
        AppSession session,
        MaintenancePolicy maintenancePolicy
) {
            this.vehicleRepository = java.util.Objects.requireNonNull(
                    vehicleRepository,
                    "vehicle repository cannot be null"
            );
            this.session = java.util.Objects.requireNonNull(
                    session,
                    "Session cannot be null"
            );

            this.maintenancePolicy = java.util.Objects.requireNonNull(
                    maintenancePolicy,
                    "Maintenance policy cannot be null"
            );
}
public void clearServiceSchedule(int vehicleId) {
            requireManager();

            if(!vehicleRepository.updateNextServiceOdometer(vehicleId,null)) {
                throw new ValidationException(
                        "the selected vehicle no longer exists."
                );
            }
}


public void scheduleService(int vehicleId, long nextServiceOdometer) {
            requireManager();

            if (nextServiceOdometer < 0 ) {
                throw new ValidationException(
                        "Next service odometer cannot be negative"
                );
            }

            if (!vehicleRepository.updateNextServiceOdometer(
                vehicleId,nextServiceOdometer)) {
                throw new ValidationException(
                        "The selected vehicle no longer exists."
                );
            }
}

public Long getNextServiceOdometer(int vehucleId) {
            requireManager();

            if (vehicleRepository.findById(vehucleId).isEmpty()){
                throw new ValidationException(
                        "The selected vehicle no longer exists"
                );
            }

            return vehicleRepository.findNextServiceOdometer(vehucleId)
                    .orElse(null);
}

public Long getNextServiceOdometerForCurrentDriver(int vehicleId) {
            User user = session.currentUser().orElseThrow(
                    () -> new AuthorizationException("Please sign in.")
            );

            if (user.role() != Role.DRIVER) {
                throw new AuthorizationException(
                        "Driver access is required."
                );
            }
            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(
                    () -> new ValidationException(
                            "The selected vehicle no longer exists."
                    )
            );

            if (vehicle.assignedDriverId() == null ||
                    !vehicle.assignedDriverId().equals(user.id())) {
                throw new AuthorizationException(
                        "You cannot view this vehicle's maintenance schedule."
                );
            }
            return vehicleRepository.findNextServiceOdometer(vehicleId)
                    .orElse(null);
}

public MaintenanceStatus getMaintenanceStatus(int vehicleId) {
            User user = session.currentUser().orElseThrow(
                    () -> new AuthorizationException("Please sign in.")

            );

            Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(
                    ( )-> new ValidationException(
                            "The selected vehicle no longer exists."
                    )
            );

            if (user.role() != Role.MANAGER) {
                if (user.role() !=Role.DRIVER ||
                        vehicle.assignedDriverId() == null ||
                        !vehicle.assignedDriverId().equals(user.id())) {
    throw new AuthorizationException(
                "You cannot view this vehicle's maintenance status."
    );
                }
            }

            Long nextServiceOdometer = vehicleRepository
                    .findNextServiceOdometer(vehicleId)
                    .orElse(null);

            return maintenancePolicy.calculate(
                    vehicle.currentOdometer(),
                    nextServiceOdometer
            );
}

private void requireManager() {
            if (!session.isManager()) {
                throw new AuthorizationException(
                        "Fleet Manager access is required"
                );
            }
}
}

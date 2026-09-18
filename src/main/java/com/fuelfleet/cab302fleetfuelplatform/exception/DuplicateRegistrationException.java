package com.fuelfleet.cab302fleetfuelplatform.exception;

public class DuplicateRegistrationException extends ValidationException {
    public DuplicateRegistrationException() {
        super("That registration number is already in the fleet.");
    }
}

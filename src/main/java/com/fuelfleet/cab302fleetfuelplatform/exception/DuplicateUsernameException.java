package com.fuelfleet.cab302fleetfuelplatform.exception;

public class DuplicateUsernameException extends ValidationException {
    public DuplicateUsernameException() {
        super("That username is already in use.");
    }
}

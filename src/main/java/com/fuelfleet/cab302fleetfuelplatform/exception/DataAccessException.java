package com.fuelfleet.cab302fleetfuelplatform.exception;

import java.sql.SQLException;

public class DataAccessException extends RuntimeException {
    private final int errorCode;

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
        this.errorCode = cause.getErrorCode();
    }

    public boolean isConstraintViolation() {
        return errorCode == 19 || (errorCode & 0xff) == 19;
    }
}

# Fleet Fuel Platform Run Guide

## Requirements

- Java Development Kit 21
- Internet access for the first Maven dependency download
- macOS, Windows, or Linux

Confirm the Java version before running the project:

```bash
java -version
```

The reported major version must be 21 or newer, while Java 21 is the configured project target.

## Project Location

Run all commands from the directory containing `pom.xml`, `mvnw`, and `mvnw.cmd`.

## Database Preparation

The application stores persistent data in `data/fleet.db`. The submitted database may already contain local demonstration data. No shared demonstration password is stored in this guide.

For a clean first-run demonstration on macOS or Linux, stop the application and preserve the existing database before starting:

```bash
mv data/fleet.db 
```

On Windows Command Prompt:

```text
move data\fleet.db 
```

When the application starts without `data/fleet.db`, it creates a new database automatically. The first user can then register the initial Fleet Manager account. The backup can be restored after the demonstration if required.

## Start the Application

macOS or Linux:

```bash
./mvnw javafx:run
```

Windows:

```text
mvnw.cmd javafx:run
```

The Maven goal uses a colon in `javafx:run`. `javafx.run` is not a valid Maven goal.

## First-Run Workflow

1. Start the application with a clean database.
2. Open the Fleet Manager registration screen.
3. Register the first Manager with demonstration credentials.
4. Use User Management to create a Driver account.
5. Use Vehicle Management to add a vehicle.
6. Attempt to reuse the same registration to demonstrate duplicate protection.
7. Assign the vehicle to the Driver.
8. Sign out and sign in as the Driver.
9. Open Fuel Logging and select the assigned vehicle.
10. Save two full-tank entries with increasing odometer readings.
11. Confirm that the newest entry appears in fuel history immediately.
12. Sign in as the Manager and open Reports.
13. Restart the application and confirm that the saved records remain available.


## Run the Automated Tests

macOS or Linux:

```bash
./mvnw test
```

Windows:

```text
mvnw.cmd test
```

Verified result for commit `8379680`:

```text
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The test suite contains 10 tests in `AnalyticsServiceTest` and 5 tests in `ReportingIntegrationTest`.

## Build Without Starting JavaFX

```bash
./mvnw -DskipTests compile
```

## Persistent Storage

SQLite schema creation and migration are implemented in:

```text
src/main/java/com/fuelfleet/cab302fleetfuelplatform/db/DBManager.java
```

The main tables are:

- `users`
- `vehicles`
- `fuel_logs`

Foreign keys are enabled for each application database connection. Usernames and vehicle registrations have case-insensitive unique indexes.


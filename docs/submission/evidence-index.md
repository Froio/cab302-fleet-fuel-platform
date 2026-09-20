# Assessment Evidence Index

This index maps the Project Progress and Performance assessment criteria to evidence in the submitted project. The video walkthrough should use the same section names and paths so that each item can be located quickly in the ZIP archive.

## Functional Prototype

| Evidence | Location in the ZIP | Video section |
|---|---|---|
| JavaFX application entry point | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/HelloApplication.java` | Starting the application |
| Manager registration | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/ManagerRegistrationController.java` and `src/main/resources/com/fuelfleet/cab302fleetfuelplatform/manager-registration.fxml` | Manager registration and authentication |
| Authentication and role-based navigation | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/LoginController.java`, `src/main/java/com/fuelfleet/cab302fleetfuelplatform/ViewAccessPolicy.java`, and `src/main/java/com/fuelfleet/cab302fleetfuelplatform/session/AppSession.java` | Manager and Driver login |
| User-account management | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/UserManagementController.java`, `src/main/java/com/fuelfleet/cab302fleetfuelplatform/service/UserService.java`, and `src/main/java/com/fuelfleet/cab302fleetfuelplatform/dao/UserDao.java` | Creating a Driver account |
| Vehicle list, add, edit, and delete | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/VehicleListController.java`, `src/main/java/com/fuelfleet/cab302fleetfuelplatform/VehicleEditController.java`, and `src/main/java/com/fuelfleet/cab302fleetfuelplatform/service/VehicleService.java` | Vehicle management |
| Duplicate registration protection | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/service/VehicleService.java`, `src/main/java/com/fuelfleet/cab302fleetfuelplatform/exception/DuplicateRegistrationException.java`, and `src/main/java/com/fuelfleet/cab302fleetfuelplatform/db/DBManager.java` | Attempting to add a duplicate registration |
| Driver-to-vehicle assignment | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/VehicleAssignmentController.java` and `src/main/java/com/fuelfleet/cab302fleetfuelplatform/service/VehicleAssignmentService.java` | Assigning a vehicle to a Driver |
| Fuel-entry form and history | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/FuelLoggingController.java`, `src/main/java/com/fuelfleet/cab302fleetfuelplatform/service/FuelLogService.java`, and `src/main/resources/com/fuelfleet/cab302fleetfuelplatform/fuel-logging.fxml` | Driver fuel logging |
| Fill-up cost and L/100 km calculations | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/service/FuelCalculations.java` | Fuel-calculation explanation |
| Fuel-log persistence | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/dao/FuelLogDao.java` | Saving a fuel log and restarting the application |
| Cost, efficiency, and emissions reports | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/ReportsController.java`, `src/main/java/com/fuelfleet/cab302fleetfuelplatform/service/ReportingService.java`, and `src/main/java/com/fuelfleet/cab302fleetfuelplatform/service/AnalyticsService.java` | Reports and analytics |
| SQLite schema and migration | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/db/DBManager.java` | Database persistence |

## Agile Planning

| Evidence | Location in the ZIP | Video section |
|---|---|---|
| Prioritised user stories and acceptance criteria | `docs/planning/user-stories.md` | Agile planning and user stories |
| Release planning | `docs/planning/release-plan.md` | Agile planning and user stories |
| Sprint goal, tasks, owners, and estimates | `docs/planning/sprint-1-plan.md` | Sprint planning |
| Project-board workflow plan | `docs/planning/trello-board-plan.md` | Project-management workflow |
| Database design | `docs/planning/database-design.md` | Persistence design |
| Research into comparable applications | `docs/research/similar-applications.md` | Design context |

## Iterative UI Design

| Evidence | Location in the ZIP | Video section |
|---|---|---|
| Design rationale and recorded revisions | `docs/planning/design-iteration-notes.md` | Iterative UI design |
| Login draft and revision | `docs/ui/login-draft-1.png` and `docs/ui/login-revised.png` | Iterative UI design |
| Manager dashboard draft and revision | `docs/ui/manager-dashboard-draft-1.png` and `docs/ui/manager-dashboard-revised.png` | Iterative UI design |
| Vehicle-list draft and revision | `docs/ui/vehicle-list-draft-1.png` and `docs/ui/vehicle-list-revised.png` | Iterative UI design |
| Add-vehicle draft and revision | `docs/ui/add-vehicle-draft-1.png` and `docs/ui/add-vehicle-revised.png` | Iterative UI design |
| Edit-vehicle draft and revision | `docs/ui/edit-vehicle-draft-1.png` and `docs/ui/edit-vehicle-revised.png` | Iterative UI design |

## Object-Oriented Design

| Principle or pattern | Evidence in the ZIP | Video explanation |
|---|---|---|
| Encapsulated domain models | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/model/` | `User`, `Role`, `Vehicle`, `FuelLog`, and report records represent domain data |
| Separation of concerns | Controller, service, DAO, model, session, and FXML packages under `src/main/` | Controllers handle UI events, services apply rules, and DAOs access SQLite |
| DAO pattern | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/dao/` | SQL is isolated from JavaFX controllers |
| Service layer | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/service/` | Validation, permissions, calculations, and workflows are encapsulated outside the UI |
| Interface-based database dependency | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/db/ConnectionProvider.java` | Database connections can be supplied to DAOs and tests |
| Role-based access | `src/main/java/com/fuelfleet/cab302fleetfuelplatform/model/Role.java`, `src/main/java/com/fuelfleet/cab302fleetfuelplatform/session/AppSession.java`, and `src/main/java/com/fuelfleet/cab302fleetfuelplatform/ViewAccessPolicy.java` | Manager and Driver access is checked outside the visual controls |
| MVC-style JavaFX structure | FXML resources and their Controller, Service, Model, and DAO collaborators | Follow one request from the view to SQLite |

## Testing and Continuous Integration

| Evidence | Location in the ZIP | Video section |
|---|---|---|
| Behaviour-focused analytics tests | `src/test/java/com/fuelfleet/cab302fleetfuelplatform/AnalyticsServiceTest.java` | Automated testing |
| SQLite and authorisation integration tests | `src/test/java/com/fuelfleet/cab302fleetfuelplatform/ReportingIntegrationTest.java` | Automated testing |
| GitHub Actions workflow | `.github/workflows/maven.yml` | Continuous integration |
| Verified local result | 15 tests, 0 failures, 0 errors, 0 skipped | Maven test output |
| Verified CI result | Successful run `35423031315` on `full-function` | GitHub Actions page |

The current repository contains meaningful automated tests and a passing CI workflow. The submission does not claim separate Red, Green, and Refactor commits where those transitions are not present in the recorded history.

## Version Control Workflow

Detailed branch, Pull Request, commit, and CI evidence is recorded in `docs/submission/version-control-evidence.md`.

## Running the Project

Environment requirements, database preparation, launch commands, test commands, and the recommended demonstration workflow are recorded in `docs/submission/run-guide.md`.

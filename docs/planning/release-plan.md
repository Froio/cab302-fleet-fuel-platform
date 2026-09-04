# Release Plan

## Product vision

Develop a desktop application that helps fleet managers and drivers record fuel
usage and understand vehicle efficiency, fuel expenditure and estimated carbon
emissions.

The release is divided into four logical increments so that core data
management is established before calculations, reporting and final integration.

## Planned releases / sprints

| Sprint | Goal | Planned functionality |
|---|---|---|
| Sprint 1 — Authentication & Vehicle Management | Establish the application foundation and vehicle-management workflow | Login, manager dashboard, vehicle list, add/edit/remove vehicle, initial SQLite schema |
| Sprint 2 — Fuel Logging | Capture and persist vehicle fuel information | Add fuel entry, vehicle selection, date, litres, cost, odometer, fuel history |
| Sprint 3 — Analytics & Sustainability | Turn stored data into useful fleet information | L/100 km calculations, fuel-cost analysis, estimated emissions, vehicle comparison, charts |
| Sprint 4 — Integration & Polish | Prepare a stable integrated application | Validation, usability improvements, bug fixes, refactoring, expanded tests, build/CI integration |

## Final release success criteria

The completed application should allow an authorised user to:

- log in;
- view and manage vehicles;
- record fuel fill-ups;
- view fuel history;
- calculate vehicle fuel efficiency;
- analyse fuel expenditure;
- estimate fuel-related carbon emissions;
- view useful summaries, reports or charts;
- keep application data persistently in SQLite.

## Release assumptions

- JavaFX is used for the desktop user interface.
- SQLite is used for persistent storage.
- Maven is used for dependency/build management.
- GitHub is used for source control and team integration.
- Functionality is delivered incrementally rather than building all features at
  once.

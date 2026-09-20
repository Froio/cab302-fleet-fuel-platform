# Version Control and Continuous Integration Evidence

## Repository

- Repository: <https://github.com/Froio/cab302-fleet-fuel-platform>
- Integrated assessment branch: `full-function`
- Shared branch: `main`
- Verified integrated commit: `837968016a52bcb9af815b932fda9d4e24abf326`
- Evidence captured: 19 September 2026, Australia/Brisbane

At the time this evidence was captured, `main` and `full-function` both referenced commit `8379680`.

## Feature Branches

| Branch | Verified head | Purpose |
|---|---|---|
| `BasicUIThomas` | `c333219` | Early JavaFX, authentication, and vehicle UI work |
| `Hongbo-Li` | `5deb9fc` | Account, role, vehicle-management, and assignment work |
| `thomas-emissions-analytics` | `945ebe5` | Reporting, analytics, and emissions work |
| `feature/fuel-logging` | `0ca4199` | Fuel-entry form, fuel history, DAO, validation, and calculations |
| `full-function` | `8379680` | Integrated assessment branch with CI configuration |
| `main` | `8379680` | Shared integrated branch |

Branch list: <https://github.com/Froio/cab302-fleet-fuel-platform/branches>

## Pull Requests

| Pull Request | Source branch | Status | Evidence |
|---|---|---|---|
| #1 — JavaFX authentication and vehicle screens | `BasicUIThomas` | Merged | <https://github.com/Froio/cab302-fleet-fuel-platform/pull/1> |
| #2 — Release planning contribution | `Hongbo-Li` | Merged | <https://github.com/Froio/cab302-fleet-fuel-platform/pull/2> |
| #3 — Hongbo Li account and vehicle work | `Hongbo-Li` | Merged | <https://github.com/Froio/cab302-fleet-fuel-platform/pull/3> |
| #4 — Reporting and emissions analytics | `thomas-emissions-analytics` | Merged | <https://github.com/Froio/cab302-fleet-fuel-platform/pull/4> |
| #5 — Fuel logging | `feature/fuel-logging` | Merged | <https://github.com/Froio/cab302-fleet-fuel-platform/pull/5> |

Pull Request list: <https://github.com/Froio/cab302-fleet-fuel-platform/pulls?q=is%3Apr>

## Selected Commits

| Commit | Author shown in Git history | Purpose |
|---|---|---|
| `818ebe4` | Thomas Froio | JavaFX authentication, Manager and vehicle screens, and SQLite DAO |
| `86de015` | Thomas Froio | Planning and UI design evidence |
| `fd3bbf5` | saviojose222-png | Sprint Plan 1 |
| `5deb9fc` | Hongbo-lll | Account, role, and vehicle-management contribution |
| `945ebe5` | Thomas Froio | Reporting and analytics implementation |
| `0ca4199` | Hongbo-lll | Fuel-logging implementation |
| `703f8ef` | Repository merge | Merge of Pull Request #5 |
| `8379680` | Repository history | Final CI workflow update captured by this evidence file |

Complete commit history: <https://github.com/Froio/cab302-fleet-fuel-platform/commits/main/>

## Continuous Integration

The workflow is stored in the submitted ZIP at:

```text
.github/workflows/maven.yml
```

The workflow performs these steps:

1. Checks out the repository with `actions/checkout@v5`.
2. Installs Temurin Java 21 with `actions/setup-java@v5`.
3. Restores the Maven dependency cache.
4. Runs `./mvnw --batch-mode test`.

Verified successful runs:

| Run ID | Branch | Result | Duration | Evidence |
|---|---|---|---:|---|
| `35423031315` | `full-function` | Success | 25 seconds | <https://github.com/Froio/cab302-fleet-fuel-platform/actions/runs/35423031315> |
| `35422729330` | `full-function` | Success | 15 seconds | <https://github.com/Froio/cab302-fleet-fuel-platform/actions/runs/35422729330> |
| `35422629522` | `main` | Success | 24 seconds | <https://github.com/Froio/cab302-fleet-fuel-platform/actions/runs/35422629522> |


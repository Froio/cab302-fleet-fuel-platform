# Fuel & Fleet Efficiency Platform

A CAB302 group project built for the "technology for change" theme, focused on fuel efficiency, cost tracking, and emissions awareness for personal and fleet vehicles.

## Overview

Users log fuel fill-ups (litres, cost, odometer reading) against their vehicles. The system then calculates:

- Fuel efficiency (L/100km)
- Cost per fill-up and cost per km
- Estimated CO2 emissions per log and over time
- Alerts when efficiency drops significantly compared to a vehicle's average

The platform supports two roles:

- **Driver** — logs fuel fill-ups, views their own vehicle's efficiency, cost, and emissions history, exports logs to CSV.
- **Fleet Manager** — manages user accounts, creates and edits vehicle records, assigns drivers to vehicles, and views fleet-wide reports on cost, efficiency, and emissions.

## Tech Stack

- **Java** — core application logic
- **JavaFX** — GUI, including chart components for efficiency/cost/emissions trends
- **SQLite** — local data persistence
- **Maven** — build and dependency management

## Project Structure

- `src/main` — application source code
- `pom.xml` — Maven project configuration and dependencies
- `mvnw` / `mvnw.cmd` — Maven wrapper scripts for consistent builds across machines

## Status

Currently in the Inception & Setup phase (Checkpoint 1): requirements and user stories drafted, repository set up, team roles being finalised.

## Team

Group project for CAB302 — Software Development Studio 1, QUT.

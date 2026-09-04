# Brief Research — Comparable Fleet/Fuel Applications

This short comparison was used to identify common interface and reporting ideas
for the CAB302 Fuel & Fleet Efficiency Platform.

## Fleetio

Fleetio's fuel-management product emphasises:

- recording and tracking fuel transactions;
- fuel-economy reporting including L/100 km;
- operating-cost insight;
- dashboard-level fuel metrics;
- reporting and export/share workflows.

**Design implication for our project:**  
Our application should make vehicle-level fuel history, efficiency and cost
information easy to reach rather than treating fuel logs as isolated records.

Source: https://www.fleetio.com/solutions/fuel-management-software

## Samsara

Samsara's fleet fuel-management offering emphasises:

- visibility of fuel usage and spend;
- fuel-efficiency information;
- dashboard-style summaries;
- identifying inefficient or wasteful behaviour;
- reports and operational insight.

**Design implication for our project:**  
A manager dashboard should surface a small set of useful fleet metrics and then
provide navigation to more detailed vehicle/fuel reports.

Source: https://www.samsara.com/products/telematics/fleet-fuel-management-system

## Features intentionally excluded from the CAB302 scope

Commercial systems can include GPS telematics, fuel-card integrations, fraud
detection, routing and real-time vehicle telemetry. Those features are beyond
the intended scope of this university project.

The CAB302 application instead focuses on a manageable desktop scope:

- manual fuel logging;
- vehicle management;
- efficiency/cost calculations;
- estimated emissions;
- charts and reports;
- SQLite persistence.

This keeps the project achievable while still reflecting useful patterns found
in real fleet-management products.

# Product User Stories

Priorities use **High / Medium / Low** for simple backlog ordering.

---

## US01 — User Login
**Priority:** High

**Story:**  
As a fleet manager, I want to log into the application so that fleet
information is protected from unauthorised users.

**Acceptance criteria**
- Username and password fields are displayed.
- Password characters are hidden while typing.
- Empty credentials cannot be accepted.
- Valid credentials allow access to the appropriate dashboard.
- Invalid credentials display a clear error.
- A successful login moves the user away from the login screen.

---

## US02 — Fleet Manager Dashboard
**Priority:** High

**Story:**  
As a fleet manager, I want to view a dashboard so that I can quickly understand
the current state of the fleet.

**Acceptance criteria**
- The dashboard is shown after successful manager login.
- Navigation to vehicle, fuel-log and reporting areas is available.
- Important fleet summary information can be displayed without opening another
  screen.
- The interface identifies the current section clearly.

---

## US03 — View Vehicles
**Priority:** High

**Story:**  
As a fleet manager, I want to view registered vehicles so that I can manage the
fleet.

**Acceptance criteria**
- Registered vehicles are displayed in a consistent list or table.
- Each vehicle shows useful identifying information.
- A vehicle can be selected.
- Controls exist to add and edit vehicles.
- Changes to stored vehicles are reflected in the list.

---

## US04 — Add Vehicle
**Priority:** High

**Story:**  
As a fleet manager, I want to add a vehicle so that a new fleet vehicle can be
tracked.

**Acceptance criteria**
- Registration can be entered.
- Make and model can be entered.
- Required fields cannot be empty.
- Invalid values produce an understandable error.
- A valid vehicle can be saved.
- A saved vehicle appears in the vehicle list.
- Saved data persists after restarting the application.

---

## US05 — Edit Vehicle
**Priority:** High

**Story:**  
As a fleet manager, I want to edit vehicle details so that fleet information
remains accurate.

**Acceptance criteria**
- An existing vehicle can be selected for editing.
- Existing values are populated in the form.
- Editable values can be changed.
- Invalid values cannot be saved.
- Valid changes are persisted.
- The vehicle list displays the updated information.

---

## US06 — Remove Vehicle
**Priority:** Medium

**Story:**  
As a fleet manager, I want to remove a vehicle that is no longer part of the
fleet so that current fleet data remains relevant.

**Acceptance criteria**
- A stored vehicle can be selected for removal.
- The application asks for confirmation before destructive removal.
- Cancelling leaves the vehicle unchanged.
- Confirming removal updates the active vehicle list.
- Related data is handled consistently with the database design.

---

## US07 — Record Fuel Fill-up
**Priority:** High

**Story:**  
As a driver, I want to record a fuel fill-up so that vehicle fuel usage can be
tracked.

**Acceptance criteria**
- A vehicle can be selected.
- Date, litres, cost and odometer reading can be entered.
- Required fields cannot be empty.
- Negative or otherwise invalid values are rejected.
- A valid fuel entry can be saved.
- The entry is associated with the selected vehicle.

---

## US08 — View Fuel History
**Priority:** High

**Story:**  
As a user, I want to view previous fuel entries so that I can inspect a
vehicle's fuel usage history.

**Acceptance criteria**
- Stored fuel entries can be displayed.
- Entries identify the associated vehicle.
- Date, litres, cost and odometer data are visible.
- Fuel history can be inspected in a sensible order.
- Newly saved entries appear in the history.

---

## US09 — Calculate Fuel Efficiency
**Priority:** High

**Story:**  
As a fleet manager, I want fuel efficiency calculated from recorded data so
that I can identify inefficient vehicles.

**Acceptance criteria**
- Efficiency is calculated only when sufficient valid data exists.
- The application uses a consistent metric such as L/100 km.
- Invalid distance or fuel values do not produce misleading results.
- Results are associated with the correct vehicle.

---

## US10 — Analyse Fuel Expenditure
**Priority:** High

**Story:**  
As a fleet manager, I want to see fuel expenditure so that I can monitor
operating costs.

**Acceptance criteria**
- Fuel costs are calculated from stored fuel entries.
- Costs can be associated with individual vehicles.
- Aggregate cost information can be displayed.
- The displayed result updates when relevant fuel data changes.

---

## US11 — Estimate Carbon Emissions
**Priority:** High

**Story:**  
As a fleet manager, I want estimated carbon emissions derived from fuel usage so
that I can evaluate the environmental impact of the fleet.

**Acceptance criteria**
- Emissions are calculated from recorded fuel consumption using a documented
  factor or method.
- The application clearly presents the result as an estimate.
- Results can be associated with a vehicle and/or reporting period.
- Invalid fuel data is not used in the calculation.

---

## US12 — View Analytics Charts
**Priority:** Medium

**Story:**  
As a fleet manager, I want charts showing efficiency, cost and emissions over
time so that trends are easy to identify.

**Acceptance criteria**
- At least one useful time-based chart can be displayed.
- Chart values come from stored application data.
- Units and labels are understandable.
- Empty-data cases are handled clearly.

---

## US13 — Compare Vehicles
**Priority:** Medium

**Story:**  
As a fleet manager, I want to compare vehicles so that I can identify vehicles
with unusually high fuel use, cost or emissions.

**Acceptance criteria**
- More than one vehicle can be included in a comparison.
- A consistent metric is used across compared vehicles.
- The comparison identifies which value belongs to which vehicle.
- Missing data is handled without producing false comparisons.

---

## US14 — Validate User Input
**Priority:** High

**Story:**  
As a user, I want invalid data to be rejected with a useful message so that
incorrect records are not stored.

**Acceptance criteria**
- Required inputs are checked before storage.
- Numeric values are checked for valid ranges.
- Errors identify what must be corrected.
- Invalid records are not persisted.
- Corrected data can subsequently be submitted.

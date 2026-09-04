# UI Design Iteration Notes

Prepared: 2026-09-04

The `docs/ui/` directory contains paired **draft** and **revised** wireframes.
The purpose is to show how an initial low-fidelity concept was reviewed and
turned into a clearer medium-fidelity design.

These files were created on 2026-09-04; they should not be represented as older
historical work.

## Login

**Draft**
- very small set of controls;
- minimal hierarchy;
- no dedicated validation area.

**Revised**
- stronger heading/hierarchy;
- clearer labels and spacing;
- explicit error-message area;
- stronger primary login action.

## Manager dashboard

**Draft**
- rough collection of metric cards and links;
- limited navigation structure.

**Revised**
- persistent left navigation;
- four summary metrics;
- recent fuel-entry table;
- clearer separation between navigation and content.

## Vehicle list

**Draft**
- simple vehicle table;
- actions not visually prioritised.

**Revised**
- page title and primary Add Vehicle action;
- search field;
- clearer table columns;
- consistent edit/delete actions.

## Add vehicle

**Draft**
- minimal form with only core fields.

**Revised**
- clear page heading;
- registration, make, model, year and fuel type;
- dedicated Cancel and Save actions;
- room for validation feedback.

## Edit vehicle

**Draft**
- same general structure as Add Vehicle without clear context.

**Revised**
- explicitly identifies the selected vehicle;
- pre-populated field concept;
- destructive delete action separated from save/cancel;
- clear Update Vehicle primary action.

## Design rationale

The revised screens aim for:

- consistent placement of navigation and primary actions;
- predictable form structures;
- visible validation feedback;
- easier conversion to JavaFX layouts;
- reduced ambiguity about the user's next action.

Fuel Logging — UX Improvement and Quality Assurance

User stories: US07 — Record Fuel Fill-up; US14 — Validate User Input
Commit: e0c6d5c

1. Purpose

This improvement focuses on making the fuel logging interface easier to use when drivers submit invalid information.

A developer-led usability review identified a weakness in the existing validation interface: errors were displayed in a general status label without visually identifying the input field requiring correction.

This review informed an improvement to field-specific validation feedback.

2. Original Behaviour

Previously, the application displayed validation messages beneath the Save Fuel Log button.

Although validation prevented invalid entries from being saved, the affected field was not visually highlighted.

This created a potential usability issue because users needed to interpret the message and locate the relevant input themselves.

3. Implemented Improvement

The FuelLoggingController was updated to:

Highlight the relevant invalid field with a red border.

Move keyboard focus to that field.

Retain the existing validation message.

Preserve entered values when validation fails.

Remove validation highlighting when a subsequent submission succeeds.

The fleet-theme.css stylesheet was also updated to introduce the input-error style.

Implementation evidence

src/main/java/com/fuelfleet/cab302fleetfuelplatform/FuelLoggingController.java

src/main/resources/com/fuelfleet/cab302fleetfuelplatform/fleet-theme.css

Git commit: e0c6d5c

4. Manual Verification

The following scenarios were checked in the running JavaFX application.

Test

Expected result

Outcome

Negative litres

Highlight litres field and display error

Passed

Negative price per litre

Highlight price field and display error

Passed

Invalid odometer reading

Highlight odometer field and display error

Passed

Missing litres

Highlight litres field and display required-field message

Passed

Corrected entry

Save successfully, clear error highlight and update history

Passed after correction

5. Iterative Improvement

The initial implementation successfully highlighted invalid fields.

However, manual testing identified a further usability defect: after correcting a missing litres value and successfully saving the fuel log, the red border remained visible.

The controller was updated to remove the input-error style after a successful save.

The affected scenario was then repeated, and the problem was no longer observed.

This demonstrates an iterative quality-improvement cycle:

Initial implementation → Manual testing → Defect identification → Code correction → Re-testing.

6. Automated Testing

The existing Maven test suite was executed after the initial validation-highlighting implementation.

Result: 74 tests executed, 0 failures, 0 errors and 0 skipped.

This establishes that the initial implementation passed the existing automated regression tests. The final visual behaviour was subsequently verified manually.

The Maven test suite does not itself establish automated coverage of JavaFX field highlighting.

7. Evaluation Limitations

This was a developer-led usability review and manual verification activity.

No independent participant usability study was conducted, and no quantitative improvement in task completion time or error rate is claimed.

The improvement was informed by internal usability assessment rather than direct external stakeholder feedback.

8. Assessment Relevance

This work supports:

US07: Record Fuel Fill-up.

US14: Validate User Input.

Quality assurance: Structured manual testing and correction of an identified defect.

UX improvement: Clearer visual feedback and easier identification of invalid fields.

Version control: A traceable source code commit.

Continuous integration: Existing automated regression testing.

The accompanying video walkthrough should demonstrate an invalid entry, the highlighted field, a corrected submission, and the disappearance of the validation highlight.
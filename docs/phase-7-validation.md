# Phase 7 validation

## Task 1

`make targeted ONLY="core:ui:TripCardEyebrowTest core:ui:TripCardInvalidationTest core:data:CheckOffWriterTest feature:trips:TripCardFormattingTest feature:trips:TripsViewModelTest feature:trips:TripsContentTest feature:trip:StepCheckTest feature:trip:TripPageSelectionTest feature:trip:CustomActionAnnouncesTest feature:trip:TimelineInvalidationTest feature:trip:ListInvalidationTest"` passes: 171 cases, 34.76 s wall including builds. The eyebrow invalidation case takes 2.53 s. `make format` passes after resolving formatting rules. The JVM and Robolectric result XMLs contain zero failures and errors.

An additional direct check of persisted marks passes in `core:data:CheckOffWriterTest#setCheckInsertsOneOutboxRowAndTheLocalMark` (2.19 s wall); its first compile used the wrong enum property (`rawValue`, 2.54 s), corrected to the landed `raw`. This checks the stored mark itself rather than relying only on the pending-outbox merge.

`make perf-timeline` and its Task 1 ±25% comparison remain with the user, as requested. The serial drawing/preparation budgets run in the single final `make quick`.

@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiReviewPrice
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.wizard.generation.TripEditContext

enum class TripWizardAction { edit, duplicate }

fun WizardForm.clearDuplicateDates(): WizardForm =
    copy(
        identity = null,
        itinerary = null,
        legs = legs.map { it.copy(departLocal = "", arriveLocal = "", leaveForAirportLocal = null) },
        stays =
            stays.map {
                it.copy(
                    bedAvailableMode = if (it.bedAvailableMode == "declared") null else it.bedAvailableMode,
                    bedAvailableFromLocal = null,
                    checkOutLocal = null,
                    busyIntervals = emptyList(),
                )
            },
        end = if (end.kind == "return_legs") end.copy(nights = null, checkOutDate = null) else WizardEnd(),
        preparation = preparation.copy(busyAnswered = false, busyIntervals = emptyList()),
    )

val MemberWizardDraft.inputFingerprint: String get() =
    WizardFingerprint
        .canonical(
            form.reviewInput(
                null,
                state.profile.takeIf {
                    it.isNotEmpty()
                },
            ),
        )
val MemberWizardDraft.hasChanges: Boolean get() =
    if (state.edit !=
        null
    ) {
        state.originalInput != inputFingerprint
    } else {
        state.entered
    }

fun MemberWizardDraft.loadTrip(
    snapshot: ApiTripInputSnapshot,
    id: String,
    action: TripWizardAction,
): MemberWizardDraft {
    val form = WizardForm.from(snapshot)
    if (form.legs.isEmpty() || form.legs.size > WizardForm.maxFlights ||
        form.returnBoundary !in 1..form.legs.size
    ) {
        throw DataFailure.Server(null)
    }
    var result =
        copy(
            form = form,
            state =
                state.copy(
                    airports = snapshot.airports,
                    fromLastTrip = false,
                    firstTrip = false,
                    homeChanged =
                        form.end.kind == "return_legs" &&
                            form.legs.lastOrNull()?.toIata != form.legs.firstOrNull()?.fromIata,
                    returnRouteConfirmed = form.end.kind == "return_legs",
                    copiedRoute =
                        (form.legs.firstOrNull()?.fromCity ?: form.legs.firstOrNull()?.fromIata ?: "") + " → " +
                            (
                                form.legs
                                    .take(form.returnBoundary)
                                    .lastOrNull()
                                    ?.toCity
                                    ?: form.legs
                                        .take(form.returnBoundary)
                                        .lastOrNull()
                                        ?.toIata
                                    ?: ""
                            ),
                ),
        )
    if (action == TripWizardAction.edit) {
        result =
            result.copy(
                state =
                    result.state.copy(
                        edit = TripEditContext(id, snapshot.sourceRevision),
                        originalForm = form,
                        snapshotStayEnd = snapshot.stayEnd,
                        profile = JsonObject(emptyMap()),
                        sleepNeeded = false,
                        entered = false,
                        step = MemberWizardStep.checking,
                        progressFloor = 1.0,
                        returnToReady = true,
                    ),
            )
        result = result.copy(state = result.state.copy(originalInput = result.inputFingerprint))
    } else {
        result =
            result.copy(
                form = form.clearDuplicateDates(),
                state = result.state.copy(entered = true, step = MemberWizardStep.outbound),
            )
    }
    return result
}

val MemberWizardModel.isEditing: Boolean get() = state.edit != null
val MemberWizardModel.hasChanges: Boolean get() = draft.hasChanges
val MemberWizardModel.checkingTitle: String get() =
    if (isEditing) {
        "Checking your changes"
    } else if (state.checkingAnswers ==
        true
    ) {
        "Checking your answers"
    } else {
        "Checking your trip"
    }

internal fun MemberWizardModel.acceptSourceReload(
    snapshot: ApiTripInputSnapshot,
    price: ApiReviewPrice?,
) {
    val form = WizardForm.from(snapshot)
    val before = state.originalForm ?: this.form
    val fields = mutableListOf<WizardPresentation.SourceChange.Field>()
    if (before.route().map { it.iata } !=
        form.route().map { it.iata }
    ) {
        fields.add(WizardPresentation.SourceChange.Field.route)
    }
    if (before.legs.take(before.returnBoundary) !=
        form.legs.take(form.returnBoundary)
    ) {
        fields.add(WizardPresentation.SourceChange.Field.flight)
    }
    if (before.legs.drop(before.returnBoundary) != form.legs.drop(form.returnBoundary) ||
        before.end != form.end
    ) {
        fields.add(WizardPresentation.SourceChange.Field.returnFlight)
    }
    if (before.override != form.override) fields.add(WizardPresentation.SourceChange.Field.sleep)
    if (before.preparation != form.preparation ||
        before.stays != form.stays
    ) {
        fields.add(WizardPresentation.SourceChange.Field.extras)
    }
    this.form = form
    state =
        state.copy(
            airports = snapshot.airports,
            profile = JsonObject(emptyMap()),
            originalForm = form,
            snapshotStayEnd = snapshot.stayEnd,
            homeChanged =
                form.end.kind == "return_legs" && form.legs.lastOrNull()?.toIata != form.legs.firstOrNull()?.fromIata,
            returnRouteConfirmed = form.end.kind == "return_legs",
            entered = false,
        )
    state = state.copy(originalInput = inputFingerprint)
    if (state.review !=
        null
    ) {
        state =
            state.copy(sourceChange = WizardPresentation.SourceChange(form, fields, price, state.review?.price))
    }
}

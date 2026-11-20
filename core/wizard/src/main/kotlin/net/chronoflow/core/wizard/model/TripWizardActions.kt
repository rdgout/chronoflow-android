@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.DataFailure
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

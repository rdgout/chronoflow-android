@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.ui

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.route
import net.chronoflow.core.wizard.model.routeComplete
import net.chronoflow.feature.wizard.TripWizardUiState
import net.chronoflow.feature.wizard.shell.WizardStepKey
import java.time.Instant
import java.util.Locale

@Immutable
data class WizardCopy(
    val key: String,
    val arguments: Map<String, String> = emptyMap(),
)

enum class NavTitle { NewTrip, EditTrip }

enum class RouteRowKind { From, Stop, To }

enum class RowHint { LastTrip, WhereYouLanded }

@Immutable
sealed interface RowLabel {
    data object From : RowLabel

    data object To : RowLabel

    data object Stop : RowLabel

    data class StopN(
        val number: Int,
    ) : RowLabel

    val copy: WizardCopy get() =
        when (this) {
            From -> WizardCopy("From")
            To -> WizardCopy("To")
            Stop -> WizardCopy("Stop")
            is StopN -> WizardCopy("Stop {{number}}", mapOf("number" to number.toString()))
        }
}

@Immutable
data class RouteRowUi(
    val position: Int,
    val routeIndex: Int,
    val kind: RouteRowKind,
    val label: RowLabel,
    val iata: String?,
    val city: String?,
    val hint: RowHint?,
    val locked: Boolean,
    val removable: Boolean,
    val invalid: Boolean,
    val sheet: WizardAirportSheet?,
) {
    val tag: String get() =
        when (kind) {
            RouteRowKind.From -> "wizard.from"
            RouteRowKind.Stop -> "wizard.stop.$position"
            RouteRowKind.To -> "wizard.to"
        }
    val description: WizardCopy get() =
        if (locked) {
            WizardCopy(
                "Leaves from {{city}}, where you landed. You can't change this.",
                mapOf("city" to (city ?: iata.orEmpty())),
            )
        } else {
            label.copy
        }
    val removeDescription: WizardCopy get() =
        WizardCopy(
            "Remove stop in {{city}}",
            mapOf("city" to (city ?: iata.orEmpty())),
        )
}

@Immutable
data class StopQuestionUi(
    val selected: Selection,
    val yesEnabled: Boolean,
    val addStopSheet: WizardAirportSheet,
) {
    enum class Selection { Direct, WithStops }

    val description: WizardCopy get() = WizardCopy("Do you change planes on the way?")
}

@Immutable
data class WhereUi(
    val home: Boolean,
    val rows: List<RouteRowUi>,
    val showSwap: Boolean,
    val swapEnabled: Boolean,
    val same: Boolean,
    val showNotice: Boolean,
    val question: StopQuestionUi?,
    val limitReached: Boolean,
    val showAddStop: Boolean,
    val complete: Boolean,
) {
    val title: WizardCopy get() = WizardCopy(if (home) "Your flight home" else "Where are you flying?")
    val subtitle: WizardCopy get() =
        WizardCopy(
            if (home) "It leaves from where you landed. Add any stops on the way." else "Pick the airports from your booking.",
        )
    val swapDescription: WizardCopy get() = WizardCopy("Swap airports")

    companion object {
        fun from(
            draft: MemberWizardDraft,
            home: Boolean = draft.state.step == MemberWizardStep.returnRoute,
        ): WhereUi {
            val form = draft.form
            val route = form.route(home)
            val start = if (home) form.returnBoundary else 0
            val same = route.firstOrNull()?.iata?.isNotEmpty() == true && route.first().iata == route.last().iata
            val stops = maxOf(0, route.size - 2)
            val canAdd = form.legs.size < WizardForm.maxFlights
            val swap = !home && route.size == 2
            val rows =
                route.mapIndexed { position, airport ->
                    val stop = position > 0 && position < route.lastIndex
                    val locked = home && position == 0
                    val label =
                        when {
                            position == 0 -> RowLabel.From
                            !stop -> RowLabel.To
                            route.size == 3 -> RowLabel.Stop
                            else -> RowLabel.StopN(position)
                        }
                    RouteRowUi(
                        position,
                        start + position,
                        when {
                            position == 0 -> RouteRowKind.From
                            stop -> RouteRowKind.Stop
                            else -> RouteRowKind.To
                        },
                        label,
                        airport.iata.takeIf { it.isNotEmpty() },
                        airport.city,
                        when {
                            locked -> RowHint.WhereYouLanded

                            position == 0 && !home &&
                                draft.state.mode == WizardMode.member &&
                                draft.state.fromLastTrip &&
                                !same -> RowHint.LastTrip

                            else -> null
                        },
                        locked,
                        stop,
                        same && position == route.lastIndex,
                        if (locked) {
                            null
                        } else {
                            WizardAirportSheet(
                                start + maxOf(0, position - 1),
                                when {
                                    position == 0 -> WizardAirportSheet.Side.from
                                    stop -> WizardAirportSheet.Side.stop
                                    home -> WizardAirportSheet.Side.home
                                    else -> WizardAirportSheet.Side.to
                                },
                                stop,
                            )
                        },
                    )
                }
            return WhereUi(
                home,
                rows,
                swap,
                swap && route.none { it.iata.isEmpty() },
                same,
                !home,
                if (form.routeComplete(home)) {
                    StopQuestionUi(
                        if (stops > 0 ||
                            draft.state.sheet?.side == WizardAirportSheet.Side.stop
                        ) {
                            StopQuestionUi.Selection.WithStops
                        } else {
                            StopQuestionUi.Selection.Direct
                        },
                        canAdd || stops > 0,
                        WizardAirportSheet(
                            if (home) form.legs.lastIndex else form.returnBoundary - 1,
                            WizardAirportSheet.Side.stop,
                        ),
                    )
                } else {
                    null
                },
                !canAdd,
                canAdd && stops > 0,
                form.routeComplete(home),
            )
        }
    }
}

@Immutable
data class WizardChromeUi(
    val navTitle: NavTitle,
    val showBack: Boolean,
    val showClose: Boolean,
    val progress: Float,
    val progressPercent: Int,
    val footerId: String?,
    val continueEnabled: Boolean,
    val routeComplete: Boolean,
) {
    companion object {
        fun from(
            active: TripWizardUiState.Active,
            closing: Boolean = false,
        ): WizardChromeUi {
            val step = active.wizard.draft.state.step
            val where = step == MemberWizardStep.whereTo || step == MemberWizardStep.returnRoute
            val complete =
                where &&
                    active.wizard.draft.form
                        .routeComplete(step == MemberWizardStep.returnRoute)
            return WizardChromeUi(
                if (active.wizard.draft.state.edit == null) NavTitle.NewTrip else NavTitle.EditTrip,
                active.path.isNotEmpty(),
                step != MemberWizardStep.calculating,
                active.progress.toFloat(),
                (
                    active.progress *
                        100
                ).toInt(),
                if (!where) {
                    null
                } else if (step == MemberWizardStep.returnRoute) {
                    "return-route"
                } else {
                    "where"
                },
                complete && !closing && !active.wizard.persistenceFailed && !active.wizard.reviewing,
                complete,
            )
        }
    }
}

@Immutable
data class AirportSheetUi(
    val sheet: WizardAirportSheet,
    val caption: Pair<String, String>?,
    val seedQuery: String? = null,
) {
    val title: WizardCopy get() =
        WizardCopy(
            when (sheet.side) {
                WizardAirportSheet.Side.from -> "Where do you fly from?"
                WizardAirportSheet.Side.to -> "Where do you fly to?"
                WizardAirportSheet.Side.home -> "Where do you fly home to?"
                WizardAirportSheet.Side.stop -> "Where's your stop?"
            },
        )

    companion object {
        fun from(draft: MemberWizardDraft): AirportSheetUi? {
            val sheet = draft.state.sheet ?: return null
            val leg = draft.form.legs.getOrNull(sheet.index) ?: return null
            val destination =
                if (sheet.replacingStop ==
                    true
                ) {
                    draft.form.legs.getOrNull(sheet.index + 1) ?: leg
                } else {
                    leg
                }
            return AirportSheetUi(
                sheet,
                if (sheet.side ==
                    WizardAirportSheet.Side.stop
                ) {
                    (leg.fromCity ?: leg.fromIata) to
                        (destination.toCity ?: destination.toIata)
                } else {
                    null
                },
            )
        }
    }
}

@Immutable
data class AirportRowUi(
    val iata: String,
    val name: String,
    val city: String?,
    val country: String,
) {
    companion object {
        fun from(airport: ApiAirportSuggestion) =
            AirportRowUi(airport.iata, airport.name, airport.city, airport.country)
    }
}

fun airportPlace(
    airport: AirportRowUi,
    locale: Locale,
): String =
    listOfNotNull(
        airport.city,
        Locale.Builder().setRegion(airport.country).build().getDisplayCountry(locale).takeIf {
            it.isNotEmpty()
        },
    ).joinToString(", ")

@Immutable
sealed interface AirportSearchUi {
    data object Hint : AirportSearchUi

    data object Searching : AirportSearchUi

    data class Results(
        val airports: List<AirportRowUi>,
    ) : AirportSearchUi

    data object Empty : AirportSearchUi

    data class Failed(
        val retryAt: Instant?,
    ) : AirportSearchUi
}

@Immutable
data class WizardUiState(
    val chrome: WizardChromeUi,
    val step: WizardStepKey,
    val where: WhereUi?,
    val sheet: AirportSheetUi?,
    val discardDialogVisible: Boolean = false,
    val path: List<WizardStepKey> = emptyList(),
    val search: AirportSearchUi = AirportSearchUi.Hint,
    val retryEnabled: Boolean = true,
) {
    companion object {
        fun from(
            active: TripWizardUiState.Active,
            closing: Boolean = false,
            dialog: Boolean = false,
        ): WizardUiState {
            val draft = active.wizard.draft
            val key = WizardStepKey(draft.state.step, draft.state.questionIndex)
            return WizardUiState(
                WizardChromeUi.from(active, closing),
                key,
                if (key == WizardStepKey.Where || key == WizardStepKey.ReturnRoute) WhereUi.from(draft) else null,
                AirportSheetUi.from(draft),
                dialog,
                active.path.map(WizardStepKey::from),
            )
        }
    }
}

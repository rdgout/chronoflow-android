package net.chronoflow.feature.wizard.ui

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.route
import net.chronoflow.core.wizard.model.routeComplete
import net.chronoflow.feature.wizard.TripWizardUiState
import net.chronoflow.feature.wizard.answers.SUPPORTED_SHEET_KINDS
import net.chronoflow.feature.wizard.answers.WizardAnswerSheetState
import net.chronoflow.feature.wizard.answers.WizardAnswerSheetUi
import net.chronoflow.feature.wizard.excluded.ExcludedNote
import net.chronoflow.feature.wizard.excluded.WizardExcludedUi
import net.chronoflow.feature.wizard.questions.WizardQuestionUi
import net.chronoflow.feature.wizard.returnchoice.WizardReturnUi
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.sleep.WizardSleepUi
import net.chronoflow.feature.wizard.sleep.WizardTimeSheetUi
import net.chronoflow.feature.wizard.sleep.clockString
import net.chronoflow.feature.wizard.times.WizardPicker
import net.chronoflow.feature.wizard.times.WizardTimesUi
import net.chronoflow.feature.wizard.times.flightTimesComplete
import java.time.Instant
import java.util.IllformedLocaleException
import java.util.Locale

@Immutable
data class WizardCopy(
    val key: String,
    val arguments: Map<String, String> = emptyMap(),
)

private val routeServerError = Regex("^legs\\.(\\d+)\\.(fromIata|toIata)$")

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
    val serverErrors: Map<String, FieldMessage> = emptyMap(),
) {
    val title: WizardCopy get() = WizardCopy(if (home) "Your flight home" else "Where are you flying?")
    val subtitle: WizardCopy get() =
        WizardCopy(
            if (home) {
                "It leaves from where you landed. Add any stops on the way."
            } else {
                "Pick the airports from your booking."
            },
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
                        (same && position == route.lastIndex) || draft.fieldErrors[
                            if (position == 0) {
                                "legs.$start.fromIata"
                            } else {
                                "legs.${start + maxOf(0, position - 1)}.toIata"
                            },
                        ] != null,
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
                draft.fieldErrors
                    .filterKeys { field ->
                        routeServerError
                            .matchEntire(field)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull()
                            ?.let { it in start until start + route.size - 1 } == true
                    }.toSortedMap(),
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
    val footerHint: WizardCopy? = null,
    val secondaryClose: Boolean = false,
    val primaryPlain: Boolean = false,
    val closeEnabled: Boolean = true,
    val primaryLabel: WizardCopy? = null,
    val primaryBusy: Boolean = false,
    val secondaryRequestsClose: Boolean = false,
) {
    companion object {
        fun from(
            active: TripWizardUiState.Active,
            closing: Boolean = false,
            checking: net.chronoflow.feature.wizard.checking.WizardCheckingUi? = null,
            fineTune: net.chronoflow.feature.wizard.finetune.WizardFineTuneUi? = null,
        ): WizardChromeUi {
            val step = active.wizard.draft.state.step
            val where = step == MemberWizardStep.whereTo || step == MemberWizardStep.returnRoute
            val complete =
                when (step) {
                    MemberWizardStep.fineTune -> {
                        !active.wizard.reviewing
                    }

                    MemberWizardStep.checking -> {
                        checking?.failure != null && checking.retrySeconds == 0
                    }

                    MemberWizardStep.excluded -> {
                        true
                    }

                    MemberWizardStep.sleep -> {
                        active.sleepComplete
                    }

                    MemberWizardStep.outbound -> {
                        flightTimesComplete(active.wizard.draft, false)
                    }

                    MemberWizardStep.returnFlight -> {
                        flightTimesComplete(active.wizard.draft, true)
                    }

                    else -> {
                        where &&
                            active.wizard.draft.form
                                .routeComplete(step == MemberWizardStep.returnRoute)
                    }
                }
            return WizardChromeUi(
                if (active.wizard.draft.state.edit == null) NavTitle.NewTrip else NavTitle.EditTrip,
                active.path.isNotEmpty() &&
                    (step != MemberWizardStep.checking || (checking?.failure != null && !active.isEditing)),
                step != MemberWizardStep.calculating,
                active.progress.toFloat(),
                (
                    active.progress *
                        100
                ).toInt(),
                when (step) {
                    MemberWizardStep.checking -> if (checking?.failure != null) "checking" else null
                    MemberWizardStep.fineTune -> "fine-tune"
                    MemberWizardStep.whereTo -> "where"
                    MemberWizardStep.returnRoute -> "return-route"
                    MemberWizardStep.sleep -> "sleep"
                    MemberWizardStep.excluded -> "excluded"
                    MemberWizardStep.outbound -> "outbound"
                    MemberWizardStep.returnFlight -> "return-flight"
                    else -> null
                },
                complete && !closing && !active.wizard.persistenceFailed && !active.wizard.reviewing,
                complete,
                secondaryClose = step == MemberWizardStep.excluded || checking?.failure != null,
                secondaryRequestsClose = step == MemberWizardStep.checking,
                primaryLabel = checking?.button ?: fineTune?.button,
                primaryBusy = fineTune?.reviewing == true,
                primaryPlain = step == MemberWizardStep.excluded,
                closeEnabled = !closing,
                footerHint =
                    if (step ==
                        MemberWizardStep.returnChoice || step == MemberWizardStep.question
                    ) {
                        WizardCopy("Tap an answer to continue.")
                    } else {
                        null
                    },
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
    val timezone: String,
) {
    val airport: ApiAirportSuggestion get() = ApiAirportSuggestion(iata, name, city, country, timezone)

    companion object {
        fun from(airport: ApiAirportSuggestion) =
            AirportRowUi(airport.iata, airport.name, airport.city, airport.country, airport.timezone)
    }
}

fun airportPlace(
    airport: AirportRowUi,
    locale: Locale,
): String {
    val country =
        try {
            Locale
                .Builder()
                .setRegion(airport.country)
                .build()
                .getDisplayCountry(locale)
                .takeIf { it.isNotEmpty() }
        } catch (_: IllformedLocaleException) {
            null
        }
    return listOfNotNull(airport.city, country).joinToString(", ")
}

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
    val times: WizardTimesUi? = null,
    val returnChoice: WizardReturnUi? = null,
    val sleep: WizardSleepUi? = null,
    val answerSheet: WizardAnswerSheetUi? = null,
    val question: WizardQuestionUi? = null,
    val excluded: WizardExcludedUi? = null,
    val checking: net.chronoflow.feature.wizard.checking.WizardCheckingUi? = null,
    val fineTune: net.chronoflow.feature.wizard.finetune.WizardFineTuneUi? = null,
    val cue: net.chronoflow.feature.wizard.checking.WizardHapticCue? = null,
    val discardMessage: WizardCopy = WizardCopy("What you entered is not saved."),
) {
    companion object {
        fun from(
            active: TripWizardUiState.Active,
            closing: Boolean = false,
            dialog: Boolean = false,
            timeFormat: TimeFormat = TimeFormat.Value24,
            now: Instant = Instant.EPOCH,
            continueFailures: Int = 0,
            picker: WizardPicker? = null,
            openTime: String? = null,
            answerSheet: WizardAnswerSheetState? = null,
            uiOverride: net.chronoflow.feature.wizard.shell.WizardUiOverride? = null,
        ): WizardUiState {
            val effective =
                active.copy(
                    wizard =
                        active.wizard.copy(
                            checking = uiOverride?.checking ?: active.wizard.checking,
                            retrySeconds = uiOverride?.retrySeconds ?: active.wizard.retrySeconds,
                            reviewing = uiOverride?.reviewing ?: active.wizard.reviewing,
                        ),
                )
            val draft = active.wizard.draft
            val key =
                WizardStepKey(
                    draft.state.step,
                    if (draft.state.step ==
                        MemberWizardStep.question
                    ) {
                        draft.state.questionIndex ?: 0
                    } else {
                        null
                    },
                )
            val checking =
                net.chronoflow.feature.wizard.checking.WizardCheckingUi
                    .from(effective)
            val fineTune =
                net.chronoflow.feature.wizard.finetune.WizardFineTuneUi
                    .from(effective, timeFormat, openTime)
            return WizardUiState(
                WizardChromeUi.from(effective, closing, checking, fineTune),
                key,
                if (key == WizardStepKey.Where || key == WizardStepKey.ReturnRoute) WhereUi.from(draft) else null,
                AirportSheetUi.from(draft),
                dialog,
                active.path.map(WizardStepKey::from),
                checking = checking,
                fineTune = fineTune,
                cue = active.cue,
                discardMessage =
                    WizardCopy(
                        when {
                            active.isEditing -> {
                                "Your trip stays as it was."
                            }

                            draft.state.mode == WizardMode.guest -> {
                                "What you entered is not saved. You go back to the welcome screen."
                            }

                            else -> {
                                "What you entered is not saved."
                            }
                        },
                    ),
                times =
                    if (key == WizardStepKey.Outbound || key == WizardStepKey.ReturnFlight) {
                        WizardTimesUi.from(
                            draft,
                            key == WizardStepKey.ReturnFlight,
                            timeFormat,
                            now,
                            continueFailures,
                            picker,
                        )
                    } else {
                        null
                    },
                answerSheet =
                    answerSheet
                        ?.takeIf { sheet ->
                            sheet.prompt.id == draft.state.promptSheetID &&
                                sheet.prompt.kind in SUPPORTED_SHEET_KINDS && sheet.prompt in active.optionalPrompts
                        }?.let { sheet ->
                            net.chronoflow.feature.wizard.answers
                                .answerSheetUi(sheet, draft, timeFormat, now, openTime)
                        },
                excluded =
                    if (key == WizardStepKey.Excluded) {
                        WizardExcludedUi(
                            draft.state.review
                                ?.reasons
                                .orEmpty(),
                            when {
                                active.isEditing -> ExcludedNote.Edit
                                active.exclusionWasSaved -> ExcludedNote.Saved
                                else -> ExcludedNote.Default
                            },
                        )
                    } else {
                        null
                    },
                question =
                    if (key.step == MemberWizardStep.question) {
                        WizardQuestionUi.from(
                            draft,
                            active.wizard.choicePending,
                            !closing && !active.wizard.persistenceFailed && !active.wizard.reviewing,
                        )
                    } else {
                        null
                    },
                sleep =
                    if (key == WizardStepKey.Sleep) {
                        WizardSleepUi.from(draft, continueFailures, timeFormat, openTime)
                    } else {
                        null
                    },
                returnChoice =
                    if (key ==
                        WizardStepKey.ReturnChoice
                    ) {
                        WizardReturnUi.from(
                            draft,
                            active.wizard.choicePending,
                            !closing && !active.wizard.persistenceFailed && !active.wizard.reviewing,
                        )
                    } else {
                        null
                    },
            )
        }
    }
}

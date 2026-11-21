@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.ready

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiReviewPrice
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardCredits
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.core.wizard.model.WizardPresentation
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.feature.wizard.TripWizardUiState

object ReadyUiMapper {
    fun map(
        active: TripWizardUiState.Active,
        language: Language = Language.En,
    ): ReadyUi? {
        val snapshot = active.wizard
        val draft = snapshot.draft
        val state = draft.state
        if (state.step != MemberWizardStep.ready) return null
        val form = draft.form
        val review = state.review
        val credits = review?.let(WizardCredits::of)
        val input = form.reviewInput(form.identity, state.profile.takeIf { it.isNotEmpty() })
        val rows =
            buildList {
                add(ReadyRowUi(ReadyRow.Route, ReadyText(routeSummary(form))))
                add(
                    ReadyRowUi(
                        ReadyRow.Flight,
                        ReadyText.capture { t ->
                            WizardPresentation.flightSummary(form, language = language, t = t)
                        },
                    ),
                )
                if (state.mode == WizardMode.member) {
                    add(
                        ReadyRowUi(
                            ReadyRow.Return,
                            ReadyText.capture { t ->
                                WizardPresentation.returnSummary(
                                    form,
                                    review?.summary?.stayEnd ?: state.snapshotStayEnd,
                                    language,
                                    t,
                                )
                            },
                        ),
                    )
                }
                add(ReadyRowUi(ReadyRow.Sleep, sleepSummary(draft)))
                if (state.mode == WizardMode.guest) {
                    val count =
                        listOf("age_band", "shift_work").count {
                            state.profile[it] != null &&
                                state.profile[it] != JsonNull
                        }
                    add(
                        ReadyRowUi(
                            ReadyRow.Questions,
                            ReadyText.capture { t ->
                                t(
                                    if (count ==
                                        0
                                    ) {
                                        "None needed"
                                    } else {
                                        "{{count}} answered"
                                    },
                                    mapOf("count" to count.toString()),
                                )
                            },
                        ),
                    )
                }
                add(
                    ReadyRowUi(
                        ReadyRow.Extras,
                        safely {
                            ReadyText.capture { t ->
                                WizardPresentation.extrasSummary(form, review, input, language, t)
                            }
                        },
                    ),
                )
                if (review?.status == PlanStatus.Modified) {
                    active.optionalPrompts.firstOrNull { it.kind == PromptKind.LatestWake }?.let { prompt ->
                        add(
                            ReadyRowUi(
                                ReadyRow.LatestWake,
                                safely {
                                    ReadyText.capture { t ->
                                        val answer = WizardPresentation.promptAnswerText(prompt, input, language, t)
                                        t("{{time}} · assumed", mapOf("time" to answer))
                                    }
                                },
                            ),
                        )
                    }
                }
            }
        val price =
            review?.price?.let {
                price(
                    it,
                    credits,
                    active.isEditing,
                    state.originalForm?.end?.kind,
                    form.end.kind,
                    state.mode,
                )
            }
        val sourceChanged =
            state.sourceChange?.let { change ->
                safely {
                    ReadyText.capture { t ->
                        WizardPresentation
                            .sourceChangeSummary(
                                change,
                                review,
                                review?.summary?.stayEnd ?: state.snapshotStayEnd,
                                language,
                                t,
                            ).orEmpty()
                    }
                }.takeIf { it.pattern.isNotEmpty() }
            }
        val failure = state.saveFailure
        val banner =
            when {
                failure != null -> {
                    ReadyBannerUi.SaveFailed(failure.kind, snapshot.retrySeconds, active.isEditing)
                }

                sourceChanged != null -> {
                    ReadyBannerUi.SourceChanged(sourceChanged)
                }

                review?.status == PlanStatus.Modified -> {
                    ReadyBannerUi.Adjusted(review.reasons)
                }

                state.updated == true -> {
                    ReadyBannerUi.Updated(
                        review?.summary?.line,
                        review?.price?.let {
                            priceValue(it, active.isEditing)
                        },
                    )
                }

                else -> {
                    null
                }
            }
        val enabled =
            !snapshot.reviewing && !snapshot.persistenceFailed && !snapshot.closed && snapshot.retrySeconds == 0 &&
                (!active.isEditing || active.hasChanges) && (state.savedTripId != null || review?.canSave == true)
        val footer =
            if (credits == WizardCredits.unavailable) {
                ReadyFooterUi.CloseOnly
            } else {
                val label =
                    when {
                        snapshot.reviewing -> {
                            ReadyButtonLabel.Checking
                        }

                        state.saveFailure?.kind == WizardSaveFailure.Kind.throttle && snapshot.retrySeconds > 0 -> {
                            ReadyButtonLabel
                                .Countdown(
                                    snapshot.retrySeconds,
                                )
                        }

                        state.saveFailure?.kind == WizardSaveFailure.Kind.throttle -> {
                            ReadyButtonLabel.Retry
                        }

                        active.isEditing -> {
                            ReadyButtonLabel.Update
                        }

                        else -> {
                            ReadyButtonLabel.MakePlan
                        }
                    }
                ReadyFooterUi.Primary(label, enabled, snapshot.reviewing)
            }
        return ReadyUi(
            review?.summary?.let { summary ->
                ReadySummaryUi(
                    summary.line,
                    ReadyText.capture { t ->
                        WizardPresentation.planDateSummary(form, summary, language, t)
                    },
                )
            },
            rows,
            price,
            banner,
            footer,
            credits,
            snapshot.reviewing,
            editing = active.isEditing,
        )
    }

    fun routeSummary(form: WizardForm): String =
        form.legs
            .firstOrNull()
            ?.fromIata
            .orEmpty() + " → " +
            form.legs
                .take(form.returnBoundary)
                .lastOrNull()
                ?.toIata
                .orEmpty()

    fun sleepSummary(draft: MemberWizardDraft): ReadyText {
        val override = draft.form.override
        val bed =
            if (override.enabled) {
                override.habitualBed
            } else {
                (draft.state.profile["habitual_bed"] as? JsonPrimitive)?.contentOrNull
                    ?: (draft.state.savedProfile?.get("habitualBed") as? JsonPrimitive)?.contentOrNull
            }
        val wake =
            if (override.enabled) {
                override.habitualWake
            } else {
                (draft.state.profile["habitual_wake"] as? JsonPrimitive)?.contentOrNull
                    ?: (draft.state.savedProfile?.get("habitualWake") as? JsonPrimitive)?.contentOrNull
            }
        val times = listOfNotNull(bed, wake).joinToString("–")
        if (draft.state.mode == WizardMode.guest) {
            val label =
                when ((draft.state.profile["chronotype"] as? JsonPrimitive)?.contentOrNull) {
                    "early" -> "Early bird"
                    "intermediate" -> "In between"
                    "late" -> "Night owl"
                    else -> "Not sure"
                }
            return ReadyText.capture { t -> times + " · " + t(label, emptyMap()) }
        }
        return if (override.enabled) {
            ReadyText.capture { t ->
                t("This trip · {{times}}", mapOf("times" to times))
            }
        } else {
            ReadyText(times)
        }
    }

    fun priceValue(
        price: ApiReviewPrice,
        editing: Boolean = false,
    ): PriceValue =
        when {
            editing && price.cost == 0 -> PriceValue.Free
            price.unlimited -> PriceValue.Unlimited
            price.cost == 0 -> PriceValue.Free
            price.cost == 1 -> PriceValue.OneCredit
            else -> PriceValue.Credits(price.cost)
        }

    fun price(
        price: ApiReviewPrice,
        credits: WizardCredits?,
        editing: Boolean = false,
        originalEndKind: String? = null,
        endKind: String? = null,
        mode: WizardMode = WizardMode.member,
    ): ReadyPriceUi =
        ReadyPriceUi(
            credits != WizardCredits.unlimited,
            priceValue(price, editing),
            buildList {
                if (mode == WizardMode.guest) {
                    add(
                        if (credits ==
                            WizardCredits.free
                        ) {
                            PriceLine.GuestFree
                        } else {
                            PriceLine.GuestPaid(price.cost, price.balance)
                        },
                    )
                } else if (credits == WizardCredits.free && !editing) {
                    add(PriceLine.FreeTrip)
                }
                if (credits == WizardCredits.oneFree) add(PriceLine.FreeHome)
                if (mode == WizardMode.member && !price.unlimited &&
                    price.cost > 0
                ) {
                    add(PriceLine.Balance(price.balance, credits == WizardCredits.unavailable))
                }
                if (editing) {
                    if (originalEndKind == "return_legs" && endKind != "return_legs") add(PriceLine.NoRefund)
                    add(
                        if (endKind == "return_legs" &&
                            originalEndKind != "return_legs"
                        ) {
                            PriceLine.EditReturnNote
                        } else {
                            PriceLine.EditNote
                        },
                    )
                }
            },
        )
}

private fun safely(block: () -> ReadyText): ReadyText =
    try {
        block()
    } catch (_: DataFailure) {
        ReadyText("")
    } catch (_: SerializationException) {
        ReadyText("")
    } catch (_: IllegalArgumentException) {
        ReadyText("")
    }

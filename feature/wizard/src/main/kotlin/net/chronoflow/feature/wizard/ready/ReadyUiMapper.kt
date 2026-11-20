@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.ready

import kotlinx.serialization.SerializationException
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
        val price = review?.price?.let { price(it, credits) }
        val failure = state.saveFailure
        val banner =
            when {
                failure != null -> ReadyBannerUi.SaveFailed(failure.kind, snapshot.retrySeconds)
                review?.status == PlanStatus.Modified -> ReadyBannerUi.Adjusted(review.reasons)
                state.updated == true -> ReadyBannerUi.Updated(review?.summary?.line, review?.price?.let(::priceValue))
                else -> null
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
        return if (override.enabled) {
            ReadyText.capture { t ->
                t("This trip · {{times}}", mapOf("times" to times))
            }
        } else {
            ReadyText(times)
        }
    }

    fun priceValue(price: ApiReviewPrice): PriceValue =
        when {
            price.unlimited -> PriceValue.Unlimited
            price.cost == 0 -> PriceValue.Free
            price.cost == 1 -> PriceValue.OneCredit
            else -> PriceValue.Credits(price.cost)
        }

    fun price(
        price: ApiReviewPrice,
        credits: WizardCredits?,
    ): ReadyPriceUi =
        ReadyPriceUi(
            credits != WizardCredits.unlimited,
            priceValue(price),
            buildList {
                if (credits == WizardCredits.free) add(PriceLine.FreeTrip)
                if (credits == WizardCredits.oneFree) add(PriceLine.FreeHome)
                if (!price.unlimited &&
                    price.cost > 0
                ) {
                    add(PriceLine.Balance(price.balance, credits == WizardCredits.unavailable))
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

@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiReviewPrice
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiReviewSummary
import net.chronoflow.core.model.contracts.ApiStayEnd
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripInputLeg
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.ReviewPromptTarget
import net.chronoflow.core.model.contracts.TripDirection
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.temporal.ChronoUnit

typealias Translate = (key: String, values: Map<String, String>) -> String

object WizardPresentation {
    @Serializable
    data class SourceChange(
        val form: WizardForm,
        val fields: List<Field>,
        val price: ApiReviewPrice?,
        val previousPrice: ApiReviewPrice? = null,
    ) {
        enum class Field { route, flight, returnFlight, sleep, extras }
    }

    fun sourceChangeSummary(
        change: SourceChange,
        review: ApiTripReview?,
        stayEnd: ApiStayEnd?,
        language: Language,
        t: Translate,
    ): String? {
        val price = change.price ?: return null
        val form = change.form
        val sentences = mutableListOf<String>()
        for (field in change.fields) {
            sentences.add(
                when (field) {
                    SourceChange.Field.route -> {
                        t(
                            "On another device: the route changed to {{from}} → {{to}}.",
                            mapOf(
                                "from" to (form.legs.firstOrNull()?.fromIata ?: ""),
                                "to" to (
                                    form.legs
                                        .take(form.returnBoundary)
                                        .lastOrNull()
                                        ?.toIata ?: ""
                                ),
                            ),
                        )
                    }

                    SourceChange.Field.flight -> {
                        t(
                            "On another device: the outbound flight changed to {{flight}}.",
                            mapOf("flight" to flightSummary(form, language = language, t = t)),
                        )
                    }

                    SourceChange.Field.returnFlight -> {
                        val date =
                            form.legs
                                .getOrNull(form.returnBoundary)
                                ?.departLocal
                                ?.let(WizardDateTime::date)
                        if (form.end.kind == "return_legs" &&
                            date != null
                        ) {
                            t(
                                "On another device: the return flight moved to {{date}}.",
                                mapOf("date" to WizardDateTime.format(date, WizardDateTemplate.EEE_D_MMM, language)),
                            )
                        } else {
                            t(
                                "On another device: the trip end changed to {{end}}.",
                                mapOf("end" to returnSummary(form, stayEnd, language, t)),
                            )
                        }
                    }

                    SourceChange.Field.sleep -> {
                        if (form.override.enabled) {
                            t(
                                "On another device: the sleep times changed to {{times}}.",
                                mapOf(
                                    "times" to
                                        ((form.override.habitualBed ?: "") + "–" + (form.override.habitualWake ?: "")),
                                ),
                            )
                        } else {
                            t("On another device: the trip now uses your usual sleep times.", emptyMap())
                        }
                    }

                    SourceChange.Field.extras -> {
                        t(
                            "On another device: the extras changed to {{extras}}.",
                            mapOf("extras" to extrasSummary(form, review, form.reviewInput(null), language, t)),
                        )
                    }
                },
            )
        }
        val text =
            if (price.cost ==
                0
            ) {
                t("Free", emptyMap())
            } else {
                t("{{count}} credits", mapOf("count" to price.cost.toString()))
            }
        sentences.add(
            if (change.previousPrice?.cost ==
                price.cost
            ) {
                t("The price is still {{price}}.", mapOf("price" to text))
            } else {
                t(
                    "The price is now {{price}}.",
                    mapOf("price" to text),
                )
            },
        )
        return sentences.joinToString(" ")
    }

    fun promptKey(prompt: ApiReviewPrompt): String =
        listOf(
            prompt.kind.raw,
            prompt.text.key,
            prompt.scope.stayIndex?.toString() ?: "",
            prompt.scope.legIndex?.toString() ?: "",
        ).joinToString(":")

    fun promptAnswers(
        prompt: ApiReviewPrompt,
        input: ApiTripReviewInput,
    ): Map<String, JsonElement> {
        val root = ApiJson.request.encodeToJsonElement(ApiTripInput.serializer(), input.input) as JsonObject
        val journey = if (prompt.scope.journey == TripDirection.Return) input.input.`return` else input.input.outbound
        val index = prompt.scope.legIndex ?: prompt.scope.stayIndex ?: 0
        val leg = journey?.legs?.getOrNull(index)
        val values =
            if (leg !=
                null
            ) {
                JsonObject(
                    root + (ApiJson.request.encodeToJsonElement(ApiTripInputLeg.serializer(), leg) as JsonObject),
                )
            } else {
                root
            }
        return prompt.fields.associateWith { field ->
            val value =
                if (prompt.target ==
                    ReviewPromptTarget.Profile
                ) {
                    input.profile?.get(field)
                } else {
                    pathValue(values, field)
                }
            if (value == null || value == JsonNull) prompt.answer[field] ?: JsonNull else value
        }
    }

    fun promptAnswerText(
        prompt: ApiReviewPrompt,
        input: ApiTripReviewInput,
        language: Language,
        t: Translate,
    ): String {
        val answers = promptAnswers(prompt, input)
        val first = prompt.fields.firstOrNull() ?: return t("Add", emptyMap())
        val value = answers[first]?.takeIf { it != JsonNull } ?: return t("Add", emptyMap())
        if (prompt.kind == PromptKind.PreDepartureBusyHours) {
            val count = (answers["pre_departure_busy_intervals"] as? JsonArray)?.size ?: 0
            return t("{{count}} time ranges", mapOf("count" to count.toString()))
        }
        if (prompt.kind == PromptKind.Cups) {
            if (value.stringValue == "non_user") return t("None", emptyMap())
            val count =
                WizardOptionalAnswer.drinkFields.fold(BigDecimal.ZERO) { sum, field ->
                    sum +
                        (answers[field].numberValue ?: BigDecimal.ZERO)
                }
            return t("{{count}} a day", mapOf("count" to count.stripTrailingZeros().toPlainString()))
        }
        if (prompt.kind == PromptKind.FreeDayTimes) {
            if (value.stringValue == "same_as_workdays") return t("Same as work days", emptyMap())
            val bed = answers["free_day_bed"].stringValue
            val wake = answers["free_day_wake"].stringValue
            return if (bed != null && wake != null) "$bed–$wake" else t("Different times", emptyMap())
        }
        if (prompt.kind ==
            PromptKind.Room
        ) {
            return if (value.stringValue ==
                "arrival"
            ) {
                t("When I land", emptyMap())
            } else {
                t(
                    "From {{time}}",
                    mapOf("time" to (answers["stay.bed_available_from_local"].stringValue ?: "").takeLast(5)),
                )
            }
        }
        if (prompt.kind == PromptKind.BlueBlockers && value.stringValue == "rated") {
            val rating =
                answers["blue_blocker_mdfd"].numberValue
                    ?: answers["blue_blocker_mdfd"].stringValue?.replace(',', '.')?.toBigDecimalOrNull()
            val text = rating?.setScale(0, RoundingMode.HALF_EVEN)?.toPlainString() ?: ""
            return t(
                "Rated {{rating}}",
                mapOf(
                    "rating" to
                        if (language ==
                            Language.Nl
                        ) {
                            text.replace('.', ',')
                        } else {
                            text
                        },
                ),
            )
        }
        val raw = value.numberValue?.stripTrailingZeros()?.toPlainString() ?: value.stringValue ?: value.toString()
        if (raw == "true" && value.stringValue == null) return t("Yes", emptyMap())
        if (raw == "false" && value.stringValue == null) return t("No", emptyMap())
        return when (raw) {
            "left" -> t("Window left", emptyMap())
            "right" -> t("Window right", emptyMap())
            "none" -> t("None", emptyMap())
            "box" -> t("Light box", emptyMap())
            "glasses" -> t("Light glasses", emptyMap())
            "no" -> t("No", emptyMap())
            "unrated" -> t("Yes, no rating", emptyMap())
            else -> raw
        }
    }

    fun flightSummary(
        form: WizardForm,
        home: Boolean = false,
        language: Language,
        t: Translate,
    ): String {
        val legs = if (home) form.legs.drop(form.returnBoundary) else form.legs.take(form.returnBoundary)
        val first = legs.firstOrNull() ?: return ""
        val last = legs.lastOrNull() ?: return ""
        val departure = WizardDateTime.date(first.departLocal) ?: return ""
        val arrival = WizardDateTime.date(last.arriveLocal) ?: return ""
        val start =
            WizardDateTime.format(departure, WizardDateTemplate.EEE_D_MMM, language) + " " +
                first.departLocal.takeLast(5)
        val end =
            (
                if (first.departLocal.take(10) ==
                    last.arriveLocal.take(10)
                ) {
                    ""
                } else {
                    WizardDateTime.format(arrival, WizardDateTemplate.EEE, language) +
                        " "
                }
            ) +
                last.arriveLocal.takeLast(5)
        val flight = "$start → $end"
        return if (legs.size >
            1
        ) {
            t(
                "{{flight}} · via {{airports}}",
                mapOf(
                    "flight" to flight,
                    "airports" to legs.dropLast(1).joinToString(", ") { it.toIata },
                ),
            )
        } else {
            flight
        }
    }

    fun extrasSummary(
        form: WizardForm,
        review: ApiTripReview?,
        input: ApiTripReviewInput,
        language: Language,
        t: Translate,
    ): String {
        val answered =
            (review?.prompts ?: emptyList()).count {
                !it.blocking &&
                    promptAnswerText(it, input, language, t) != t("Add", emptyMap())
            }
        val prep =
            if (form.preparation.days ==
                0
            ) {
                t("No prep days", emptyMap())
            } else {
                t(
                    "{{count}} days prep",
                    mapOf("count" to form.preparation.days.toString()),
                )
            }
        return if (answered > 0) t("{{count}} answered", mapOf("count" to answered.toString())) + " · " + prep else prep
    }

    fun returnSummary(
        form: WizardForm,
        stayEnd: ApiStayEnd?,
        language: Language,
        t: Translate,
    ): String {
        if (form.end.kind == "return_legs") return flightSummary(form, true, language, t)
        val nights = t("{{count}} nights", mapOf("count" to (form.end.nights ?: 0).toString()))
        val end =
            if (form.end.kind ==
                "check_out"
            ) {
                form.end.checkOutDate
            } else if (form.end.kind == "nights") {
                stayEnd?.checkOutLocalDate
            } else {
                null
            }
        val date = end?.let { WizardDateTime.date(it + "T12:00") }
        if (date == null) return if (form.end.kind == "nights") nights else t("One way", emptyMap())
        val text = WizardDateTime.format(date, WizardDateTemplate.EEE_D_MMM, language).replace(",", "")
        return if (form.end.kind ==
            "nights"
        ) {
            t("{{nights}} · until {{date}}", mapOf("nights" to nights, "date" to text))
        } else {
            t(
                "Check-out · {{date}}",
                mapOf("date" to text),
            )
        }
    }

    fun planDateSummary(
        form: WizardForm,
        summary: ApiReviewSummary?,
        language: Language,
        t: Translate,
    ): String {
        summary ?: return ""
        val start = WizardDateTime.parseInstant(summary.planStartUtc) ?: return ""
        val end = WizardDateTime.date(summary.planEndDate + "T12:00") ?: return ""
        val local = WizardDateTime.localDate(start, summary.planStartZone) ?: return ""
        val template = if (local.year != end.year) WizardDateTemplate.EEE_D_MMM_Y else WizardDateTemplate.EEE_D_MMM
        val range =
            WizardDateTime.format(local, template, language) + " – " + WizardDateTime.format(end, template, language)
        if (form.end.kind == "check_out") return range
        val kind =
            if (form.end.kind ==
                "nights"
            ) {
                t("{{count}} nights", mapOf("count" to (form.end.nights ?: 0).toString()))
            } else if (form.end.kind == "return_legs") {
                t("return", emptyMap())
            } else {
                t("one way", emptyMap())
            }
        return "$range · $kind"
    }

    fun planDayCount(summary: ApiReviewSummary?): Int? {
        summary ?: return null
        val start = WizardDateTime.parseInstant(summary.planStartUtc) ?: return null
        val local = WizardDateTime.localDate(start, summary.planStartZone) ?: return null
        val end = WizardDateTime.date(summary.planEndDate + "T12:00") ?: return null
        return ChronoUnit.DAYS.between(local.toLocalDate(), end.toLocalDate()).toInt() + 1
    }

    fun promptFieldError(
        prompt: ApiReviewPrompt,
        form: WizardForm,
        errors: Map<String, FieldMessage>,
    ): FieldMessage? {
        val paths =
            prompt.fields.map { field ->
                val raw =
                    if (prompt.target ==
                        ReviewPromptTarget.Profile
                    ) {
                        "profile.$field"
                    } else if (field == "latest_wake" ||
                        field.startsWith("pre_departure_")
                    ) {
                        field
                    } else {
                        "${prompt.scope.journey?.raw ?: "outbound"}.legs.${prompt.scope.legIndex ?: prompt.scope.stayIndex ?: 0}.$field"
                    }
                WizardJourney.errorPath(raw, form)
            }
        return errors
            .toSortedMap()
            .entries
            .firstOrNull { (path, _) ->
                paths.any { path == it || path.startsWith("$it.") }
            }?.value
    }

    private fun pathValue(
        root: JsonElement,
        path: String,
    ): JsonElement? =
        path.split('.').fold(root as JsonElement?) { value, key ->
            when (value) {
                is JsonArray -> key.toIntOrNull()?.let(value::getOrNull)
                is JsonObject -> value[key]
                else -> null
            }
        }
}

package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.TripDirection
import net.chronoflow.core.wizard.generationReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class WizardCompletionTests {
    class Credits {
        @Test fun creditsCoverEveryReadyVariant() {
            var review = generationReview()
            assertEquals(WizardCredits.credits, WizardCredits.of(review))
            review = review.copy(price = review.price!!.copy(freeDirections = listOf(TripDirection.Return)))
            assertEquals(WizardCredits.oneFree, WizardCredits.of(review))
            review = review.copy(price = review.price!!.copy(cost = 0))
            assertEquals(WizardCredits.free, WizardCredits.of(review))
            review = review.copy(price = review.price!!.copy(unlimited = true))
            assertEquals(WizardCredits.unlimited, WizardCredits.of(review))
            review = review.copy(canSave = false, saveBlockedReason = "Not enough credits")
            // 06 §11.3: the model exposes unavailable; 9g supplies Close only.
            assertEquals(WizardCredits.unavailable, WizardCredits.of(review))
        }

        @Test fun sheetValidationAcceptsCommaRatingAndRejectsMissingDrinks() {
            val cups =
                wizardPrompt(
                    kind = PromptKind.Cups,
                    blocking = false,
                    fields =
                        listOf(
                            "caffeine_habit",
                            "cups_filter_coffee",
                            "cups_tea",
                            "cups_cola",
                            "cups_energy_drink",
                        ),
                )
            assertNotNull(WizardOptionalAnswer.error(cups, mapOf("caffeine_habit" to JsonPrimitive("cups"))))
            assertNull(WizardOptionalAnswer.error(cups, mapOf("caffeine_habit" to JsonPrimitive("non_user"))))
            assertNull(WizardOptionalAnswer.error(cups, mapOf("cups_tea" to JsonPrimitive(1))))
            assertNotNull(WizardOptionalAnswer.error(cups, mapOf("cups_tea" to JsonPrimitive("1"))))
            val blue =
                wizardPrompt(
                    kind = PromptKind.BlueBlockers,
                    blocking = false,
                    fields = listOf("blue_blockers", "blue_blocker_mdfd"),
                )
            assertNull(
                WizardOptionalAnswer.error(
                    blue,
                    mapOf(
                        "blue_blockers" to JsonPrimitive("rated"),
                        "blue_blocker_mdfd" to JsonPrimitive("1,5"),
                    ),
                ),
            )
            assertNull(
                WizardOptionalAnswer.error(
                    blue,
                    mapOf(
                        "blue_blockers" to JsonPrimitive("rated"),
                        "blue_blocker_mdfd" to JsonPrimitive(1.5),
                    ),
                ),
            )
        }
    }

    @RunWith(Parameterized::class)
    class Ratings(
        private val text: String,
        private val valid: Boolean,
    ) {
        @Test fun sheetValidationAcceptsCommaRatingAndRejectsMissingDrinks() {
            val blue =
                wizardPrompt(
                    kind = PromptKind.BlueBlockers,
                    blocking = false,
                    fields = listOf("blue_blockers", "blue_blocker_mdfd"),
                )
            assertEquals(
                valid,
                WizardOptionalAnswer.error(
                    blue,
                    mapOf(
                        "blue_blockers" to JsonPrimitive("rated"),
                        "blue_blocker_mdfd" to JsonPrimitive(text),
                    ),
                ) ==
                    null,
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "rating={0}")
            fun cases() =
                listOf(
                    arrayOf<Any>("0", false),
                    arrayOf<Any>("12", false),
                    arrayOf<Any>("NaN", false),
                    arrayOf<Any>("", false),
                    arrayOf<Any>("0.1", true),
                    arrayOf<Any>("9,9", true),
                    arrayOf<Any>("0.09", false),
                    arrayOf<Any>("9.91", false),
                    arrayOf<Any>("Infinity", false),
                )
        }
    }

    class FreeDay {
        @Test fun declaredFreeDayNeedsBothStrictTimes() {
            val prompt = wizardPrompt(kind = PromptKind.FreeDayTimes)
            val declared = mapOf("free_day_status" to JsonPrimitive("declared"))
            assertNotNull(WizardOptionalAnswer.error(prompt, declared))
            assertNotNull(
                WizardOptionalAnswer.error(
                    prompt,
                    declared +
                        mapOf("free_day_bed" to JsonPrimitive("24:00"), "free_day_wake" to JsonPrimitive("07:00")),
                ),
            )
            assertNull(
                WizardOptionalAnswer.error(
                    prompt,
                    declared +
                        mapOf("free_day_bed" to JsonPrimitive("23:00"), "free_day_wake" to JsonPrimitive("07:00")),
                ),
            )
        }
    }
}

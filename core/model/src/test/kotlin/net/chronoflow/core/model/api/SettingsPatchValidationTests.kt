package net.chronoflow.core.model.api

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.SettingsSection
import org.junit.Test

class SettingsPatchValidationTests {
    @Test fun bodyClockAcceptsAnAllowedKey() = valid(SettingsSection.BodyClock, "habitual_bed")

    @Test fun preferencesAcceptsAnAllowedKey() = valid(SettingsSection.Preferences, "time_format")

    @Test fun remindersAcceptsAnAllowedKey() = valid(SettingsSection.Reminders, "lead_min")

    @Test fun emptyPatchFails() = invalid(JsonObject(emptyMap()))

    @Test fun unknownKeyFails() = invalid(JsonObject(mapOf("unknown" to JsonPrimitive("x"))))

    @Test fun leadMinCannotChangeBodyClock() = invalid(JsonObject(mapOf("lead_min" to JsonPrimitive(5))))

    private fun valid(
        section: SettingsSection,
        key: String,
    ) = SettingsPatchValidation.validate(section, JsonObject(mapOf(key to JsonPrimitive("x"))))

    private fun invalid(patch: JsonObject) {
        val failure =
            try {
                SettingsPatchValidation.validate(SettingsSection.BodyClock, patch)
                error("expected validation")
            } catch (failure: DataFailure.Validation) {
                failure
            }
        assertThat(failure).isEqualTo(
            DataFailure.Validation(
                mapOf(
                    "form" to
                        net.chronoflow.core.model
                            .FieldMessage("This setting cannot be changed here."),
                ),
                422,
            ),
        )
    }
}

package net.chronoflow.core.model.store

import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.OutboxKind
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class OutboxPayloadTests(
    private val label: String,
    private val section: SettingsSection,
    private val patch: String,
) {
    @Test fun rejectsUnsafeSettings() {
        val value = ApiJson.response.parseToJsonElement(patch) as JsonObject
        assertFalse(label, OutboxPayload.safeSettings(value, section))
        assertThrows(LocalMutationFailure.InvalidPayload::class.java) {
            OutboxPayload.record(OutboxChange.Settings(section, value), "id", "now")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<Any>> =
            listOf(
                arrayOf("password", SettingsSection.Preferences, """{"password":"secret"}"""),
                arrayOf("token", SettingsSection.Reminders, """{"token":"secret"}"""),
                arrayOf("recoveryCode", SettingsSection.BodyClock, """{"recovery_code":"secret"}"""),
                arrayOf("signature", SettingsSection.Preferences, """{"signature":"secret"}"""),
                arrayOf("currentPassword", SettingsSection.BodyClock, """{"current_password":"secret"}"""),
                arrayOf("nested", SettingsSection.BodyClock, """{"habitual_bed":{}}"""),
                arrayOf("nonBoolean", SettingsSection.BodyClock, """{"exclude_caffeine":"true"}"""),
                arrayOf("mixedArray", SettingsSection.Preferences, """{"hidden_categories":["a",1]}"""),
            )
    }
}

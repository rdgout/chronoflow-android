package net.chronoflow.feature.settings.security

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.feature.settings.components.settingsFormatArguments
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class SecurityTextArgumentsTests(
    private val min: String?,
) {
    @Test fun nonNumericOrMissingMinimumLeavesTheIosPlaceholderIntact() =
        runTest(timeout = 60.seconds) {
            val model =
                SettingsSecurityModel({
                    throw DataFailure.Validation(
                        mapOf(
                            "current_password" to
                                FieldMessage(
                                    "Use at least :min characters.",
                                    min?.let { mapOf("min" to JsonPrimitive(it)) }.orEmpty(),
                                ),
                        ),
                        422,
                    )
                }) { Instant.parse("2026-11-21T12:00:00Z") }
            model.showPrompt(SettingsSecurityPrompt.Enable)
            model.submitPrompt(FakeSecretField("password"))
            val key = checkNotNull(model.state.value.errors["current_password"])
            val entry = checkNotNull(CopyIndex.find(CopyTable.Settings, key))
            val arguments =
                settingsFormatArguments(
                    entry.args,
                    model.state.value.errorArguments["current_password"]
                        .orEmpty(),
                )

            assertEquals("Use at least {{min}} characters.", key)
            assertEquals(
                "Use at least {{min}} characters.",
                String.format(Locale.ENGLISH, "Use at least %1\$s characters.", *arguments),
            )
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "min={0}")
        fun cases(): List<Array<String?>> = listOf(arrayOf("12"), arrayOf(null))
    }
}

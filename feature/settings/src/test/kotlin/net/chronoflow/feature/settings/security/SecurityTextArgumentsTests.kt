package net.chronoflow.feature.settings.security

import android.content.res.Configuration
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.components.settingsText
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityTextArgumentsTests(
    private val min: String?,
    private val language: String,
    private val expected: String,
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
                }, now = { Instant.parse("2026-11-21T12:00:00Z") })
            model.showPrompt(SettingsSecurityPrompt.Enable)
            model.submitPrompt(FakeSecretField("password"))
            val key = checkNotNull(model.state.value.errors["current_password"])
            val args =
                model.state.value.errorArguments["current_password"]
                    .orEmpty()
            val application = RuntimeEnvironment.getApplication()
            val configuration =
                Configuration(application.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(language))
                }
            val resources = application.createConfigurationContext(configuration).resources
            val recomposer = Recomposer(backgroundScope.coroutineContext)
            val composition = Composition(CopyApplier(), recomposer)
            try {
                var resolved: String? = null
                composition.setContent {
                    CompositionLocalProvider(LocalResources provides resources) {
                        resolved = settingsText(key, args)
                    }
                }
                assertEquals(expected, resolved)
            } finally {
                composition.dispose()
                recomposer.cancel()
                withContext(NonCancellable) { recomposer.join() }
            }
        }

    private class CopyApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(
            index: Int,
            instance: Unit,
        ) = error("Copy resolution must not emit UI nodes")

        override fun insertBottomUp(
            index: Int,
            instance: Unit,
        ) = error("Copy resolution must not emit UI nodes")

        override fun move(
            from: Int,
            to: Int,
            count: Int,
        ) = Unit

        override fun remove(
            index: Int,
            count: Int,
        ) = Unit

        override fun onClear() = Unit
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "min={0}-{1}")
        fun cases(): List<Array<String?>> =
            listOf(
                arrayOf("12", "en", "Use at least {{min}} characters."),
                arrayOf(null, "en", "Use at least {{min}} characters."),
                arrayOf("12", "nl", "Gebruik minstens {{min}} tekens."),
                arrayOf(null, "nl", "Gebruik minstens {{min}} tekens."),
            )
    }
}

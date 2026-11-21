package net.chronoflow.core.ui

import android.content.res.Configuration
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GuestJourneyCatalogTests {
    @Test fun welcomeWizardSignupAndTripCopyStayPaired() {
        val keys =
            mapOf(
                CopyTable.Welcome to listOf("Plan my trip", "Sign in"),
                CopyTable.Wizard to
                    listOf(
                        "On a normal work day.",
                        "Your answers",
                        "Ready to plan",
                        "Make my plan",
                        "Your sign-up credits stay unused.",
                    ),
                CopyTable.Android to
                    listOf(
                        "Paid with {{cost}} of the {{balance}} credit you get when you sign up.",
                        "Paid with {{cost}} of the {{balance}} credits you get when you sign up.",
                    ),
                CopyTable.Auth to listOf("Save your plan", "Create account and save", "I already have an account"),
                CopyTable.Plan to listOf("Loading your plan", "Downloading this trip’s plan…"),
            )
        val app = RuntimeEnvironment.getApplication()

        fun localized(language: String) =
            app
                .createConfigurationContext(
                    Configuration(app.resources.configuration).apply {
                        setLocale(Locale.forLanguageTag(language))
                    },
                ).resources
        val english = localized("en")
        val dutch = localized("nl")
        keys.forEach { (table, expected) ->
            expected.forEach { key ->
                val entry = checkNotNull(CopyIndex.find(table, key)) { "Missing $table copy: $key" }
                val args = entry.args.map { "{{$it}}" }.toTypedArray()
                val en = english.getString(entry.resId, *args)
                val nl = dutch.getString(entry.resId, *args)
                assertEquals(key, en)
                assertTrue(key, nl.isNotBlank())
                assertNotEquals(key, en, nl)
                if (table == CopyTable.Android) {
                    assertTrue(nl.contains("{{cost}}"))
                    assertTrue(nl.contains("{{balance}}"))
                }
            }
        }
    }
}

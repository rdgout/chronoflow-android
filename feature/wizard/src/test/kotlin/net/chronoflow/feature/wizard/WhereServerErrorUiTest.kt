@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.fieldCopy
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.ui.WhereUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal val routeError = FieldMessage("Choose an airport from the search results.")

internal fun serverErrorDraft() = routeDraft().copy(fieldErrors = mapOf("legs.0.toIata" to routeError))

class WhereServerErrorUiTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun toErrorMarksOnlyTheToRow() {
        val ui = WhereUi.from(serverErrorDraft())
        assertEquals(listOf(false, true), ui.rows.map { it.invalid })
        assertEquals(listOf("legs.0.toIata"), ui.serverErrors.keys.toList())
    }

    @Test fun homeErrorsStayOnTheHomeRoute() {
        val draft = routeDraft(home = true).copy(fieldErrors = mapOf("legs.1.fromIata" to routeError))
        val home = WhereUi.from(draft, home = true)
        assertEquals(listOf("legs.1.fromIata"), home.serverErrors.keys.toList())
        assertTrue(home.rows.first().invalid)
        assertTrue(WhereUi.from(draft, home = false).serverErrors.isEmpty())
    }

    @Test fun outOfRangeAndNonRouteKeysAreIgnored() {
        val draft =
            routeDraft().copy(
                fieldErrors =
                    mapOf(
                        "legs.1.toIata" to routeError,
                        "legs.99.fromIata" to routeError,
                        "legs.0.departLocal" to routeError,
                        "profile.habitual_bed" to routeError,
                    ),
            )
        val ui = WhereUi.from(draft)
        assertTrue(ui.serverErrors.isEmpty())
        assertFalse(ui.rows.any { it.invalid })
    }

    @Test fun fieldCopyPrefersTheCatalogAndOtherwiseUsesTheWizardTable() {
        val catalog =
            CopyCatalog(
                """{"entries":[{"key":"both :value","arguments":["value"],"languages":{
            "en":{"kind":"string","formats":["catalog %@"]},
            "nl":{"kind":"string","formats":["catalogus %@"]}
        }}]}""".encodeToByteArray(),
            )
        val formatter = CopyLineFormatter(catalog)
        val message = FieldMessage("both :value", mapOf("value" to JsonPrimitive(3)))
        assertEquals("catalogus 3", fieldCopy(message, formatter, Language.Nl) { "wizard" })
        val fallback = FieldMessage("wizard", mapOf("value" to JsonPrimitive(4)))
        assertEquals(
            "wizard:4",
            fieldCopy(fallback, formatter, Language.Nl) {
                "${it.key}:${it.arguments["value"]}"
            },
        )
    }

    @Test fun routeEditClearsTheServerError() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = serverErrorDraft() }) { fixture ->
                runCurrent()
                fixture.model.onEvent(WizardEvent.SwapAirports)
                runCurrent()
                assertTrue(
                    fixture.model.uiState.value!!
                        .where!!
                        .serverErrors
                        .isEmpty(),
                )
                assertTrue(
                    fixture.model.wizard!!
                        .draft.fieldErrors
                        .isEmpty(),
                )
            }
        }
}

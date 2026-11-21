@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.bodyclock

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.feature.settings.support.SettingsFixtureTest
import net.chronoflow.feature.settings.support.withSettingsFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SettingsBodyClockStoreTests : SettingsFixtureTest() {
    @Test fun reopenedEditorUsesChronotypeObservedFromLocalSave() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                f.settings.upsert(SettingsRecord("bodyClock", """{"chronotype":"early"}"""))
                val editor = f.bodyClock()
                runCurrent()
                assertEquals(
                    JsonPrimitive("early"),
                    editor.ui.value.editor!!
                        .answers["chronotype"],
                )
                editor.event(BodyClockEvent.Choice(BodyClockGroup.About, "chronotype", "late"))
                runCurrent()
                assertTrue(
                    BodyClockGroup.About in
                        editor.ui.value.editor!!
                            .saved,
                )
                val stored =
                    json(
                        f.settings
                            .settings()
                            .single { it.section == "bodyClock" }
                            .value,
                    )
                assertEquals(JsonPrimitive("late"), stored["chronotype"])
                val reopened = f.bodyClock()
                runCurrent()
                assertEquals(
                    JsonPrimitive("late"),
                    reopened.ui.value.editor!!
                        .answers["chronotype"],
                )
                assertTrue(
                    reopened.ui.value.editor!!
                        .saved
                        .isEmpty(),
                )
                assertFalse(editor.ui.value.pages.failed)
            }
        }

    @Test fun chronotypeSelectionIsSavedLocallyAndQueuedBeforeShowingSaved() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                f.settings.upsert(SettingsRecord("bodyClock", """{"ageBand":"18_64"}"""))
                val editor = f.bodyClock()
                runCurrent()
                editor.event(BodyClockEvent.Choice(BodyClockGroup.About, "chronotype", "late"))
                runCurrent()
                assertEquals(
                    setOf(BodyClockGroup.About),
                    editor.ui.value.editor!!
                        .saved,
                )
                assertFalse(
                    editor.ui.value.editor!!
                        .failure,
                )
                assertTrue(
                    editor.ui.value.editor!!
                        .errors
                        .isEmpty(),
                )
                f.reopen()
                val stored =
                    json(
                        f.settings
                            .settings()
                            .single { it.section == "bodyClock" }
                            .value,
                    )
                assertEquals(JsonPrimitive("late"), stored["chronotype"])
                assertEquals(JsonPrimitive("18_64"), stored["ageBand"])
                val row = f.outbox.pending().single()
                assertEquals("settings", row.kind)
                val payload = json(row.payload)
                assertEquals(JsonPrimitive("bodyClock"), payload["section"])
                val change = OutboxPayload.change(row) as OutboxChange.Settings
                assertEquals(SettingsSection.BodyClock, change.section)
                assertEquals(JsonPrimitive("late"), change.patch["chronotype"])
            }
        }
}

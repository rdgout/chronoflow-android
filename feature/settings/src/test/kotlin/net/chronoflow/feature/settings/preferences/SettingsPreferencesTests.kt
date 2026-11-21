package net.chronoflow.feature.settings.preferences

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.settings.bodyclock.json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SettingsPreferencesTests {
    @Test fun groupsPreserveUnrelatedAndFutureCategoriesAndOnlySendChangedSetting() =
        runTest(timeout = 60.seconds) {
            val writes = mutableListOf<JsonObject>()
            val model =
                SettingsPreferencesModel(
                    json("""{"timeFormat":"24","hiddenCategories":["flight","future_category"]}"""),
                ) { writes += it }
            model.setShown(false, SettingsTimelineGroup.all[0])
            assertEquals(listOf("flight", "future_category", "sleep", "nap", "rest"), model.hidden)
            assertEquals(setOf("hidden_categories"), writes.last().keys)
            model.setShown(true, SettingsTimelineGroup.all[0])
            assertEquals(listOf("flight", "future_category"), model.hidden)
            model.setTimeFormat("12")
            assertEquals(json("""{"time_format":"12"}"""), writes.last())
            assertEquals(listOf("flight", "future_category"), model.hidden)
            val count = writes.size
            model.setTimeFormat("13")
            assertTrue(model.state.value.failure)
            assertEquals(count, writes.size)
        }

    @Test fun localFailureKeepsDraftThroughIncomingValuesAndCanRetry() =
        runTest(timeout = 60.seconds) {
            var failing = true
            val model =
                SettingsPreferencesModel(json("""{"timeFormat":"24"}""")) {
                    if (failing) throw StoreFailure.Closed
                }
            model.setTimeFormat("12")
            model.receive(json("""{"timeFormat":"24"}"""))
            assertEquals(JsonPrimitive("12"), model.state.value.value["timeFormat"])
            assertTrue(model.state.value.failure)
            failing = false
            model.setTimeFormat("12")
            assertTrue(model.state.value.saved)
            assertFalse(model.state.value.failure)
        }

    @Test fun missingTimeFormatReadsAs24() {
        assertEquals("24", SettingsPreferencesModel(json("{}")) {}.state.value.timeFormat)
    }
}

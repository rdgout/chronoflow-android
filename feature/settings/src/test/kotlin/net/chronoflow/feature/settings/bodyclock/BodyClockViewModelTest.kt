@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.bodyclock

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

internal const val MISSING_BODY_READINESS =
    """{"variant":"needs-answers","missing":[{"question":null},{"question":"shift_work"}],"invalidSections":[]}"""
internal const val DEFAULT_BODY = """{"habitualBed":"23:00","habitualWake":"07:00"}"""

internal fun bodyDependencies(
    body: String = DEFAULT_BODY,
    readiness: String? = MISSING_BODY_READINESS,
) = FakeSettingsDependencies().apply {
    settings.value = listOf(SettingsRecord("bodyClock", body)) +
        listOfNotNull(readiness?.let { SettingsRecord("readiness", it) })
}

class BodyClockViewModelTest {
    @Test fun missingReadinessHasOnlyTheTwoRequiredAnswers() =
        runTest(timeout = 60.seconds) {
            withBodyClock(bodyDependencies()) { vm ->
                assertEquals(BodyClockReadiness.NeedsAnswers(2, 2, 0), vm.ui.value.readiness)
                assertEquals(
                    setOf("ageBand", "shiftWork"),
                    vm.ui.value.sections.values
                        .flatMap { it.missing }
                        .toSet(),
                )
                vm.event(BodyClockEvent.FirstMissing)
                val scroll = checkNotNull(vm.ui.value.scroll)
                assertEquals(BodyClockGroup.About, scroll.group)
                assertEquals("ageBand", scroll.fieldKey)
                vm.event(BodyClockEvent.ScrollHandled(scroll.id))
                assertNull(vm.ui.value.scroll)
            }
        }

    @Test fun declaredFreeDaysPrefillHabitualTimes() =
        runTest(timeout = 60.seconds) {
            val deps = bodyDependencies()
            withBodyClock(deps) { vm ->
                vm.event(BodyClockEvent.Choice(BodyClockGroup.Sleep, "freeDayStatus", "declared"))
                runCurrent()
                val patch = (OutboxPayload.change(deps.rows.value.single()) as OutboxChange.Settings).patch
                assertEquals(JsonPrimitive("23:00"), patch["free_day_bed"])
                assertEquals(JsonPrimitive("07:00"), patch["free_day_wake"])
                assertEquals(
                    BodyClockGroup.Sleep.fields
                        .map(BodyClockGroup::requestKey)
                        .toSet(),
                    patch.keys,
                )
            }
        }

    @Test fun cupsWithZeroDrinksShowsTheFieldErrorAndDoesNotEnqueue() =
        runTest(timeout = 60.seconds) {
            val deps = bodyDependencies()
            withBodyClock(deps) { vm ->
                vm.event(BodyClockEvent.Choice(BodyClockGroup.Caffeine, "caffeineHabit", "cups"))
                runCurrent()
                assertTrue(deps.rows.value.isEmpty())
                assertEquals(
                    SettingsBodyClockModel.CUPS_ERROR,
                    vm.ui.value.editor!!
                        .errors["cupsFilterCoffee"],
                )
            }
        }

    @Test fun dutchRatingCommaIsNormalizedBeforeEnqueue() =
        runTest(timeout = 60.seconds) {
            val deps = bodyDependencies("""{"blueBlockers":"rated","blueBlockerMdfd":"2.0"}""")
            withBodyClock(deps) { vm ->
                vm.event(BodyClockEvent.Rating("2,1"))
                runCurrent()
                val patch = (OutboxPayload.change(deps.rows.value.single()) as OutboxChange.Settings).patch
                assertEquals(JsonPrimitive("2.1"), patch["blue_blocker_mdfd"])
            }
        }

    @Test fun timeSheetIsHeldUntilConfirmOrDismissAndRoundsOnDone() =
        runTest(timeout = 60.seconds) {
            val deps = bodyDependencies()
            withBodyClock(deps) { vm ->
                vm.event(BodyClockEvent.OpenTime("habitualBed"))
                assertEquals(BodyClockTimeSheet("habitualBed", 23, 0), vm.ui.value.timeSheet)
                vm.event(BodyClockEvent.DismissTime)
                assertNull(vm.ui.value.timeSheet)
                assertTrue(deps.rows.value.isEmpty())
                vm.event(BodyClockEvent.OpenTime("habitualBed"))
                vm.event(BodyClockEvent.ConfirmTime("habitualBed", 23, 13))
                runCurrent()
                assertNull(vm.ui.value.timeSheet)
                val patch = (OutboxPayload.change(deps.rows.value.single()) as OutboxChange.Settings).patch
                assertEquals(JsonPrimitive("23:15"), patch["habitual_bed"])
            }
        }

    @Test fun untouchedUnsetTimeAlwaysWritesNoonOnDone() =
        runTest(timeout = 60.seconds) {
            val deps = bodyDependencies("""{"habitualWake":"20:00"}""")
            withBodyClock(deps) { vm ->
                vm.event(BodyClockEvent.OpenTime("habitualBed"))
                assertEquals(BodyClockTimeSheet("habitualBed", 12, 0), vm.ui.value.timeSheet)
                vm.event(BodyClockEvent.ConfirmTime("habitualBed", 12, 0))
                runCurrent()
                val patch = (OutboxPayload.change(deps.rows.value.single()) as OutboxChange.Settings).patch
                assertEquals(JsonPrimitive("12:00"), patch["habitual_bed"])
            }
        }

    @Test fun consecutiveSameGroupSavesEachHoldANewScrollUntilAcknowledged() =
        runTest(timeout = 60.seconds) {
            withBodyClock(bodyDependencies()) { vm ->
                vm.event(BodyClockEvent.Choice(BodyClockGroup.About, "chronotype", "late"))
                runCurrent()
                val first = checkNotNull(vm.ui.value.scroll)
                assertTrue(
                    vm.ui.value.sections
                        .getValue(BodyClockGroup.About)
                        .saved,
                )
                vm.event(BodyClockEvent.Choice(BodyClockGroup.About, "chronotype", "early"))
                runCurrent()
                val second = checkNotNull(vm.ui.value.scroll)
                assertTrue(second.id > first.id)
                vm.event(BodyClockEvent.ScrollHandled(first.id))
                assertEquals(second, vm.ui.value.scroll)
                vm.event(BodyClockEvent.ScrollHandled(second.id))
                assertNull(vm.ui.value.scroll)
            }
        }

    @Test fun completionOfAnOlderSaveDoesNotScrollToANewerInvalidDraft() =
        runTest(timeout = 60.seconds) {
            val deps = bodyDependencies()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            deps.saveSettings = { _, _ ->
                entered.complete(Unit)
                release.await()
            }
            withBodyClock(deps) { vm ->
                vm.change(BodyClockGroup.Sleep, mapOf("habitualBed" to JsonPrimitive("23:00")))
                entered.await()
                vm.change(BodyClockGroup.Sleep, mapOf("habitualWake" to JsonPrimitive("23:30")))
                runCurrent()
                release.complete(Unit)
                runCurrent()
                assertNull(vm.ui.value.scroll)
                assertFalse(
                    vm.ui.value.sections
                        .getValue(BodyClockGroup.Sleep)
                        .saved,
                )
                assertNotNull(
                    vm.ui.value.editor!!
                        .errors["habitualWake"],
                )
            }
        }

    @Test fun foreignCancellationIsSilentAndDoesNotScroll() =
        runTest(timeout = 60.seconds) {
            val deps = bodyDependencies().apply { current = false }
            withBodyClock(deps) { vm ->
                vm.event(BodyClockEvent.Choice(BodyClockGroup.About, "chronotype", "late"))
                runCurrent()
                assertFalse(
                    vm.ui.value.editor!!
                        .failure,
                )
                assertTrue(deps.rows.value.isEmpty())
                assertNull(vm.ui.value.scroll)
            }
        }

    @Test fun checkedSaveFailureShowsTheBannerState() =
        runTest(timeout = 60.seconds) {
            val deps = bodyDependencies().apply { saveSettings = { _, _ -> throw IOException() } }
            withBodyClock(deps) { vm ->
                vm.event(BodyClockEvent.Choice(BodyClockGroup.About, "chronotype", "late"))
                runCurrent()
                assertTrue(
                    vm.ui.value.editor!!
                        .failure,
                )
            }
        }

    @Test fun pendingBodyClockWithoutReadinessStillHasItsEditor() =
        runTest(timeout = 60.seconds) {
            withBodyClock(bodyDependencies(readiness = null)) { vm ->
                assertNull(vm.ui.value.readiness)
                assertNotNull(vm.ui.value.editor)
            }
        }
}

internal suspend fun TestScope.withBodyClock(
    deps: FakeSettingsDependencies,
    block: suspend (BodyClockViewModel) -> Unit,
) {
    val vm = BodyClockViewModel(deps, StandardTestDispatcher(testScheduler))
    val store = ViewModelStore().apply { put("bodyClock", vm) }
    try {
        runCurrent()
        block(vm)
    } finally {
        withContext(NonCancellable) {
            store.clear()
            vm.viewModelScope.coroutineContext.job
                .cancelAndJoin()
        }
    }
}

package net.chronoflow.feature.settings.bodyclock

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BodyClockRatingBindingTest : SettingsScreenTest() {
    @Test fun bindingFollowsPullsWithoutSavingThemAndOnlyWritesUserChanges() {
        val main = StandardTestDispatcher()
        val deps = bodyDependencies("""{"blueBlockers":"rated","blueBlockerMdfd":"2.0"}""")
        val vm = BodyClockViewModel(deps, main)
        val store = ViewModelStore().apply { put("bodyClock", vm) }
        lateinit var field: androidx.compose.foundation.text.input.TextFieldState
        try {
            main.scheduler.runCurrent()
            show {
                val ui by vm.ui.collectAsState()
                field =
                    rememberBodyClockRatingField(
                        vm,
                        ui.editor
                            ?.answers
                            ?.get("blueBlockerMdfd")
                            .stringValue()
                            .orEmpty(),
                        ui.editor != null,
                    )
            }
            main.scheduler.runCurrent()
            assertEquals(0, deps.settingsSaves)
            deps.settings.value =
                listOf(
                    SettingsRecord("bodyClock", """{"blueBlockers":"rated","blueBlockerMdfd":"2.1"}"""),
                )
            main.scheduler.runCurrent()
            compose.waitForIdle()
            main.scheduler.runCurrent()
            assertEquals("2.1", field.text.toString())
            assertEquals(0, deps.settingsSaves)
            compose.runOnIdle { field.setTextAndPlaceCursorAtEnd("2,2") }
            compose.waitForIdle()
            main.scheduler.runCurrent()
            compose.waitForIdle()
            main.scheduler.runCurrent()
            assertEquals(1, deps.settingsSaves)
        } finally {
            runTest(main, timeout = 60.seconds) {
                withContext(NonCancellable) {
                    store.clear()
                    vm.viewModelScope.coroutineContext.job
                        .cancelAndJoin()
                }
            }
        }
    }

    @Test fun initialValueAndExternalChangesNeverBecomeEdits() {
        val field = BodyClockRatingField("2.0")
        assertNull(field.edit("2.0"))
        field.receive("2.1")
        assertEquals("2.1", field.state.text.toString())
        assertNull(field.edit(field.state.text.toString()))
    }

    @Test fun commaEchoKeepsTheCursorDraftAndAnExternalValueStillUpdatesIt() {
        val field = BodyClockRatingField("2.0")
        field.state.setTextAndPlaceCursorAtEnd("2,1")
        assertEquals("2,1", field.edit("2,1"))
        field.receive("2.1")
        assertEquals("2,1", field.state.text.toString())
        assertNull(field.edit(field.state.text.toString()))
        field.receive("2.2")
        assertEquals("2.2", field.state.text.toString())
        assertNull(field.edit(field.state.text.toString()))
    }

    @Test fun aModelEchoNeverOverwritesInputThatIsAheadOfIt() {
        val field = BodyClockRatingField("2.0")
        field.state.setTextAndPlaceCursorAtEnd("2.19")
        field.edit("2.19")
        field.receive("2.1")
        assertEquals("2.19", field.state.text.toString())
        field.receive("2.19")
        assertEquals("2.19", field.state.text.toString())
    }
}

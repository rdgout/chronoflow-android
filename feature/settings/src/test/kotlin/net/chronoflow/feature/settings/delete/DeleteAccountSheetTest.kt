@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.delete

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsScreenTest
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeleteAccountSheetTest : SettingsScreenTest() {
    private val fixture = DeleteRecreationFixture()
    private val store = ViewModelStore()
    private var model: DeleteAccountViewModel? = null
    private var dismissed = 0
    private var sent = 0

    @After fun clearModel() =
        runTest(fixture.dispatcher, timeout = 60.seconds) {
            withContext(NonCancellable) {
                store.clear()
                model
                    ?.viewModelScope
                    ?.coroutineContext
                    ?.job
                    ?.cancelAndJoin()
                fixture.scope.coroutineContext.job
                    .cancelAndJoin()
            }
        }

    private fun route(): DeleteAccountViewModel {
        val model = DeleteAccountViewModel(fixture.deps, fixture.dispatcher)
        this.model = model
        store.put("delete", model)
        fixture.scheduler.runCurrent()
        show { DeleteAccountRoute(model, { dismissed++ }) }
        fixture.scheduler.runCurrent()
        return model
    }

    @Test fun keepDismissesAndSendsNothing() {
        fixture.deps.delete = { sent++ }
        route()
        compose.onNodeWithTag("settings.delete.keep").performClick()
        fixture.scheduler.runCurrent()
        assertEquals(1, dismissed)
        assertEquals(0, sent)
    }

    @Test fun deleteMyAccountShowsTheDeletePrompt() {
        route()
        compose.onNodeWithTag("settings.delete.confirm").performClick()
        fixture.scheduler.runCurrent()
        compose.onNodeWithTag("settings.delete.prompt").assertExists()
        compose.onNodeWithTag("settings.delete.password").assertExists()
        compose.onNodeWithTag("settings.delete.submit").assertIsNotEnabled()
    }

    private fun failure(failure: DataFailure) {
        fixture.deps.delete = { throw failure }
        val model = route()
        compose.onNodeWithTag("settings.delete.confirm").performClick()
        fixture.scheduler.runCurrent()
        model.event(DeleteAccountEvent.Submit(FakeSecretField("pw")))
        fixture.scheduler.runCurrent()
    }

    @Test fun wrongPasswordShowsTheErrorUnderTheField() {
        failure(DataFailure.Validation(mapOf("password" to FieldMessage("The provided password was incorrect.")), 422))
        compose.onNodeWithTag("settings.delete.error").assertExists()
        compose.onNodeWithTag("settings.delete.password").assertExists()
    }

    @Test fun billingFailureShowsTheBillingSentence() {
        failure(DataFailure.Validation(mapOf("billing" to FieldMessage("billing")), 422))
        compose.onNodeWithTag("settings.delete.error").assertExists()
    }

    @Test fun throttleShowsTheCountdownLine() {
        failure(DataFailure.Throttled(30.0, 429))
        compose.onNodeWithTag("settings.delete.error").assertExists()
        compose.onNodeWithTag("settings.delete.submit").assertIsNotEnabled()
    }

    @Test fun sheetSurvivesRecreationWithoutThePassword() {
        DeleteRecreationActivity.fixture = fixture
        val controller = Robolectric.buildActivity(DeleteRecreationActivity::class.java).setup()
        try {
            fixture.scheduler.runCurrent()
            compose.onNodeWithTag("settings.delete.confirm").performClick()
            fixture.scheduler.runCurrent()
            val model = checkNotNull(fixture.model)
            compose.onNodeWithTag("settings.delete.password").performTextInput("memory-only")
            compose.onNodeWithTag("settings.delete.submit").assertIsEnabled()
            controller.recreate()
            fixture.scheduler.runCurrent()
            assertSame(model, fixture.model)
            assertEquals(SettingsDeleteModel.Stage.Password, model.ui.value.delete.stage)
            compose.onNodeWithTag("settings.delete.submit").assertIsNotEnabled()
        } finally {
            controller.pause().stop().destroy()
            fixture.scheduler.runCurrent()
        }
    }
}

internal class DeleteRecreationFixture {
    val scheduler = TestCoroutineScheduler()
    val dispatcher = StandardTestDispatcher(scheduler)
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    val deps = FakeSettingsDependencies(scope)
    var model: DeleteAccountViewModel? = null
    val factory = viewModelFactory { initializer { DeleteAccountViewModel(deps, dispatcher) } }
}

internal class DeleteRecreationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    val model: DeleteAccountViewModel = viewModel(factory = fixture.factory)
                    SideEffect { fixture.model = model }
                    DeleteAccountRoute(model, {})
                }
            }
        }
    }

    companion object {
        lateinit var fixture: DeleteRecreationFixture
    }
}

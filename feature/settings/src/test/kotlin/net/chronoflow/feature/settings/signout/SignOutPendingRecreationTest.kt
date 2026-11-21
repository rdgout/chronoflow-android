@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.signout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.home.SettingsHomeRoute
import net.chronoflow.feature.settings.home.SettingsHomeViewModel
import net.chronoflow.feature.settings.home.check
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignOutPendingRecreationTest {
    private val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun pendingSheetSurvivesRecreation() {
        SettingsScreenTest.warmNativeRuntime()
        val fixture = SettingsRecreationFixture()
        SettingsRecreationActivity.fixture = fixture
        val host = Robolectric.buildActivity(SettingsRecreationActivity::class.java).setup()
        try {
            compose.runOnIdle { fixture.scheduler.runCurrent() }
            val model = checkNotNull(fixture.model)
            compose.runOnIdle {
                model.signOut()
                fixture.scheduler.runCurrent()
            }
            compose.onNodeWithTag("settings.pending").assertExists()
            host.recreate()
            compose.runOnIdle { fixture.scheduler.runCurrent() }
            assertSame(model, fixture.model)
            compose.onNodeWithTag("settings.pending").assertExists()
        } finally {
            host.pause().stop().destroy()
            fixture.scheduler.runCurrent()
        }
    }
}

internal class SettingsRecreationFixture {
    val scheduler = TestCoroutineScheduler()
    val main = StandardTestDispatcher(scheduler)
    val deps = FakeSettingsDependencies().apply { rows.value = listOf(check("1")) }
    var model: SettingsHomeViewModel? = null
    val factory = viewModelFactory { initializer { SettingsHomeViewModel(deps, main) } }
}

internal class SettingsRecreationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    val model: SettingsHomeViewModel = viewModel(factory = fixture.factory)
                    SideEffect { fixture.model = model }
                    SettingsHomeRoute(model, fixture.deps, true, {})
                }
            }
        }
    }

    companion object {
        lateinit var fixture: SettingsRecreationFixture
    }
}

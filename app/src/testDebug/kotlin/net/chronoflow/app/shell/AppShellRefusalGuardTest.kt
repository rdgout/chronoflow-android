package net.chronoflow.app.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.layout.WindowPolicy
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.sync.SyncRefusal
import net.chronoflow.core.sync.SyncRefusalNotice
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.sync.refusal.RefusedChangeModel
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.plan.LocalRefusedChanges
import net.chronoflow.core.ui.plan.RefusedChangeDebug
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class AppShellRefusalGuardTest(
    private val profile: DeviceProfile,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun refusalStaysInsideTheSafeInsets() {
        val model = RefusedChangeModel()
        val before = RefusedChangeDebug.presentedCount.get()
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    CompositionLocalProvider(LocalRefusedChanges provides model) {
                        val holder = rememberShellViewModel(Route.Trips)
                        Box(Modifier.fillMaxSize().testTag("test.window")) {
                            AppShellScaffold(holder, {}, {}, {}, {}, {}, {})
                        }
                    }
                }
            }
        }
        compose.runOnIdle {
            model.receive(
                SyncResult(
                    SyncState.Idle,
                    0,
                    refusals =
                        listOf(
                            SyncRefusal("one", "checkOff", 422, "now", "refused", SyncRefusalNotice.SleepTimes),
                        ),
                ),
            )
        }
        compose.waitUntil(2_000) { RefusedChangeDebug.presentedCount.get() == before + 1 }
        val window = compose.onNodeWithTag("test.window").fetchSemanticsNode().boundsInRoot
        val refusal = compose.onNodeWithTag("sync.refusal").fetchSemanticsNode().boundsInRoot
        val safeBottom = window.bottom - profile.insets.bottom * profile.density
        val safeRight = window.right - profile.insets.right * profile.density
        val layout = if (profile.widthDp >= WindowPolicy.wideBreakpointDp) "rail" else "bar"
        compose.onNodeWithTag("cf.nav.$layout").assertExists()
        assertTrue("refusal must have visible content", refusal.height > 0)
        assertTrue("refusal bottom ${refusal.bottom} exceeds safe bottom $safeBottom", refusal.bottom <= safeBottom)
        println("${profile.name}: refusal right=${refusal.right}, safe right=$safeRight, window right=${window.right}")
        assertTrue("refusal right ${refusal.right} exceeds safe right $safeRight", refusal.right <= safeRight)
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                DeviceProfiles.landscapeSideNav,
                DeviceProfiles.foldInner,
                DeviceProfiles.landscapeSideNav.copy(name = "narrowLandscapeSideNav", widthDp = 640, heightDp = 360),
                DeviceProfiles.landscapeSideNav.copy(name = "barLandscapeSideNav", widthDp = 560, heightDp = 360),
            ).map { arrayOf(it) }
    }
}

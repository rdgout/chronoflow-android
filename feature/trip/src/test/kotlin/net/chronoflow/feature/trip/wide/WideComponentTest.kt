package net.chronoflow.feature.trip.wide

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.SafeInsetsDp
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.WindowSizeFake
import net.chronoflow.core.testing.compose.WindowSizeFakeHost
import net.chronoflow.feature.trip.page.TripPageTestRig
import org.junit.After
import org.junit.Rule
import org.junit.rules.RuleChain

abstract class WideComponentTest {
    protected val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(
                    DeviceProfiles.foldInner.copy(widthDp = 960, heightDp = 951, insets = SafeInsetsDp(0f, 0f, 0f, 0f)),
                ),
            ).around(compose)
    protected var rig: TripPageTestRig? = null
    protected lateinit var window: WindowSizeFake

    protected fun start(
        width: Int = 669,
        content: (@Composable (TripPageTestRig) -> Unit)? = null,
    ): TripPageTestRig {
        val host = TripPageTestRig("S3").also { rig = it }
        window = WindowSizeFake(DpSize(width.dp, 951.dp))
        compose.setContent {
            WindowSizeFakeHost(window) {
                ChronoFlowTheme(reduceMotion = true) {
                    if (content != null) content(host) else host.Content()
                }
            }
        }
        compose.waitForIdle()
        return host
    }

    @After fun close() {
        rig?.close()
    }
}

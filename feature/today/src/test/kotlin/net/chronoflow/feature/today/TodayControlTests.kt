package net.chronoflow.feature.today

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.clock.MinuteClock
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodayControlTests {
    private val profile = DeviceProfiles.small.copy(fontScale = 1.3f)
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, AppLanguage.Nl)).around(compose)

    @Test fun testEachRowStateKeepsOneLargeTapTargetAtXXLarge() {
        lateinit var formatter: CopyLineFormatter
        lateinit var row: TodayRowUi
        runTest(timeout = 60.seconds) {
            formatter = todayFormatter(StandardTestDispatcher(testScheduler))
            row = TodayBoardFixture.row(formatter, Language.Nl)
        }
        val clock = MinuteClock(mutableStateOf(TodayBoardFixture.rowNow))
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    TodayRows(row, clock, Language.Nl, formatter)
                }
            }
        }
        assertControl("running")
        assertControl("done")
        assertControl("skipped")
    }

    private fun assertControl(id: String) {
        val tag = "today.step.actionable.$id"
        val scope = hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag))
        compose.onAllNodes(scope and hasClickAction(), useUnmergedTree = true).assertCountEquals(1)
        val r = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val d = profile.density
        assertTrue("$id must keep a 56 dp target", r.height / d >= 56)
        assertTrue("$id crosses the left gutter", r.left / d >= 15)
        assertTrue("$id crosses the right gutter", r.right / d <= profile.widthDp - 15)
    }
}

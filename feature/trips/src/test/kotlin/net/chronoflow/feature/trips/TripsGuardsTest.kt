package net.chronoflow.feature.trips

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.sync.refresh.RefreshFeedbackState
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TripsGuardsTest(
    private val case: LayoutCase,
    private val stateName: String,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun screenFitsTheProfileAndSafeArea() {
        val language = if (case.language == AppLanguage.Nl) Language.Nl else Language.En
        var content: TripsUiState = TripsUiState.Loading
        lateinit var formatter: CopyLineFormatter
        runTest(timeout = 60.seconds) {
            formatter = CopyLineFormatter(CopyCatalog.load(StandardTestDispatcher(testScheduler)))
            content =
                when (stateName) {
                    "Content" -> {
                        TripsBoardFixture.content(
                            formatter,
                            language,
                        )
                    }

                    "Empty" -> {
                        TripsUiState.Empty
                    }

                    else -> {
                        TripsUiState.Loading
                    }
                }
        }
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    TripsContent(content, RefreshFeedbackState(), language, {}, {}, {}, {}, formatter = formatter)
                }
            }
        }
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile)
        Guards.assertNoInteractiveOverlap(compose)
        Guards.assertInk(
            compose,
            case.profile,
            minChannelDifference = if (stateName == "Loading") 2 else 16,
        )
        if (case.profile.name == "foldInner" && stateName == "Content") {
            compose.onNodeWithTag("trips.list").performScrollToIndex(0)
            val bounds = compose.onNodeWithTag("trip.active").fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.width <= 640 * case.profile.density + 1)
            assertTrue(abs(bounds.center.x - case.profile.widthDp * case.profile.density / 2) <= case.profile.density)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> {
            val minimum = LayoutCase(DeviceProfiles.largeFont.copy(name = "minimum", widthDp = 280), AppLanguage.Nl)
            return (LayoutMatrix.wide + minimum).flatMap { case ->
                listOf("Content", "Empty", "Loading").map { arrayOf<Any>(case, it) }
            }
        }
    }
}

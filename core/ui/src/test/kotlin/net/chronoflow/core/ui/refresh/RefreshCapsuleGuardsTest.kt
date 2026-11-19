package net.chronoflow.core.ui.refresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.sync.refresh.RefreshFeedback
import net.chronoflow.core.sync.refresh.RefreshFeedbackKey
import net.chronoflow.core.sync.refresh.RefreshFeedbackState
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class RefreshCapsuleGuardsTest(
    private val case: LayoutCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun fitsAndWrapsWithoutClipping() {
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme {
                    Box(Modifier.fillMaxSize()) {
                        RefreshFeedbackCapsule(
                            RefreshFeedbackState(
                                feedback =
                                    listOf(
                                        RefreshFeedback(RefreshFeedbackKey.TripsAdded, 3),
                                        RefreshFeedback(RefreshFeedbackKey.TripsRemoved, 1),
                                    ),
                                presentationId = 1,
                            ),
                            {
                            },
                            RefreshDebugProbe.None,
                        )
                    }
                }
            }
        }
        Guards.assertTextFits(compose, case.profile)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = LayoutMatrix.parameters(LayoutMatrix.phone)
    }
}

package net.chronoflow.feature.auth.verify

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.auth.model.VerifyState
import net.chronoflow.feature.auth.signin.authBoard
import org.junit.Assert.assertEquals
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
class VerifyBoardGeometryTest(
    private val failed: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(authBoard)).around(compose)

    @Test fun initialBoardViewportKeepsTheBrandAndHeadingAtTheirInsets() {
        compose.setContent {
            ProfileHost(authBoard) {
                ChronoFlowTheme {
                    VerifyEmailContent(
                        VerifyEmailUiState("rick@example.com", if (failed) VerifyState.Failed else VerifyState.Waiting),
                        { if (failed) 0 else 42 },
                        {},
                    )
                }
            }
        }
        compose.onNodeWithTag("auth.brand").assertIsDisplayed()
        assertEquals(
            16f,
            compose
                .onNodeWithTag("auth.brand.row")
                .getUnclippedBoundsInRoot()
                .top.value,
            0.5f,
        )
        assertEquals(
            if (failed) 160f else 96f,
            compose
                .onNodeWithTag("auth.heading")
                .getUnclippedBoundsInRoot()
                .top.value,
            0.5f,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "failed={0}")
        fun cases() = listOf(false, true)
    }
}

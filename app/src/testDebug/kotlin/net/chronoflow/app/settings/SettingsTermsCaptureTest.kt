package net.chronoflow.app.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.captureWindow
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.onboarding.terms.TermsScreen
import net.chronoflow.feature.onboarding.terms.TermsUiState
import net.chronoflow.feature.settings.home.PendingChange
import net.chronoflow.feature.settings.home.PendingText
import net.chronoflow.feature.settings.signout.SignOutPendingSheet
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsTermsCaptureTest {
    private val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun capture() {
        org.robolectric.nativeruntime.DefaultNativeRuntimeLoader
            .injectAndLoad()
        android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            host.get().setContent {
                ProfileHost(DeviceProfiles.pixel8) {
                    ChronoFlowTheme(reduceMotion = true) {
                        Box(Modifier.fillMaxSize().testTag("terms.capture.window")) {
                            TermsScreen(
                                TermsUiState(
                                    policies = AccountPolicies("1", "1"),
                                    pendingSignOutCount = 2,
                                ),
                                {},
                                signOutConfirmation = {
                                    _,
                                    stay,
                                    discard,
                                    ->
                                    SignOutPendingSheet(
                                        listOf(
                                            PendingChange(
                                                "check",
                                                BlockType.AvoidLight,
                                                PendingText.CheckedOff(BlockType.AvoidLight),
                                            ),
                                            PendingChange("sleep", BlockType.Sleep, PendingText.Sleep),
                                        ),
                                        false,
                                        stay,
                                        discard,
                                    )
                                },
                            )
                        }
                    }
                }
            }
            compose.waitForIdle()
            BoardCapture.captureWindow(compose, "SignOutPending_pixel8_en_light", "terms.capture.window")
        } finally {
            host.pause().stop().destroy()
        }
    }
}

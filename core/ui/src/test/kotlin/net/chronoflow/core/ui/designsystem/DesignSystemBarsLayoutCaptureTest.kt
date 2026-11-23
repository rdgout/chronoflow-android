package net.chronoflow.core.ui.designsystem

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.designsystem.GalleryHost
import net.chronoflow.core.testing.compose.designsystem.GalleryPage
import net.chronoflow.core.testing.compose.designsystem.GallerySection
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class DesignSystemBarsLayoutCaptureTest(
    private val case: LayoutCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun captureDutchTitleTruncation() {
        compose.setContent {
            ProfileHost(case.profile) { GalleryHost(case.language, false) { GalleryPage(GallerySection.Bars) } }
        }
        BoardCapture.capture(compose, "measurements/phase2/Bars_${case.profile.name}_${case.language.tag}_light")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> =
            LayoutMatrix.parameters(
                LayoutMatrix.phone.filter {
                    it.language.tag ==
                        "nl"
                },
            )
    }
}

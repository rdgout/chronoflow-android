package net.chronoflow.core.ui.designsystem

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.designsystem.GalleryHost
import net.chronoflow.core.testing.compose.designsystem.GalleryPage
import net.chronoflow.core.testing.compose.designsystem.GallerySection
import net.chronoflow.core.testing.tiers.Snapshot
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Snapshot
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class DesignSystemSnapshotTest(
    private val name: String,
    private val section: GallerySection,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, dark = dark),
            ).around(compose)

    @Test fun primitivesMatchTheirOptInJvmReferences() {
        compose.setContent {
            ProfileHost(
                DeviceProfiles.pixel8,
            ) { GalleryHost(AppLanguage.En, dark) { GalleryPage(section) } }
        }
        compose.onRoot().captureRoboImage("src/test/snapshots/${name}_pixel8_en_${if (dark) "dark" else "light"}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-dark={2}")
        fun cases(): Collection<Array<Any>> =
            listOf(
                arrayOf("CfButton_styles", GallerySection.Buttons, false),
                arrayOf("CfButton_styles", GallerySection.Buttons, true),
                arrayOf("CfCard_shadow", GallerySection.CardsRows, false),
                arrayOf("CfCard_shadow", GallerySection.CardsRows, true),
                arrayOf("CfRow_states", GallerySection.CardsRows, false),
                arrayOf("CfBanner_default", GallerySection.BannersNotices, false),
                arrayOf("CfTextField_states", GallerySection.Fields, false),
            )
    }
}

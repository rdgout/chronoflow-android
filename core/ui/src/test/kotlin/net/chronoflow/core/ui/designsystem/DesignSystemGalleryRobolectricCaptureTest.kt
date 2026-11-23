package net.chronoflow.core.ui.designsystem

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
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
class DesignSystemGalleryRobolectricCaptureTest(
    private val section: GallerySection,
    private val language: AppLanguage,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, language, dark),
            ).around(compose)

    @Test fun capturePrimitiveSection() {
        compose.setContent {
            ProfileHost(
                DeviceProfiles.pixel8,
            ) { GalleryHost(language, dark) { GalleryPage(section) } }
        }
        BoardCapture.capture(
            compose,
            "designsystem/${section.name}_pixel8_${language.tag}_${if (dark) "dark" else "light"}",
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-dark={2}")
        fun cases(): Collection<Array<Any>> =
            GallerySection.entries.flatMap { section ->
                AppLanguage.entries.flatMap { language ->
                    listOf(false, true).map { dark -> arrayOf<Any>(section, language, dark) }
                }
            }
    }
}

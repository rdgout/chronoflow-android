package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.designsystem.GalleryHost
import net.chronoflow.core.testing.compose.designsystem.GalleryPage
import net.chronoflow.core.testing.compose.designsystem.GallerySection
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Capture
@RunWith(Parameterized::class)
class DesignSystemGalleryCaptureTest(
    private val section: GallerySection,
    private val language: AppLanguage,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(GalleryLocaleRule(language)).around(compose)

    @Test fun capturePrimitiveSection() {
        compose.setContent {
            CompositionLocalProvider(LocalSafeInsets provides WindowInsets.safeDrawing) {
                GalleryHost(language, dark) { GalleryPage(section) }
            }
        }
        BoardCapture.capture(
            compose,
            "designsystem/${section.name}_pixel_${language.tag}_${if (dark) "dark" else "light"}",
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}-dark={2}")
        fun cases(): Collection<Array<Any>> =
            GallerySection.entries.flatMap { section ->
                AppLanguage.entries.flatMap { language ->
                    listOf(false, true).map { dark -> arrayOf<Any>(section, language, dark) }
                }
            }
    }
}

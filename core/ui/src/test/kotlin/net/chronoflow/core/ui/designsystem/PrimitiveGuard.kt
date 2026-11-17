package net.chronoflow.core.ui.designsystem

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.designsystem.GalleryHost
import net.chronoflow.core.testing.compose.designsystem.GalleryPage
import net.chronoflow.core.testing.compose.designsystem.GallerySection
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

abstract class PrimitiveGuard(
    private val case: LayoutCase,
    private val section: GallerySection,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun primitiveFitsTheProfileAndSafeArea() {
        compose.setContent { ProfileHost(case.profile) { GalleryHost(case.language, false) { GalleryPage(section) } } }
        Guards.assertTextFits(compose, case.profile)
        val nativeWindow =
            section in
                setOf(GallerySection.Sheet, GallerySection.Alert, GallerySection.DatePicker, GallerySection.TimePicker)
        // Calendar columns distribute fractional pixels; a native 48 dp target can round one pixel down.
        val minTouch =
            if (section == GallerySection.DatePicker) {
                (PlanUITokens.actionMinHeight - 1f / case.profile.density).dp
            } else {
                PlanUITokens.actionMinHeight.dp
            }
        Guards.assertTouchTargets(compose, case.profile, minSize = minTouch)
        if (!nativeWindow) {
            Guards.assertInteractiveInSafeArea(compose, case.profile)
            // Material segments intentionally share their one-dp border.
            val sharedBorders = if (section == GallerySection.Controls) setOf("gallery.segments") else emptySet()
            Guards.assertNoInteractiveOverlap(compose, exemptTags = sharedBorders)
        }
        if (section == GallerySection.Sheet) {
            val bounds = compose.onNodeWithTag("gallery.sheet").fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.width <= PlanUITokens.walkthroughCompactWidth * case.profile.density + 1)
        }
        // Dialog coordinates are window-local; inset and circular-dial hit testing belongs on the device.
        Guards.assertInk(
            compose,
            case.profile,
            minInkFraction = if (nativeWindow) .001f else .005f,
            minChannelDifference = if (section == GallerySection.Skeleton) 2 else 16,
        )
    }
}

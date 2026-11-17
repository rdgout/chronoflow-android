package net.chronoflow.core.ui.designsystem

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.designsystem.GalleryHost
import net.chronoflow.core.testing.compose.designsystem.GalleryPage
import net.chronoflow.core.testing.compose.designsystem.GallerySection
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
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile)
        Guards.assertNoInteractiveOverlap(compose)
        Guards.assertInk(compose, case.profile)
    }
}

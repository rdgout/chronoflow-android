
package net.chronoflow.feature.trip.page

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.plan.trippage.TripPageLayout
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
class TripPageBoardGuardTest(
    private val case: TripPageBoardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)
    private lateinit var fixture: TripPageBoardFixture

    @After fun close() {
        if (::fixture.isInitialized) fixture.close()
    }

    @Test fun pageFitsTheBoardState() {
        fixture = TripPageBoardFixture(case)
        fixture.start(compose)
        val model = fixture.rig.model
        val day = model.prepared.days[fixture.dayIndex]
        when (case.board) {
            "Plan" -> {
                compose.onNodeWithTag("trip.summary").assertDoesNotExist()
                assertEquals(
                    compose.onNodeWithTag("trip.scroll").getUnclippedBoundsInRoot().top,
                    compose.onNodeWithTag("trip.headerItem.${day.id}").getUnclippedBoundsInRoot().top,
                )
            }

            "PlanScrolled", "PlanDayMenu" -> {
                val children = compose.onNodeWithTag("trip.compactHeader").onChildren()
                val wide = TripPageLayout.resolve(case.profile.widthDp.toDouble()).isWide
                children.assertCountEquals(if (wide) 3 else 1)
                children.filter(hasTestTag("trip.lanes")).assertCountEquals(1)
                if (wide) {
                    children.filter(hasTestTag("trip.compactHeader.day")).assertCountEquals(1)
                    children.filter(hasTestTag("trip.compactHeader.zone")).assertCountEquals(1)
                }
            }

            "PlanTimelineTop" -> {
                compose.onNodeWithTag("trip.summary").assertIsDisplayed()
                compose.onNodeWithTag("trip.days").assertIsDisplayed()
                compose.onNodeWithTag("trip.days.pill.0").assertIsSelected()
            }

            "PlanEnd", "PlanEndNoText" -> {
                val mode = if (TripPageLayout.resolve(case.profile.widthDp.toDouble()).isWide) "timeline" else "list"
                compose.onNodeWithTag("trip.mode.$mode").assertExists()
                compose.onNodeWithTag("trip.plan.end.title").assertIsDisplayed()
                if (model.prepared.returnDayID != null) compose.onNodeWithTag("trip.plan.end.return").assertExists()
            }
        }
        val candidates = model.prepared.days.map { "trip.dayHeader.${it.id}" } + "trip.plan.end.title"
        val headings = candidates.filter { compose.onNodeWithTag(it).isDisplayed() }.toSet()
        Guards.assertAll(
            compose,
            case.profile,
            headings,
            fixture.blockTags,
            rootTag = TripPageBoardFixture.ROOT_TAG,
            scrollIntoView = false,
        )
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile, scrollIntoView = false)
        if (case.board == "PlanDayMenu") {
            fixture.position(compose)
            fixture.openMenu(compose)
            val tags =
                model.prepared.days.indices
                    .map { "trip.dayMenu.day.$it" }
                    .toSet()
            compose
                .onAllNodes(
                    SemanticsMatcher("fixture menu tags") {
                        it.config.getOrNull(SemanticsProperties.TestTag) in
                            tags
                    },
                ).assertCountEquals(model.prepared.days.size)
            compose.onNodeWithTag("trip.dayMenu.day.1").assertIsSelected()
            tags.forEach { tag ->
                val bounds = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
                assertTrue((bounds.bottom - bounds.top) >= TripPageMetrics.menuItemMinHeight)
            }
            val popup = compose.onNode(isPopup()).fetchSemanticsNode()
            assertTrue(popup.layoutInfo.width <= case.profile.widthDp * case.profile.density + 1)
            assertTrue(popup.layoutInfo.height <= case.profile.heightDp * case.profile.density + 1)
            assertTrue(fixture.bitmap(compose).width > 0)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = TripPageBoardFixture.guardCases().map { arrayOf(it) }
    }
}

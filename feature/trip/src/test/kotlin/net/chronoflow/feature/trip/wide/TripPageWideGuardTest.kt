package net.chronoflow.feature.trip.wide

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class TripPageWideGuardTest(
    private val case: TripPageWideCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)
    private var fixture: TripPageWideFixture? = null

    @After fun close() {
        fixture?.close()
    }

    @Test fun widePageFitsAndKeepsItsWindowPolicy() {
        val host = TripPageWideFixture(case).also { fixture = it }
        host.start(compose)
        val ready = host.rig.vm.ui.value.state == net.chronoflow.core.plan.trippage.TripPageState.Ready
        val heading =
            when (case.board) {
                "step" -> "step.title"
                "legend" -> "legend.title"
                "PlanNoPlan" -> "trip.noPlan.title"
                "PlanRemoteWideOffline" -> "trip.remote.offline.title"
                "ErrorNotFound", "ErrorServer" -> "trip.error.title"
                else -> if (ready) "trip.dayHeader.${host.day.id}" else null
            }
        val headings = heading?.takeIf { compose.onNodeWithTag(it).isDisplayed() }?.let { setOf(it) }.orEmpty()
        val exemptions = if (ready) host.blockTags else setOf("trip.skeleton", "trip.wide.loading")
        Guards.assertAll(
            compose,
            case.profile,
            headings,
            exemptions,
            rootTag = TripPageWideFixture.ROOT,
            scrollIntoView = false,
            interactiveRootTag =
                if (case.board ==
                    "step"
                ) {
                    "step.sheet"
                } else if (case.board == "legend") {
                    "legend.priorities"
                } else {
                    null
                },
            inkChannelDifference = if (case.board == "PlanLoading") 1 else 16,
        )
        if (ready && case.board != "step" && case.board != "legend") {
            val day = if (case.position == "top") host.rig.model.prepared.days[0] else host.day
            val node = compose.onNodeWithTag("trip.day.${day.id}").fetchSemanticsNode()
            assertNull(node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange))
            val list = compose.onNodeWithTag("trip.wide.list.${day.id}").fetchSemanticsNode().boundsInRoot
            val page = compose.onNodeWithTag("trip.scroll").fetchSemanticsNode().boundsInRoot
            val expectedRight =
                page.right - with(compose.density) { (case.profile.insets.right.dp + 20.dp).roundToPx() }
            assertTrue("list respects the right inset and gutter", list.right <= expectedRight + 1f)
        }
        if (case.board == "step") {
            val sheet = compose.onNodeWithTag("step.sheet").fetchSemanticsNode().boundsInRoot
            val width = with(compose.density) { case.profile.widthDp.toFloat() * density }
            assertTrue(sheet.width <= 640f * compose.density.density + 1f)
            assertEquals(width / 2, sheet.center.x, 1f)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = TripPageWideCase.guards().map { arrayOf(it) }
    }
}

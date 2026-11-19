package net.chronoflow.feature.trip.state

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.After
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
class TripStatesGuardsTest(
    private val case: TripStatesBoardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)
    private lateinit var fixture: TripStatesFixture

    @After fun close() {
        if (::fixture.isInitialized) fixture.close()
    }

    @Test fun stateFits() {
        fixture = case.fixture()
        fixture.start(compose)
        val title =
            when (case.board) {
                "PlanNoPlan" -> "trip.noPlan.title"
                "PlanRemoteOffline", "PlanRemoteOfflineNL" -> "trip.remote.offline.title"
                "ErrorNotFound", "ErrorServer" -> "trip.error.title"
                "PlanList", "PlanListScrolled" -> "trip.dayHeader.${fixture.rig.model.prepared.days[0].id}"
                else -> null
            }
        val headings = title?.takeIf { compose.onNodeWithTag(it).isDisplayed() }?.let { setOf(it) }.orEmpty()
        Guards.assertAll(
            compose,
            case.profile,
            headings,
            setOf("trip.skeleton"),
            rootTag = TripStatesFixture.ROOT_TAG,
            scrollIntoView = false,
            inkChannelDifference = if (case.board == "PlanLoading") 1 else 16,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = TripStatesBoardCase.guardCases().map { arrayOf(it) }
    }
}

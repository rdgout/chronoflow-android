package net.chronoflow.feature.trip.wide

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.SafeInsetsDp
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.WindowSizeFake
import net.chronoflow.core.testing.compose.WindowSizeFakeHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.state.TripStatesFixture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class WideRemoteStatesTest(
    private val language: AppLanguage,
) {
    private val compose = createComposeRule()
    private val profile =
        DeviceProfiles.foldInner.copy(
            widthDp = 669,
            heightDp = 951,
            insets = SafeInsetsDp(0f, 0f, 0f, 0f),
        )

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language)).around(compose)
    private var fixture: TripStatesFixture? = null

    private fun show(
        board: String,
        width: Int = 669,
    ) {
        val host = TripStatesFixture(board, profile, language).also { fixture = it }
        val window = WindowSizeFake(DpSize(width.dp, 951.dp))
        compose.setContent { WindowSizeFakeHost(window) { ChronoFlowTheme { host.rig.Content() } } }
        compose.waitForIdle()
    }

    @After fun close() {
        fixture?.close()
    }

    @Test fun remoteLoadingUsesTheWideSkeleton() {
        show("PlanRemote")
        val list = compose.onNodeWithTag("trip.wide.loading.list", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val timeline =
            compose
                .onNodeWithTag(
                    "trip.wide.loading.timeline",
                    useUnmergedTree = true,
                ).getUnclippedBoundsInRoot()
        val tolerance = 0.5f / compose.density.density
        assertEquals(280f, list.width.value, tolerance)
        assertEquals(16f, list.left.value - timeline.right.value, tolerance)
        assertEquals(360f, timeline.height.value, tolerance)
        compose
            .onNodeWithTag(
                "trip.wide.loading",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
    }

    @Test fun remoteOfflineCardSpansTheGutter() {
        show("PlanRemoteOffline")
        val card = compose.onNodeWithTag("trip.remote.offline").fetchSemanticsNode().boundsInRoot
        val page = compose.onNodeWithTag("trip.page").fetchSemanticsNode().boundsInRoot
        val gutter = with(compose.density) { 20.dp.roundToPx().toFloat() }
        assertEquals(page.left + gutter, card.left, 0.5f)
        assertEquals(page.right - gutter, card.right, 0.5f)
        compose.onNodeWithTag("trip.wide.loading").assertDoesNotExist()
    }

    @Test fun phoneRemoteLoadingKeepsThePhoneSkeleton() {
        show("PlanRemote", 411)
        compose.onNodeWithTag("trip.wide.loading").assertDoesNotExist()
        compose.onNodeWithTag("trip.skeleton").assertExists()
    }

    @Test fun localLoadingKeepsThePhoneSkeletonOnWide() {
        show("PlanLoading")
        compose.onNodeWithTag("trip.wide.loading").assertDoesNotExist()
        compose.onNodeWithTag("trip.skeleton").assertExists()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "language={0}")
        fun languages(): List<Array<Any>> = listOf(arrayOf(AppLanguage.En), arrayOf(AppLanguage.Nl))
    }
}

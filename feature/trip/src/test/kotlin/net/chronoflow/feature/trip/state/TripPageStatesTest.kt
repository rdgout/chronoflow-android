package net.chronoflow.feature.trip.state

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.TripPageEvent
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
class TripPageStatesTest(
    private val scenario: String,
    private val board: String,
) {
    private val compose = createComposeRule()
    private val language = if (scenario == "remoteOfflineDutchCopy") AppLanguage.Nl else AppLanguage.En

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8, language)).around(compose)
    private var fixture: TripStatesFixture? = null

    @After fun close() {
        fixture?.close()
    }

    @Test fun stateStructure() {
        if (scenario == "loadingBarHasDisabledActions") {
            compose.setContent { ProfileHost(DeviceProfiles.pixel8) { ChronoFlowTheme { TripPageLoadingBar({}) } } }
            compose.onNodeWithTag("trip.loadingTitle").assertExists()
            compose.onNodeWithTag("trip.actions.open").assertIsNotEnabled()
            compose.onNodeWithTag("trip.skeleton").assertDoesNotExist()
            return
        }
        val host = TripStatesFixture(board, language = language).also { fixture = it }
        host.start(compose)
        when (scenario) {
            "loadingShowsSkeletonWithoutDownloadingLine" -> {
                compose
                    .onNodeWithTag(
                        "trip.skeleton",
                    ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
                compose.onNodeWithTag("trip.skeleton.downloading", useUnmergedTree = true).assertDoesNotExist()
                compose.onNodeWithTag("trip.title").assertExists()
            }

            "remoteLoadingShowsSummaryAndDownloadingLine" -> {
                compose.onNodeWithTag("trip.remote.summary").assertExists()
                compose.onNodeWithTag("trip.skeleton.downloading", useUnmergedTree = true).assertExists()
                compose.onNodeWithTag("trip.viewToggle").assertIsNotEnabled()
            }

            "remoteOfflineRetryFetchesAgain" -> {
                compose.onNodeWithTag("trip.remote.retry").performClick()
                assertEquals(1, host.events.count { it == TripPageEvent.Retry })
                compose.onNodeWithTag("trip.remote.offline").assertExists()
            }

            "noPlanCardButtonDisabled" -> {
                compose.onNodeWithTag("trip.noPlan.makePlan").assertIsNotEnabled()
                compose
                    .onNodeWithTag(
                        "trip.noPlan.title",
                    ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            }

            "notFoundButtonsNavigate" -> {
                compose.onNodeWithTag("trip.error.primary").performClick()
                compose.onNodeWithTag("trip.error.secondary").performClick()
                assertEquals(1, host.events.count { it == TripPageEvent.Trips })
                assertEquals(1, host.events.count { it == TripPageEvent.Today })
                compose.onNodeWithTag("trip.viewToggle").assertDoesNotExist()
                compose.onNodeWithTag("trip.actions.open").assertDoesNotExist()
            }

            "serverTryAgainRetries" -> {
                host.failure = null
                compose.onNodeWithTag("trip.error.primary").performClick()
                compose.onNodeWithTag("trip.skeleton").assertExists()
                assertEquals(1, host.events.count { it == TripPageEvent.Retry })
            }

            "tombstonedTripIsNotFound", "missingAfterSyncIsNotFound" -> {
                compose.onNodeWithTag("trip.error.notFound").assertExists()
            }

            "metadataLineOnlyWhenNotReady_remote" -> {
                compose.onNodeWithTag("trip.meta").assertExists()
            }

            "metadataLineOnlyWhenNotReady_ready" -> {
                compose.onNodeWithTag("trip.meta").assertDoesNotExist()
            }

            "pullToRefreshOnStates_offline", "pullToRefreshOnStates_notFound" -> {
                compose.onNodeWithTag("trip.scroll").performTouchInput { swipeDown() }
                compose.waitForIdle()
                assertEquals(1, host.events.count { it == TripPageEvent.Refresh })
            }

            "remoteOfflineDutchCopy" -> {
                compose
                    .onNodeWithTag(
                        "trip.remote.offline.title",
                    ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
                compose.onNodeWithTag("trip.remote.retry").assertHasClickAction()
            }

            else -> {
                error("unknown state case")
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<String>> =
            listOf(
                arrayOf("loadingShowsSkeletonWithoutDownloadingLine", "PlanLoading"),
                arrayOf("remoteLoadingShowsSummaryAndDownloadingLine", "PlanRemote"),
                arrayOf("remoteOfflineRetryFetchesAgain", "PlanRemoteOffline"),
                arrayOf("noPlanCardButtonDisabled", "PlanNoPlan"),
                arrayOf("notFoundButtonsNavigate", "ErrorNotFound"),
                arrayOf("serverTryAgainRetries", "ErrorServer"),
                arrayOf("tombstonedTripIsNotFound", "Tombstone"),
                arrayOf("missingAfterSyncIsNotFound", "Missing"),
                arrayOf("metadataLineOnlyWhenNotReady_remote", "PlanRemoteOffline"),
                arrayOf("metadataLineOnlyWhenNotReady_ready", "Ready"),
                arrayOf("pullToRefreshOnStates_offline", "PlanRemoteOffline"),
                arrayOf("pullToRefreshOnStates_notFound", "ErrorNotFound"),
                arrayOf("loadingBarHasDisabledActions", "PlanLoading"),
                arrayOf("remoteOfflineDutchCopy", "PlanRemoteOfflineNL"),
            )
    }
}

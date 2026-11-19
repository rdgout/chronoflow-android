package net.chronoflow.app.perf

import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppGraph
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.FixedPlanClock
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.feature.trip.page.DefaultTripPlanPipeline
import net.chronoflow.feature.trip.page.TripPageArgs
import net.chronoflow.feature.trip.page.TripPageViewModel
import net.chronoflow.feature.trip.timeline.TextPaintMetrics
import net.chronoflow.feature.trip.timeline.TimelineTypefaces
import net.chronoflow.feature.trip.page.TripPage as RealTripPage

/** `cf.trip-page=S1…S6|large30|large60` opens the timeline page from a fixture, in debug and the perf variants. */
object TripPageEntry {
    private val names = setOf("S1", "S2", "S3", "S4", "S5", "S6", "large30", "large60")

    fun content(
        intent: Intent,
        graph: AppGraph,
    ): (@Composable () -> Unit)? {
        val name = intent.getStringExtra(EXTRA_PAGE) ?: return null
        if (name !in names) {
            Log.w(LOG_TAG, "unknown $EXTRA_PAGE value: $name")
            return null
        }
        val clock: PlanClock =
            perfClockInstant(intent.getStringExtra(EXTRA_FREEZE))?.let(::FixedPlanClock) ?: graph.planClock
        return { TripPage(name, clock) }
    }

    private const val EXTRA_PAGE = "cf.trip-page"
    private const val EXTRA_FREEZE = "cf.freezeAt"
    private const val LOG_TAG = "CfTimeline"
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
private fun TripPage(
    name: String,
    clock: PlanClock,
) {
    // The fixture is generated before the ViewModel exists: its init begins the first-frame section, which must not include generation.
    val bytes by produceState<ByteArray?>(null, name) {
        value = withContext(Dispatchers.Default) { LargeTimelineFixtures.detailBytes(name) }
    }
    val fixture = bytes ?: return
    val language = if (LocalConfiguration.current.locales[0].language == "nl") Language.Nl else Language.En
    val appContext = LocalContext.current.applicationContext
    // A locale change recreates the activity but keeps the ViewModel store, and the language is a constructor argument.
    val viewModel: TripPageViewModel =
        viewModel(
            key = "cf.trip-page:$name:${language.raw}",
            factory =
                viewModelFactory {
                    initializer {
                        val catalogs = CopyCatalogProvider(Dispatchers.IO)
                        val decoder = PlanDecoder(Dispatchers.Default)
                        val preparer = PlanPreparer(decoder, catalogs, Dispatchers.Default)
                        TripPageViewModel(
                            args = TripPageArgs("fixture-$name"),
                            source = FixtureTripPageSource(name, fixture),
                            pipeline = DefaultTripPlanPipeline(decoder, preparer),
                            savedState = SavedStateHandle(),
                            clock = clock,
                            language = language,
                            compute = Dispatchers.Default,
                            buildDispatcher = Dispatchers.Default.limitedParallelism(2),
                            metrics = { TextPaintMetrics(TimelineTypefaces.load(appContext)) },
                            formatter = { CopyLineFormatter(catalogs.catalog()) },
                        )
                    }
                },
        )
    RealTripPage(viewModel, onBack = {}, onSettings = null)
}

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.trip

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.AppGraphKey
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.app.SoftwareSessionCipher
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.ScreenSessionFixture
import net.chronoflow.debug.TestSeed
import net.chronoflow.debug.TestSeeds
import net.chronoflow.feature.trip.page.TripPageArgs
import net.chronoflow.feature.trip.page.TripPageViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
class TripPageViewModelFactoryTest {
    @Test fun theEntryScopedFactoryHoldsAndReleasesItsLease() =
        runTest(timeout = 60.seconds) {
            val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val seed =
                TestSeed.parse(
                    mapOf(
                        TestSeed.TEST_ID_KEY to UUID.randomUUID().toString(),
                        TestSeed.PROFILE_KEY to "screen",
                        "CF_AUTH_TEST_SIGNED_IN" to "demo",
                        "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                        "CF_MOCK_DELAY" to "0",
                    ),
                )
            TestSeeds.apply(
                app,
                seed,
                Dispatchers.IO,
                sessionCipher = SoftwareSessionCipher(),
                dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher),
            )
            val graph = app.graph
            val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            try {
                ScreenSessionFixture.route(graph)
                val session =
                    graph.sessionGraphs.current
                        .filterNotNull()
                        .first()
                val trip = session.trips.trips().first()
                val owner = activity.get()
                val extras =
                    MutableCreationExtras(
                        owner.defaultViewModelCreationExtras,
                    ).apply { set(AppGraphKey, graph) }
                val factory = tripPageViewModelFactory(TripPageArgs(trip.id), app, Language.En, true)
                ViewModelProvider(owner.viewModelStore, factory, extras)[TripPageViewModel::class.java]
                assertEquals(setOf(trip.id), session.sync.retention.withHeldPlans { it })
                owner.viewModelStore.clear()
                assertEquals(emptySet<String>(), session.sync.retention.withHeldPlans { it })
            } finally {
                activity.pause().stop().destroy()
                withContext(NonCancellable) { graph.close() }
                Dispatchers.resetMain()
            }
        }
}

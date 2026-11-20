@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.onboarding.walkthrough

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.feature.onboarding.OnboardingSessions
import net.chronoflow.feature.onboarding.support.FakeOnboardingSessions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class WalkthroughViewModelTests {
    @Test fun counterAndButtonFollowThePage() =
        runTest(timeout = 60.seconds) {
            val model = WalkthroughViewModel(false, FakeOnboardingSessions(), StandardTestDispatcher(testScheduler))
            val store = ViewModelStore().apply { put("walkthrough", model) }
            try {
                runCurrent()
                assertEquals(WalkthroughModel.Page.Timeline, model.state.value?.page)
                model.onEvent(WalkthroughEvent.Advance)
                runCurrent()
                assertEquals(WalkthroughModel.Page.Categories, model.state.value?.page)
                model.onEvent(WalkthroughEvent.Advance)
                runCurrent()
                assertEquals(WalkthroughModel.Page.Progress, model.state.value?.page)
            } finally {
                store.clear()
                model.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }

    @Test fun busyDisablesSkipAndDoneAndCompletesOnce() =
        runTest(timeout = 60.seconds) {
            val saved = CompletableDeferred<Unit>()
            var writes = 0
            val model =
                WalkthroughViewModel(
                    false,
                    FakeOnboardingSessions(enqueue = {
                        writes++
                        saved.await()
                    }),
                    StandardTestDispatcher(testScheduler),
                )
            val store = ViewModelStore().apply { put("walkthrough", model) }
            try {
                runCurrent()
                model.onEvent(WalkthroughEvent.Skip)
                runCurrent()
                assertTrue(model.state.value!!.busy)
                model.onEvent(WalkthroughEvent.Skip)
                model.onEvent(WalkthroughEvent.Advance)
                runCurrent()
                assertEquals(1, writes)
                saved.complete(Unit)
                runCurrent()
                assertFalse(model.state.value!!.busy)
                assertTrue(model.state.value!!.completed)
                assertFalse(model.takeFinish())
            } finally {
                store.clear()
                model.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }

    @Test fun loadingWaitsForASession() =
        runTest(timeout = 60.seconds) {
            val model =
                WalkthroughViewModel(
                    false,
                    OnboardingSessions { flow { awaitCancellation() } },
                    StandardTestDispatcher(testScheduler),
                )
            val store = ViewModelStore().apply { put("walkthrough", model) }
            try {
                runCurrent()
                assertNull(model.state.value)
            } finally {
                store.clear()
                model.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }
}

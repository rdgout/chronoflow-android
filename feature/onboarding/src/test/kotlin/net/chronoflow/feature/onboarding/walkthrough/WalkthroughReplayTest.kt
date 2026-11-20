@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.onboarding.walkthrough

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.feature.onboarding.support.FakeOnboardingSessions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class WalkthroughReplayTest(
    private val exit: String,
) {
    @Test fun returnsExactlyOnceWithoutWriting() =
        runTest(timeout = 60.seconds) {
            val model =
                WalkthroughViewModel(
                    true,
                    FakeOnboardingSessions(enqueue = {
                        error("replay wrote")
                    }),
                    StandardTestDispatcher(testScheduler),
                )
            val store = ViewModelStore().apply { put("walkthrough", model) }
            try {
                runCurrent()
                if (exit == "done") {
                    model.onEvent(WalkthroughEvent.Advance)
                    runCurrent()
                    model.onEvent(WalkthroughEvent.Advance)
                    runCurrent()
                    model.onEvent(WalkthroughEvent.Advance)
                } else {
                    model.onEvent(WalkthroughEvent.Skip)
                }
                runCurrent()
                var returns = 0
                if (model.takeFinish()) returns++
                model.onEvent(WalkthroughEvent.Skip)
                runCurrent()
                if (model.takeFinish()) returns++
                assertEquals(1, returns)
                assertFalse(model.state.value!!.busy)
            } finally {
                store.clear()
                model.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun exits() = listOf("skip", "done", "back").map { arrayOf(it) }
    }
}

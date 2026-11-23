@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class SessionActivationTest {
    @Test fun foregroundRunsTheIosStepsAfterEvictingIdleConnections() =
        runTest(timeout = 60.seconds) {
            val events = mutableListOf<String>()
            val snapshot = SessionSnapshot(StoredSession("test", syncAccount), 1)
            val activation =
                SessionActivation(
                    backgroundScope,
                    { events += "evict" },
                    { events += "enabled:$it" },
                    { events += "foreground" },
                    { events += "unauthorized" },
                    { events += "retry" },
                    {
                        events += "current"
                        snapshot
                    },
                    {
                        assertEquals(snapshot, it)
                        events += "resume"
                    },
                )
            activation.setActive(true)
            assertEquals(
                listOf("evict", "enabled:true", "foreground", "unauthorized", "retry", "current", "resume"),
                events,
            )
        }

    @Test fun aFailedUnauthorizedRetryStopsBeforeReadinessAndResume() =
        runTest(timeout = 60.seconds) {
            val events = mutableListOf<String>()
            val activation =
                SessionActivation(
                    backgroundScope,
                    { events += "evict" },
                    { events += "enabled:$it" },
                    { events += "foreground" },
                    {
                        events += "unauthorized"
                        throw IOException()
                    },
                    { events += "retry" },
                    { error("unexpected active session read") },
                    { error("unexpected resume") },
                )
            activation.setActive(true)
            assertEquals(listOf("evict", "enabled:true", "foreground", "unauthorized"), events)
        }

    @Test fun backgroundOnlyDisablesSync() =
        runTest(timeout = 60.seconds) {
            val events = mutableListOf<String>()
            val activation =
                SessionActivation(
                    backgroundScope,
                    { error("eviction") },
                    { events += "enabled:$it" },
                    { error("foreground") },
                    { error("unauthorized") },
                    { error("retry") },
                    { error("current") },
                    { error("source") },
                )
            activation.setActive(false)
            assertEquals(listOf("enabled:false"), events)
        }

    @Test fun backgroundCancelsAFlightAndForegroundReenablesAndResumes() =
        runTest(timeout = 60.seconds) {
            val f = AppSessionFixture(StandardTestDispatcher(testScheduler), backgroundScope)
            val entered = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()
            var cancelled = false
            try {
                f.controller.accept(sessionResult())
                f.source.configure(hook = {
                    if (it ==
                        "identity"
                    ) {
                        entered.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled = true
                        }
                    }
                })
                // requestStart attaches asynchronously; a refresh before attachment returns null.
                assertTrue(eventually { f.clock.pendingCount == 1 })
                val refresh =
                    backgroundScope.async {
                        try {
                            f.sync.refreshNow()
                        } catch (_: kotlinx.coroutines.CancellationException) {
                        }
                    }
                entered.await()
                val activation =
                    SessionActivation(
                        backgroundScope,
                        { events += "evict" },
                        {
                            events += "enabled:$it"
                            f.sync.setEnabled(it)
                        },
                        { events += "foreground" },
                        {
                            events += "unauthorized"
                            f.controller.retryUnauthorized()
                        },
                        { events += "retry" },
                        {
                            events += "current"
                            f.controller.activeSession()
                        },
                        {
                            events += "resume"
                            f.sync.resume(it.session, it.epoch)
                        },
                    )
                val calls = f.source.calls.toList()
                activation.setActive(false)
                refresh.join()
                assertTrue(cancelled)
                assertEquals(calls, f.source.calls)
                assertEquals(listOf("enabled:false"), events)
                events.clear()
                f.source.configure()
                activation.setActive(true)
                assertEquals(
                    listOf("evict", "enabled:true", "foreground", "unauthorized", "retry", "current", "resume"),
                    events,
                )
                assertTrue(eventually { f.clock.pendingCount == 1 })
                f.clock.advance()
                assertTrue(eventually { f.source.calls.count { it == "identity" } == 2 })
            } finally {
                f.close()
            }
        }

    @Test fun foregroundAndBackgroundAreSerialized() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()
            val activation =
                SessionActivation(
                    backgroundScope,
                    {
                        entered.complete(Unit)
                        gate.await()
                        events += "evict"
                    },
                    { events += "enabled:$it" },
                    {},
                    {},
                    {},
                    { null },
                    {},
                )
            val start = backgroundScope.async { activation.setActive(true) }
            entered.await()
            val stop = backgroundScope.async { activation.setActive(false) }
            runCurrent()
            assertTrue(events.isEmpty())
            gate.complete(Unit)
            start.await()
            stop.await()
            assertEquals(listOf("evict", "enabled:true", "enabled:false"), events)
        }

    @Test fun prepareBackgroundDisablesSyncOnlyBeforeTheFirstStart() =
        runTest(timeout = 60.seconds) {
            val enabled = mutableListOf<Boolean>()
            val activation = SessionActivation(backgroundScope, {}, { enabled += it }, {}, {}, {}, { null }, {})
            activation.prepareBackground()
            assertEquals(listOf(false), enabled)
            activation.setActive(true)
            assertEquals(listOf(false, true), enabled)
            activation.prepareBackground()
            activation.setActive(false)
            activation.prepareBackground()
            assertEquals(listOf(false, true, false), enabled)
        }

    @Test fun foregroundReschedulesAfterTheGuestHook() =
        runTest(timeout = 60.seconds) {
            val events = mutableListOf<String>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val root =
                net.chronoflow.core.database.testing.TestRoots
                    .temporaryStoreRoot()
            val guest =
                net.chronoflow.core.wizard.model.GuestWizardStore(
                    {
                        events += "guest"
                        java.io.File(root, "guest.json")
                    },
                    dispatcher,
                    { net.chronoflow.core.testing.reminders.reminderNow },
                )
            val reminders =
                net.chronoflow.app.reminders.ReminderSessionHooks(
                    { events += "reschedule" },
                    { events += "detach" },
                )
            try {
                val hooks =
                    net.chronoflow.app.guest
                        .GuestSessionHooks(guest, reminders)
                val activation = SessionActivation(backgroundScope, {}, {}, hooks::onForeground, {}, {}, { null }, {})
                activation.setActive(true)
                assertEquals(listOf("guest", "reschedule"), events)
                hooks.onSessionEnding()
                assertEquals(listOf("guest", "reschedule", "detach"), events)
            } finally {
                root.deleteRecursively()
            }
        }
}

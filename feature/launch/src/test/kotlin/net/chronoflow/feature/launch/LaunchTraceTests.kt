package net.chronoflow.feature.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchTraceTests {
    @Test fun skippedOverlayClosesBothTracesAtFirstFrameCommit() {
        val tracer = FakeTracer()
        val controller = controller(tracer)
        tracer.beginBoth()
        assertFalse(controller.claimOverlay(LaunchOverrides(skipOverlay = true)))
        assertEquals(2, tracer.events.size)

        controller.onFrameCommitted(false)
        controller.onFrameCommitted(false)
        controller.onActivityDestroyed()
        tracer.beginBoth()

        tracer.assertPairedOnce()
    }

    @Test fun recreatedActivityClosesBothTracesAtItsFirstCommit() {
        val tracer = FakeTracer()
        val controller = controller(tracer)
        tracer.beginBoth()
        assertTrue(controller.claimOverlay(LaunchOverrides.None))
        controller.onActivityCreated(restored = true)
        assertFalse(controller.claimOverlay(LaunchOverrides.None))

        controller.onFrameCommitted(false)
        controller.onOpeningSettled()
        controller.onActivityDestroyed()

        tracer.assertPairedOnce()
    }

    @Test fun abandonedOpeningClosesBothTracesBeforeAnyDraw() {
        val tracer = FakeTracer()
        val controller = controller(tracer)
        tracer.beginBoth()
        assertTrue(controller.claimOverlay(LaunchOverrides.None))
        controller.onFrameCommitted(true)
        assertEquals(2, tracer.events.size)

        controller.onActivityDestroyed()
        controller.onActivityDestroyed()
        controller.onOpeningSettled()

        tracer.assertPairedOnce()
    }

    @Test fun abandonedOpeningAfterFirstDrawClosesLaunchWithoutAnotherCommitEnd() {
        val tracer = FakeTracer()
        val controller = controller(tracer)
        tracer.beginBoth()
        assertTrue(controller.claimOverlay(LaunchOverrides.None))
        tracer.session.endFirstCommit()

        controller.onActivityDestroyed()
        controller.onOpeningSettled()

        tracer.assertPairedOnce()
    }

    @Test fun settledOpeningDoesNotEndAgainOnDestructionOrAnotherActivity() {
        val tracer = FakeTracer()
        val controller = controller(tracer)
        tracer.beginBoth()
        assertTrue(controller.claimOverlay(LaunchOverrides.None))
        tracer.session.endFirstCommit()

        controller.onOpeningSettled()
        controller.onOpeningSettled()
        controller.onActivityDestroyed()
        controller.onFrameCommitted(false)
        tracer.beginBoth()

        tracer.assertPairedOnce()
    }

    @Test fun skippedActivityDestroyedBeforeFirstCommitStillClosesBothTraces() {
        val tracer = FakeTracer()
        val controller = controller(tracer)
        tracer.beginBoth()
        assertFalse(controller.claimOverlay(LaunchOverrides(skipOverlay = true)))

        controller.onActivityDestroyed()
        controller.onFrameCommitted(false)

        tracer.assertPairedOnce()
    }

    private fun controller(tracer: FakeTracer): LaunchController =
        LaunchController(moon = { error("trace lifecycle does not decode a moon") }, tracing = tracer.session)

    private class FakeTracer {
        val events = mutableListOf<String>()
        val session =
            LaunchTraceSession(
                begin = { name, cookie -> events += "begin:$name:$cookie" },
                end = { name, cookie -> events += "end:$name:$cookie" },
            )

        fun beginBoth() {
            session.beginLaunch()
            session.beginFirstCommit()
        }

        fun assertPairedOnce() {
            assertEquals(
                listOf("begin:launch:1", "begin:first-commit:2", "end:first-commit:2", "end:launch:1"),
                events,
            )
        }
    }
}

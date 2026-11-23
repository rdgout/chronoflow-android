package net.chronoflow.feature.trip.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class DrawingFailureTest {
    @Test fun cancellationIsRethrownAndNotReported() {
        val reported = mutableListOf<Exception>()
        val cancellation = CancellationException("cancelled")
        try {
            recoverDrawingFailure({ reported += it }) { throw cancellation }
            fail("expected the cancellation")
        } catch (e: CancellationException) {
            assertSame(cancellation, e)
        }
        assertTrue(reported.isEmpty())
    }

    @Test fun listedExceptionsAreReported() {
        val failures =
            listOf(
                IllegalStateException("a"),
                IllegalArgumentException("b"),
                IndexOutOfBoundsException("c"),
                NoSuchElementException("d"),
            )
        val reported = mutableListOf<Exception>()
        for (failure in failures) recoverDrawingFailure({ reported += it }) { throw failure }
        assertEquals(failures, reported)
    }

    @Test fun otherExceptionsPropagate() {
        try {
            recoverDrawingFailure({}) { throw UnsupportedOperationException() }
            fail("expected the exception")
        } catch (e: UnsupportedOperationException) {
            // expected
        }
    }
}

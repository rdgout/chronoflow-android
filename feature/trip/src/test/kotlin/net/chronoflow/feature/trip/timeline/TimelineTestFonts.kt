package net.chronoflow.feature.trip.timeline

import android.content.Context
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds

/** Loads the Figtree faces once per test JVM, off the test body, with the hang cap of AGENTS.md "Tests". */
object TimelineTestFonts {
    @Volatile
    private var loaded: TimelineTypefaces? = null

    fun typefaces(context: Context): TimelineTypefaces {
        loaded?.let { return it }
        var result: TimelineTypefaces? = null
        runTest(timeout = 60.seconds) { result = TimelineTypefaces.load(context) }
        return checkNotNull(result).also { loaded = it }
    }
}

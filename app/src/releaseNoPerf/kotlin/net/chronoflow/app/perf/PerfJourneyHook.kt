package net.chronoflow.app.perf

import android.content.Intent
import net.chronoflow.app.AppGraph
import java.util.Locale

object PerfJourneyHook {
    @Suppress("UNUSED_PARAMETER")
    fun onIntent(intent: Intent?) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun prepareWizard(
        graph: AppGraph,
        epoch: Long,
        locale: Locale,
    ): Boolean = false
}

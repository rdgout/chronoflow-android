package net.chronoflow.app.perf

import android.content.Intent
import android.util.Log
import net.chronoflow.app.AppGraph
import net.chronoflow.app.wizard.WizardStartOverride
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.ui.WizardInitialPresentation
import java.util.Locale

object PerfJourneyHook {
    private var opened = false

    fun onIntent(intent: Intent?) {
        val extras =
            PerfJourneyExtras.parse(
                PerfJourneyExtras.keys
                    .mapNotNull { key ->
                        intent?.getStringExtra(key)?.let {
                            key to
                                it
                        }
                    }.toMap(),
            )
        PerfJourneyState.current = extras
        extras.ignored.forEach { Log.w("CfPerf", "ignored $it") }
    }

    @Suppress("UNUSED_PARAMETER")
    fun prepareWizard(
        graph: AppGraph,
        epoch: Long,
        locale: Locale,
    ): Boolean {
        if (opened || PerfJourneyState.current.wizard != PerfWizard.WizardSaveJourney) return false
        opened = true
        graph.wizardStartOverride =
            object : WizardStartOverride {
                override val presentation = WizardInitialPresentation()

                override fun dependencies(session: TripWizardDependencies): TripWizardDependencies =
                    object : TripWizardDependencies by session {
                        override val uiOverride: net.chronoflow.feature.wizard.shell.WizardUiOverride? = null

                        override suspend fun start() = PerfWizardJourney.apply(session.start(), locale, session.now())
                    }
            }
        return true
    }
}

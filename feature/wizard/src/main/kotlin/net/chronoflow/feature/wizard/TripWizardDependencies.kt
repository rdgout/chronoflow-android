package net.chronoflow.feature.wizard

import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineScope
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardCheckingClock
import net.chronoflow.core.wizard.model.WizardDraftStore
import java.time.Instant

interface TripWizardDependencies {
    val store: WizardDraftStore
    val airportSearch: AirportSearchService
    val source: DataSource
    val now: () -> Instant
    val writerScope: CoroutineScope
    val checkingClock: WizardCheckingClock get() = WizardCheckingClock.continuous

    suspend fun start(): MemberWizardDraft

    suspend fun startTrip(
        id: String,
        action: TripWizardAction,
    ): MemberWizardDraft

    fun newGeneration(): TripGenerationService
}

object TripWizardDependenciesKey : CreationExtras.Key<TripWizardDependencies>

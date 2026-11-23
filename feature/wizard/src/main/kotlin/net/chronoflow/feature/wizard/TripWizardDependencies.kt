package net.chronoflow.feature.wizard

import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.wizard.generation.MemberWizardGeneration
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardCheckingClock
import net.chronoflow.core.wizard.model.WizardDraftStore
import java.time.Instant

interface TripWizardDependencies {
    val guest: Boolean get() = false
    val signUpLauncher: net.chronoflow.feature.wizard.guest.GuestSignUpLauncher? get() = null
    val store: WizardDraftStore
    val airportSearch: AirportSearchService
    val source: DataSource
    val now: () -> Instant
    val writerScope: CoroutineScope
    val timeFormat: Flow<TimeFormat> get() = flowOf(TimeFormat.Value24)
    val uiOverride: net.chronoflow.feature.wizard.shell.WizardUiOverride? get() = null
    val checkingClock: WizardCheckingClock get() = WizardCheckingClock.continuous

    suspend fun copyFormatter(): net.chronoflow.core.plan.CopyLineFormatter? = null

    suspend fun recover(): MemberWizardDraft? = null

    suspend fun start(): MemberWizardDraft

    suspend fun startTrip(
        id: String,
        action: TripWizardAction,
    ): MemberWizardDraft

    fun newGeneration(): MemberWizardGeneration
}

object TripWizardDependenciesKey : CreationExtras.Key<TripWizardDependencies>

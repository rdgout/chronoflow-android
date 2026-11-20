@file:Suppress("ktlint:standard:max-line-length")
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.AirportSearchState
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardDraftStore
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.from
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.core.wizard.model.reviewInput
import net.chronoflow.core.wizard.model.setRouteAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds
import net.chronoflow.core.navigation.WizardMode as WizardEntry

internal class WizardMainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(StandardTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

internal class FakeWizardDependencies(
    scope: TestScope,
) : TripWizardDependencies {
    override var timeFormat: kotlinx.coroutines.flow.Flow<net.chronoflow.core.model.contracts.TimeFormat> =
        kotlinx.coroutines.flow
            .flowOf(
                net.chronoflow.core.model.contracts.TimeFormat.Value24,
            )
    val recovery = InMemoryWizardDraftStore()
    var discardFailure: IOException? = null
    override val store =
        object : WizardDraftStore by recovery {
            override suspend fun discard(instance: String) {
                discardFailure?.let { throw it }
                recovery.discard(instance)
            }
        }
    val wall = WizardTestClock()
    override var now = { wall.now }
    val checking = CheckingTestClock(scope.testScheduler)
    override val checkingClock = checking.clock
    override val writerScope =
        CoroutineScope(
            scope.backgroundScope.coroutineContext +
                SupervisorJob(
                    scope.backgroundScope.coroutineContext.job,
                ),
        )
    override val source = GenerationSource()
    val queries = mutableListOf<String>()
    override var airportSearch =
        AirportSearchService(
            search = { query ->
                queries.add(query)
                emptyList()
            },
            now = now,
            sleep = { delay((it * 1000).roundToLong()) },
        )
    var draft = MemberWizardDraft()
    var failure: DataFailure? = null
    var starts = 0
    val tripStarts = mutableListOf<Pair<String, TripWizardAction>>()
    val generations = mutableListOf<TripGenerationService>()

    override suspend fun start(): MemberWizardDraft {
        starts++
        failure?.let { throw it }
        return draft
    }

    override suspend fun startTrip(
        id: String,
        action: TripWizardAction,
    ): MemberWizardDraft {
        tripStarts.add(id to action)
        failure?.let { throw it }
        return draft
    }

    override fun newGeneration() =
        TripGenerationService(
            source,
            TripGenerationEffects({}, {}, {}),
            parentContext = writerScope.coroutineContext,
        ).also { generations.add(it) }

    fun checkedDraft(step: MemberWizardStep): MemberWizardDraft {
        val review = generationReview()
        val input = generationInput()
        val form =
            WizardForm.from(input).let { initial ->
                initial.copy(
                    identity = review.identity,
                    legs =
                        initial.legs.map {
                            it.copy(departLocal = "2028-10-13T09:15", arriveLocal = "2028-10-13T15:15")
                        },
                )
            }
        val draft =
            MemberWizardDraft(
                form = form,
                state =
                    draft.state.copy(
                        step = step,
                        review = review,
                        reviewedAt = wall.now,
                        sleepNeeded = false,
                    ),
            )
        return draft.copy(state = draft.state.copy(checkedInput = draft.inputFingerprint))
    }

    suspend fun close() =
        withContext(NonCancellable) {
            generations.forEach { it.cancel() }
            writerScope.coroutineContext.job.cancelAndJoin()
        }
}

internal class WizardViewModelFixture(
    deps: TripWizardDependencies?,
    entry: WizardEntry,
    presentation: net.chronoflow.feature.wizard.ui.WizardInitialPresentation? = null,
) {
    val store = ViewModelStore()
    val model =
        ViewModelProvider.create(
            store,
            TripWizardViewModel.factory(
                entry,
            ),
            MutableCreationExtras().apply {
                deps?.let { set(TripWizardDependenciesKey, it) }
                presentation?.let { set(net.chronoflow.feature.wizard.ui.WizardInitialPresentationKey, it) }
            },
        )[TripWizardViewModel::class.java]

    suspend fun clear() {
        store.clear()
        model.viewModelScope.coroutineContext.job
            .cancelAndJoin()
    }
}

internal class WizardFixture(
    val deps: FakeWizardDependencies,
    val vm: WizardViewModelFixture,
) {
    val model get() = vm.model

    suspend fun clear() = vm.clear()
}

internal suspend fun TestScope.withWizard(
    entry: WizardEntry = WizardEntry.Create,
    configure: FakeWizardDependencies.() -> Unit = {},
    block: suspend TestScope.(WizardFixture) -> Unit,
) {
    val deps = FakeWizardDependencies(this).apply(configure)
    val vm = WizardViewModelFixture(deps, entry)
    try {
        block(WizardFixture(deps, vm))
    } finally {
        withContext(NonCancellable) {
            vm.clear()
            deps.close()
        }
    }
}

internal suspend fun TestScope.awaitWizard(condition: suspend () -> Boolean) {
    runCurrent()
    assertTrue(condition())
}

internal val AMS =
    net.chronoflow.core.model.contracts.ApiAirportSuggestion(
        "AMS",
        "Amsterdam Airport Schiphol",
        "Amsterdam",
        "NL",
        "Europe/Amsterdam",
    )
internal val NRT =
    net.chronoflow.core.model.contracts.ApiAirportSuggestion(
        "NRT",
        "Narita International",
        "Tokyo",
        "JP",
        "Asia/Tokyo",
    )
internal val DXB =
    net.chronoflow.core.model.contracts.ApiAirportSuggestion(
        "DXB",
        "Dubai International",
        "Dubai",
        "AE",
        "Asia/Dubai",
    )
internal val BKK =
    net.chronoflow.core.model.contracts.ApiAirportSuggestion(
        "BKK",
        "Suvarnabhumi",
        "Bangkok",
        "TH",
        "Asia/Bangkok",
    )

internal fun routeDraft(
    stops: Int = 0,
    home: Boolean = false,
): MemberWizardDraft {
    var form = WizardForm().setRouteAirport(AMS, 0).setRouteAirport(NRT, 1)
    repeat(stops) { form = form.insertStop(if (it % 2 == 0) DXB else BKK, form.returnBoundary - 1) }
    if (home) form = form.addReturn().setRouteAirport(AMS, 1, home = true)
    return MemberWizardDraft(
        form,
        net.chronoflow.core.wizard.model.MemberWizardState(
            entered = true,
            fromLastTrip = true,
            step = if (home) MemberWizardStep.returnRoute else MemberWizardStep.whereTo,
        ),
    )
}

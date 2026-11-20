package net.chronoflow.feature.onboarding.walkthrough

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.feature.onboarding.OnboardingMainKey
import net.chronoflow.feature.onboarding.OnboardingSessions
import net.chronoflow.feature.onboarding.OnboardingSessionsKey

typealias WalkthroughUiState = WalkthroughModel.State

enum class WalkthroughEvent { Advance, Skip }

class WalkthroughViewModel(
    val replay: Boolean,
    sessions: OnboardingSessions,
    main: CoroutineDispatcher,
) : ViewModel(CoroutineScope(SupervisorJob() + main)) {
    private var model: WalkthroughModel? = null
    private var finishDelivered = false
    private val mutableState = MutableStateFlow<WalkthroughUiState?>(null)
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val resolved =
                if (replay) {
                    WalkthroughModel(WalkthroughModel.Mode.Replay, complete = {})
                } else {
                    val session = sessions.current().filterNotNull().first()
                    WalkthroughModel(complete = { session.enqueue(OutboxChange.Walkthrough) })
                }
            model = resolved
            resolved.state.collect { mutableState.value = it }
        }
    }

    fun onEvent(event: WalkthroughEvent) {
        viewModelScope.launch {
            when (event) {
                WalkthroughEvent.Advance -> model?.advance()
                WalkthroughEvent.Skip -> model?.skip()
            }
        }
    }

    fun takeFinish(): Boolean {
        if (!replay || state.value?.completed != true || finishDelivered) return false
        finishDelivered = true
        return true
    }

    companion object {
        fun factory(replay: Boolean) =
            viewModelFactory {
                initializer {
                    WalkthroughViewModel(
                        replay,
                        checkNotNull(this[OnboardingSessionsKey]),
                        this[OnboardingMainKey] ?: Dispatchers.Main.immediate,
                    )
                }
            }
    }
}

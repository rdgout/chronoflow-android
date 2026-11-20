package net.chronoflow.feature.onboarding.walkthrough

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.onboarding.SessionChangedException
import java.io.IOException
import java.security.GeneralSecurityException

class WalkthroughModel(
    val mode: Mode = Mode.FirstRun,
    private val complete: suspend () -> Unit,
    private val onFinish: () -> Unit = {},
) {
    enum class Mode { FirstRun, Replay }

    enum class Page { Timeline, Categories, Progress }

    data class State(
        val page: Page = Page.Timeline,
        val busy: Boolean = false,
        val completed: Boolean = false,
        val failure: Throwable? = null,
    )

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    suspend fun advance() {
        val value = state.value
        if (value.busy || value.completed) return
        val next = Page.entries.getOrNull(value.page.ordinal + 1)
        if (next != null) mutableState.update { it.copy(page = next) } else skip()
    }

    suspend fun skip() {
        if (state.value.busy || state.value.completed) return
        mutableState.update { it.copy(busy = true) }
        try {
            if (mode == Mode.FirstRun) complete()
            mutableState.update { it.copy(failure = null, completed = true) }
            onFinish()
        } catch (failure: StaleSessionException) {
            failed(failure)
        } catch (failure: CancellationException) {
            if (!currentCoroutineContext().isActive) throw failure
            failed(failure)
        } catch (failure: SessionChangedException) {
            failed(failure)
        } catch (failure: SessionFailure) {
            failed(failure)
        } catch (failure: DataFailure) {
            failed(failure)
        } catch (failure: DeviceIdentityFailure) {
            failed(failure)
        } catch (failure: StoreFailure) {
            failed(failure)
        } catch (failure: LocalMutationFailure) {
            failed(failure)
        } catch (failure: IOException) {
            failed(failure)
        } catch (failure: GeneralSecurityException) {
            failed(failure)
        } catch (failure: RuntimeException) {
            failed(failure)
        } finally {
            mutableState.update { it.copy(busy = false) }
        }
    }

    private fun failed(failure: Throwable) {
        mutableState.update { it.copy(failure = failure) }
    }
}

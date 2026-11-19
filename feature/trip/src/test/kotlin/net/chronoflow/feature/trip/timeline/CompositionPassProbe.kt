package net.chronoflow.feature.trip.timeline

import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.CompositionRegistrationObserver
import androidx.compose.runtime.tooling.ObservableComposition
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Body probes miss conditional invalidations that schedule a composition and then skip its bodies. */
@OptIn(ExperimentalComposeRuntimeApi::class)
class CompositionPassProbe : AutoCloseable {
    private val passes = AtomicInteger()
    val count: Int get() = passes.get()
    private val handles = IdentityHashMap<ObservableComposition, CompositionObserverHandle>()
    private val observer =
        object : CompositionObserver {
            override fun onBeginComposition(composition: ObservableComposition) {
                passes.incrementAndGet()
            }

            override fun onScopeEnter(scope: RecomposeScope) = Unit

            override fun onReadInScope(
                scope: RecomposeScope,
                value: Any,
            ) = Unit

            override fun onScopeExit(scope: RecomposeScope) = Unit

            override fun onEndComposition(composition: ObservableComposition) = Unit

            override fun onScopeInvalidated(
                scope: RecomposeScope,
                value: Any?,
            ) = Unit

            override fun onScopeDisposed(scope: RecomposeScope) = Unit
        }
    private val registrations =
        Recomposer.runningRecomposers.value.map { recomposer ->
            checkNotNull(
                recomposer.observe(
                    object : CompositionRegistrationObserver {
                        override fun onCompositionRegistered(composition: ObservableComposition) {
                            if (!handles.containsKey(composition)) {
                                handles[composition] =
                                    composition.setObserver(observer)
                            }
                        }

                        override fun onCompositionUnregistered(composition: ObservableComposition) {
                            handles.remove(composition)?.dispose()
                        }
                    },
                ),
            )
        }

    override fun close() {
        registrations.forEach { it.dispose() }
        handles.values.toList().forEach { it.dispose() }
        handles.clear()
    }
}

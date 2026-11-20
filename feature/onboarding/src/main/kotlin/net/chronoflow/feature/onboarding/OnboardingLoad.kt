package net.chronoflow.feature.onboarding

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException

internal fun <T> retryOnboardingLoad(source: () -> Flow<T>): Flow<T> =
    flow { emitAll(source()) }.retryWhen { failure, _ ->
        when (failure) {
            is CancellationException -> {
                throw failure
            }

            is IllegalArgumentException, is StoreFailure, is IOException -> {
                delay(100)
                true
            }

            else -> {
                false
            }
        }
    }

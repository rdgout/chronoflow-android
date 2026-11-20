package net.chronoflow.feature.onboarding

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.isActive
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException
import android.database.SQLException as AndroidSQLException
import java.sql.SQLException as JdbcSQLException

internal fun <T> retryOnboardingLoad(source: () -> Flow<T>): Flow<T> =
    flow {
        while (true) {
            emitAll(source())
            delay(100)
        }
    }.retryWhen { failure, _ ->
        when (failure) {
            is CancellationException -> {
                if (!currentCoroutineContext().isActive) throw failure
            }

            is IllegalArgumentException, is StoreFailure, is IOException,
            is AndroidSQLException, is JdbcSQLException,
            -> {
            }

            else -> {
                return@retryWhen false
            }
        }
        delay(100)
        true
    }

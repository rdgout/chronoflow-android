package net.chronoflow.app.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.chronoflow.app.AppGraphKey
import net.chronoflow.feature.auth.twofactor.TwoFactorViewModel

internal object TwoFactorViewModelFactory : ViewModelProvider.Factory by (
    viewModelFactory {
        initializer {
            val graph = checkNotNull(this[AppGraphKey])
            TwoFactorViewModel(
                graph.auth.signIn,
                graph.auth.router,
                CoroutineScope(SupervisorJob() + graph.dispatchers.main),
            )
        }
    }
)

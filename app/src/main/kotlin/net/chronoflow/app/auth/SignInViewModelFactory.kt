package net.chronoflow.app.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.chronoflow.app.AppGraphKey
import net.chronoflow.feature.auth.signin.SignInViewModel

internal object SignInViewModelFactory : ViewModelProvider.Factory by (
    viewModelFactory {
        initializer {
            val graph = checkNotNull(this[AppGraphKey])
            SignInViewModel(graph.auth.signIn, CoroutineScope(SupervisorJob() + graph.dispatchers.main))
        }
    }
)

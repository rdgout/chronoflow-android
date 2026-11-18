package net.chronoflow.app.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import net.chronoflow.app.AppGraphKey
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.signup.SignUpViewModel

internal object SignUpViewModelFactory : ViewModelProvider.Factory by (
    viewModelFactory {
        initializer {
            val graph = checkNotNull(this[AppGraphKey])
            SignUpViewModel(
                graph.auth.signIn.makeSignUpModel { graph.rootNavigator.navigate(Route.SignIn) },
                CoroutineScope(SupervisorJob() + graph.dispatchers.main),
            )
        }
    }
)

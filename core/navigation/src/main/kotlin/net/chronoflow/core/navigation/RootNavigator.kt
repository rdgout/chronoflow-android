package net.chronoflow.core.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.chronoflow.core.model.launch.Gate

data class RootState(
    val route: Route? = null,
    val revision: Long = 0,
)

class RootNavigator {
    private val mutableState = MutableStateFlow(RootState())
    val state = mutableState.asStateFlow()

    fun navigate(to: Route) {
        mutableState.update { RootState(to, it.revision + 1) }
    }
}

fun Gate.toRoute(): Route =
    when (this) {
        Gate.Verify -> Route.Verify
        Gate.Terms -> Route.Terms
        Gate.Walkthrough -> Route.Walkthrough()
        Gate.App -> Route.Today
    }

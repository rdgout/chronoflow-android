package net.chronoflow.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import net.chronoflow.core.navigation.RootState
import net.chronoflow.core.navigation.Route

@Composable
internal fun RootContent(
    state: RootState,
    onBack: () -> Unit,
    content: @Composable (Route) -> Unit,
) {
    val currentContent = rememberUpdatedState(content)
    val revision =
        when (state.route) {
            Route.GuestWizard, Route.Terms, is Route.Walkthrough -> state.revision
            else -> null
        }
    val entries =
        remember(state.route, revision) {
            state.route
                ?.let { route ->
                    listOf(
                        NavEntry(
                            route,
                            contentKey = revision?.let { route to it } ?: route,
                        ) { currentContent.value(it) },
                    )
                }.orEmpty()
        }
    if (entries.isNotEmpty()) {
        NavDisplay(
            entries = entries,
            onBack = onBack,
        )
    }
}

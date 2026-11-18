package net.chronoflow.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
                            contentKey = revision?.let { "$route:$it" } ?: route.toString(),
                        ) { currentContent.value(it) },
                    )
                }.orEmpty()
        }
    val decorated =
        rememberDecoratedNavEntries(
            entries,
            listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
        )
    if (decorated.isNotEmpty()) {
        NavDisplay(
            entries = decorated,
            onBack = onBack,
            transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
            predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        )
    }
}

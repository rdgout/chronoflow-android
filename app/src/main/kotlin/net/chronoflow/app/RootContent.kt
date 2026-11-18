package net.chronoflow.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import net.chronoflow.core.navigation.RootState
import net.chronoflow.core.navigation.Route

@Composable
internal fun RootContent(
    state: RootState,
    onBack: () -> Unit,
    content: @Composable (Route) -> Unit,
) {
    val revision = rememberUpdatedState(state.revision)
    val currentContent = rememberUpdatedState(content)
    val stack = remember { mutableStateListOf<NavKey>().apply { state.route?.let(::add) } }
    LaunchedEffect(state.revision, state.route) {
        stack.clear()
        state.route?.let(stack::add)
    }
    if (stack.isNotEmpty()) {
        NavDisplay(
            backStack = stack,
            onBack = onBack,
            entryDecorators = emptyList(),
            entryProvider = { route -> NavEntry(route) { key(revision.value) { currentContent.value(it as Route) } } },
        )
    }
}

package net.chronoflow.feature.settings.signout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.feature.settings.home.SettingsHomeState
import net.chronoflow.feature.settings.settingsAttempt

@Composable
fun PendingSheetFromOutbox(
    count: Int,
    outbox: Flow<List<OutboxRecord>>,
    onStay: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(outbox) { mutableStateOf<SettingsHomeState?>(null) }
    val stay by rememberUpdatedState(onStay)
    LaunchedEffect(outbox) {
        settingsAttempt({}) {
            outbox.collect { rows ->
                state = SettingsHomeState().apply(outbox = rows)
                if (rows.isEmpty()) stay()
            }
        }
    }
    state?.takeIf { count > 0 && it.pending.isNotEmpty() }?.let {
        SignOutPendingSheet(it.pending, busy = false, onStay, onDiscard, modifier)
    }
}

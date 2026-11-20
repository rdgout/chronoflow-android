package net.chronoflow.app.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.feature.settings.signout.PendingSheetFromOutbox

@Composable
internal fun TermsSignOutConfirmation(
    count: Int,
    outbox: Flow<List<OutboxRecord>>,
    onStay: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PendingSheetFromOutbox(count, outbox, onStay, onDiscard, modifier)
}

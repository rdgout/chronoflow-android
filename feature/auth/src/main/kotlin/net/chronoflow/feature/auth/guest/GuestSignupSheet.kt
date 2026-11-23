package net.chronoflow.feature.auth.guest

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.feature.auth.signup.PolicyOpener

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestSignupSheet(
    state: GuestSignupUiState,
    fields: GuestSignupFields,
    policyOpener: PolicyOpener,
    onEvent: (GuestSignupEvent) -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    val busy by rememberUpdatedState(state.form.busy)
    val sheet =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !busy },
        )
    CfModalBottomSheet(
        onDismissRequest = { if (!busy) onEvent(GuestSignupEvent.Dismiss) },
        modifier = modifier,
        sheetState = sheet,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !busy),
    ) {
        GuestSignupContent(state, fields, policyOpener, onEvent, takeAnnouncement = takeAnnouncement)
    }
}

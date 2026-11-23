package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import net.chronoflow.feature.auth.model.AuthFormState

@Composable
internal fun AuthFailureFeedback(
    form: AuthFormState,
    fields: List<Pair<String, FocusRequester>>,
    banner: BringIntoViewRequester,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(form.busy, form.announcementRevision) {
        if (form.busy) {
            focus.clearFocus()
        } else if (form.failure != null) {
            val first = fields.firstOrNull { it.first in form.fields }
            if (first != null) {
                first.second.requestFocus()
            } else {
                focus.clearFocus()
                keyboard?.hide()
                withFrameNanos { }
                banner.bringIntoView()
            }
        }
    }
}

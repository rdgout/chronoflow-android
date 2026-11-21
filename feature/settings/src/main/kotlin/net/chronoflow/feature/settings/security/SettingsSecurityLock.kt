package net.chronoflow.feature.settings.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import net.chronoflow.feature.settings.components.SettingsError
import net.chronoflow.feature.settings.security.prompt.PromptAlertLine
import java.time.Instant

@Composable
internal fun SettingsSecurityLock(
    retryAt: Instant?,
    now: () -> Instant,
) {
    if (retryAt != null) {
        PromptAlertLine(retryAt, now) { remaining ->
            if (remaining > 0) {
                SettingsError(
                    lockoutText(remaining),
                    Modifier.testTag("settings.security.lock").semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

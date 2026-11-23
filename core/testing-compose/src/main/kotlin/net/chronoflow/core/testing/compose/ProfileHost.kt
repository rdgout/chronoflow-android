package net.chronoflow.core.testing.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.ui.window.LocalWindowSize

/** Provides the profile's insets through the same local the root fills from `WindowInsets.safeDrawing`. */
@Composable
fun ProfileHost(
    profile: DeviceProfile,
    content: @Composable () -> Unit,
) {
    val insets =
        remember(profile) {
            WindowInsets(
                left = profile.insets.left.dp,
                top = profile.insets.top.dp,
                right = profile.insets.right.dp,
                bottom = profile.insets.bottom.dp,
            )
        }
    CompositionLocalProvider(
        LocalSafeInsets provides insets,
        LocalWindowSize provides DpSize(profile.widthDp.dp, profile.heightDp.dp),
        LocalLayoutDirection provides LayoutDirection.Ltr,
        content = content,
    )
}

package net.chronoflow.core.testing.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.testing.SafeInsetsDp
import net.chronoflow.core.ui.window.LocalWindowSize

class WindowSizeFake(
    initial: DpSize,
) {
    var size by mutableStateOf(initial)
}

@Composable
fun WindowSizeFakeHost(
    fake: WindowSizeFake,
    modifier: Modifier = Modifier,
    insets: SafeInsetsDp = SafeInsetsDp(0f, 0f, 0f, 0f),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalWindowSize provides fake.size,
        LocalSafeInsets provides WindowInsets(insets.left.dp, insets.top.dp, insets.right.dp, insets.bottom.dp),
        LocalLayoutDirection provides LayoutDirection.Ltr,
    ) {
        Box(modifier.requiredSize(fake.size), contentAlignment = Alignment.TopStart) { content() }
    }
}

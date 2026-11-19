@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.core.ui.window

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.DpSize

// Window bounds include the space consumed by the shell rail.
val LocalWindowSize = compositionLocalOf<DpSize> { error("LocalWindowSize is provided at the root and by ProfileHost") }

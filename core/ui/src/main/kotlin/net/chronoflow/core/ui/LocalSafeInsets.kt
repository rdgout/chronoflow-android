@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.core.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.staticCompositionLocalOf

/** The root provides the window's safe drawing insets; tests provide a device profile's insets through the same local. */
val LocalSafeInsets =
    staticCompositionLocalOf<WindowInsets> {
        error("LocalSafeInsets is provided at the root and by ProfileHost")
    }

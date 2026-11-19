@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.feature.today.ui

import androidx.compose.runtime.staticCompositionLocalOf

interface TodayProbe {
    enum class Kind { SCREEN, HERO, PANEL, ROW, LEAF }

    fun composed(
        kind: Kind,
        id: String,
    )
}

val LocalTodayProbe = staticCompositionLocalOf<TodayProbe?> { null }

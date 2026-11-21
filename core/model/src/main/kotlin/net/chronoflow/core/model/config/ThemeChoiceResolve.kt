package net.chronoflow.core.model.config

fun ThemeChoice.resolve(systemDark: Boolean): Boolean =
    when (this) {
        ThemeChoice.System -> systemDark
        ThemeChoice.Light -> false
        ThemeChoice.Dark -> true
    }

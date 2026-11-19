package net.chronoflow.app

enum class IconAppearance { LightIcons, DarkIcons }

object SystemBarStyles {
    fun statusBar(
        night: Boolean,
        overlayShowing: Boolean,
        onSky: Boolean,
    ): IconAppearance = if (night || overlayShowing || onSky) IconAppearance.LightIcons else IconAppearance.DarkIcons

    fun navigationBar(night: Boolean): IconAppearance =
        if (night) IconAppearance.LightIcons else IconAppearance.DarkIcons
}

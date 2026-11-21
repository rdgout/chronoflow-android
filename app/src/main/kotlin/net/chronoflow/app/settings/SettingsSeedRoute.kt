package net.chronoflow.app.settings

import net.chronoflow.core.navigation.Route

object SettingsSeedRoute {
    fun path(raw: String?): List<Route>? =
        when (raw) {
            "profile" -> listOf(Route.Account, Route.Profile)
            "language" -> listOf(Route.Account, Route.Language)
            "delete" -> listOf(Route.Account)
            "password" -> listOf(Route.Security, Route.Password)
            "twoFactor", "twoFactorSetup" -> listOf(Route.Security, Route.TwoFactorSettings)
            "account" -> listOf(Route.Account)
            "security" -> listOf(Route.Security)
            "bodyClock" -> listOf(Route.BodyClock)
            "preferences" -> listOf(Route.Preferences)
            "plan" -> listOf(Route.Plan)
            "reminders" -> listOf(Route.Reminders)
            else -> null
        }
}

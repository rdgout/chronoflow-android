package net.chronoflow.feature.reminders

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles

internal fun remindersGuardCases(): List<Array<Any>> =
    listOf(
        DeviceProfiles.small,
        DeviceProfiles.pixel8,
        DeviceProfiles.foldInner,
        DeviceProfiles.landscapeSideNav,
        DeviceProfiles.largeFont,
        DeviceProfiles.small.copy(name = "minimum280", widthDp = 280),
        DeviceProfiles.small.copy(name = "smallLargeFont", fontScale = 1.3f),
    ).flatMap { profile ->
        AppLanguage.entries.flatMap { language -> listOf(false, true).map { dark -> arrayOf(profile, language, dark) } }
    }

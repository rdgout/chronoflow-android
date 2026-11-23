package net.chronoflow.core.testing.compose

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles

/** One layout-guard run: a device profile and the language it renders in. Theme is not a dimension (see [LayoutMatrix]). */
data class LayoutCase(
    val profile: DeviceProfile,
    val language: AppLanguage,
) {
    override fun toString(): String = "${profile.name}-${language.tag}"
}

/**
 * The default guard matrix (user decision, 2026-11-16): light only, because theme never changes geometry.
 * `small` and `largeFont` run in nl, the longest copy; `pixel8` runs once, in en. Screens with a wide or
 * side-navigation layout add `foldInner` and `landscapeSideNav`. Captures cover light and dark.
 *
 * Use it as the parameter list of a `ParameterizedRobolectricTestRunner` so each case is its own test.
 */
object LayoutMatrix {
    val phone: List<LayoutCase> =
        listOf(
            LayoutCase(DeviceProfiles.small, AppLanguage.Nl),
            LayoutCase(DeviceProfiles.largeFont, AppLanguage.Nl),
            LayoutCase(DeviceProfiles.pixel8, AppLanguage.En),
        )

    val wide: List<LayoutCase> =
        phone +
            listOf(
                LayoutCase(DeviceProfiles.foldInner, AppLanguage.En),
                LayoutCase(DeviceProfiles.landscapeSideNav, AppLanguage.En),
            )

    fun parameters(cases: List<LayoutCase>): Collection<Array<Any>> = cases.map { arrayOf<Any>(it) }
}

package net.chronoflow.core.testing.compose

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.config.ConfigurationRules
import net.chronoflow.core.testing.DeviceProfile
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment

/**
 * Applies a device profile (size, density, capped font scale, locale, night mode) before the test body.
 *
 * It must run before the compose rule launches its activity:
 * `@get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(composeRule)`.
 * A class-level rule cannot switch profiles per method, so parameterize the profile (see [LayoutMatrix]).
 */
class ProfileRule(
    val profile: DeviceProfile,
    val language: AppLanguage = AppLanguage.En,
    val dark: Boolean = false,
) : TestRule {
    constructor(case: LayoutCase, dark: Boolean = false) : this(case.profile, case.language, dark)

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                RuntimeEnvironment.setQualifiers(profileQualifiers(profile, language, dark))
                RuntimeEnvironment.setFontScale(ConfigurationRules.capFontScale(profile.fontScale))
                base.evaluate()
            }
        }
}

/** Qualifiers in Android's order, for example `nl-rNL-w360dp-h640dp-port-notnight-320dpi`. */
internal fun profileQualifiers(
    profile: DeviceProfile,
    language: AppLanguage,
    dark: Boolean,
): String {
    val region = if (language == AppLanguage.Nl) "NL" else "US"
    val orientation = if (profile.isLandscape) "land" else "port"
    val night = if (dark) "night" else "notnight"
    val size = "w${profile.widthDp}dp-h${profile.heightDp}dp"
    return "${language.tag}-r$region-$size-$orientation-$night-${profile.densityDpi}dpi"
}

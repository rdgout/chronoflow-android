package net.chronoflow.feature.auth.reset

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ResetGuardsTest internal constructor(
    private val case: ResetGuardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case.profile, case.language)).around(compose)

    @Test fun resetStatesRespectTheWindowAndTextCap() {
        compose.setContent {
            ProfileHost(
                case.profile,
            ) { ChronoFlowTheme(darkTheme = false) { ResetSample(case.state) } }
        }
        Guards.assertAll(compose, case.profile, headingTags = setOf("auth.heading"))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        internal fun cases(): List<Array<Any>> {
            val fields =
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.landscapeSideNav,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    listOf(AppLanguage.En, AppLanguage.Nl).map { ResetGuardCase("fieldErrors", profile, it) }
                }
            return (
                fields +
                    listOf(
                        ResetGuardCase("done", DeviceProfiles.small, AppLanguage.Nl),
                        ResetGuardCase("done", DeviceProfiles.largeFont, AppLanguage.Nl),
                        ResetGuardCase("form", DeviceProfiles.pixel8, AppLanguage.En),
                    )
            ).map { arrayOf<Any>(it) }
        }
    }
}

internal data class ResetGuardCase(
    val state: String,
    val profile: DeviceProfile,
    val language: AppLanguage,
) {
    override fun toString(): String = "$state-${profile.name}-${language.tag}"
}

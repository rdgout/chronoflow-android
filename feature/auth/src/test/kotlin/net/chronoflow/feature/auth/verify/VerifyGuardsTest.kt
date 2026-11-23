package net.chronoflow.feature.auth.verify

import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.runTest
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
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class VerifyGuardsTest internal constructor(
    private val case: VerifyGuardCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)

    @Test fun testWaitingFailedAndErrorActionsFitTheLeanDeviceProfiles() {
        lateinit var sample: VerifySampleState
        runTest(timeout = 60.seconds) { sample = verifySnapshotModel(case.state, case.email) }
        compose.setContent {
            ProfileHost(case.profile) { ChronoFlowTheme(darkTheme = case.dark) { VerifySample(sample) } }
        }
        Guards.assertAll(compose, case.profile, headingTags = setOf("auth.heading"))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        internal fun cases(): List<Array<Any>> {
            val tallest =
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.landscapeSideNav,
                    DeviceProfiles.largeFont,
                ).flatMap { profile ->
                    listOf(AppLanguage.En, AppLanguage.Nl).map { VerifyGuardCase("throttle", profile, it) }
                }
            val small =
                listOf(
                    "waiting",
                    "countdown",
                    "failed",
                    "server",
                    "offline",
                    "failedServer",
                    "failedThrottle",
                    "failedOffline",
                    "signOutError",
                ).map { VerifyGuardCase(it, DeviceProfiles.small, AppLanguage.Nl) }
            return (
                tallest + small +
                    listOf(
                        VerifyGuardCase(
                            "waiting",
                            DeviceProfiles.largeFont,
                            AppLanguage.Nl,
                            email = "a.very.long.address.for.testing.purposes@example-domain.com",
                        ),
                        VerifyGuardCase("failed", DeviceProfiles.pixel8, AppLanguage.En, true),
                    )
            ).map { arrayOf<Any>(it) }
        }
    }
}

internal data class VerifyGuardCase(
    val state: String,
    val profile: DeviceProfile,
    val language: AppLanguage,
    val dark: Boolean = false,
    val email: String = "rick@example.com",
) {
    override fun toString() = "$state-${profile.name}-${language.tag}-dark=$dark-email=$email"
}

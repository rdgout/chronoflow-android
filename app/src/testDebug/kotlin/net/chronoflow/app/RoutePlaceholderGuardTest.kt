package net.chronoflow.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.LaunchRoute
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoutePlaceholderGuardTest(
    private val profile: DeviceProfile,
    private val route: LaunchRoute,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, AppLanguage.Nl, dark)).around(compose)

    @Test fun routeLabelFitsTheWindow() {
        compose.setContent {
            ChronoFlowTheme(
                darkTheme = dark,
                reduceMotion = true,
            ) { RoutePlaceholder(route, LaunchPlatform.routeLabel) }
        }
        compose.onNodeWithTag("route.${route.name.lowercase()}").assertExists()
        compose.onNodeWithTag("route.label", useUnmergedTree = true).assertExists()
        Guards.assertTextFits(compose, profile)
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}, {1}, dark={2}")
        fun cases(): List<Array<Any>> =
            listOf(
                DeviceProfiles.small,
                DeviceProfiles.pixel8,
                DeviceProfiles.foldInner,
                DeviceProfiles.largeFont,
            ).flatMap { profile ->
                listOf(LaunchRoute.Today).flatMap { route ->
                    listOf(false, true).map { dark -> arrayOf<Any>(profile, route, dark) }
                }
            }
    }
}

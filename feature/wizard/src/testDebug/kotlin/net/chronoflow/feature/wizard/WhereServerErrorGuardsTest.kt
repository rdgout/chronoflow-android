package net.chronoflow.feature.wizard

import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
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
class WhereServerErrorGuardsTest(
    private val case: LayoutCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun serverRouteErrorFitsTheProfile() {
        lateinit var formatter: CopyLineFormatter
        runTest(timeout = 60.seconds) { formatter = wizardCopyFormatter() }
        val state = screenState(serverErrorDraft())
        compose.setContent { WizardTestHost(case.profile) { WizardScreen(state, formatter, {}) } }
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile, rootTag = "wizard.window")
        Guards.assertNoInteractiveOverlap(
            compose,
            exemptTags = setOf("wizard.route.stops"),
            rootTag = "wizard.window",
        )
        Guards.assertHeadings(compose, "wizard.title")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() =
            listOf(
                LayoutCase(DeviceProfiles.small, AppLanguage.Nl),
                LayoutCase(DeviceProfiles.largeFont, AppLanguage.Nl),
                LayoutCase(DeviceProfiles.small, AppLanguage.En),
            ).also { check(it.size == 3) }
    }
}

package net.chronoflow.feature.wizard

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.height
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardQuestionLargeTextTest(
    private val scale: Float,
) {
    private val profile = DeviceProfiles.small.copy(name = "question-$scale", fontScale = scale)
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, AppLanguage.Nl)).around(compose)

    @Test fun largeTextKeepsFullOptionsAndTheHintInsideTheSafeArea() {
        lateinit var state: WizardUiState
        lateinit var formatter: CopyLineFormatter
        runTest(timeout = 60.seconds) {
            state = boardState("WizardQShift", Locale.forLanguageTag("nl"))
            formatter = wizardCopyFormatter()
        }
        compose.setContent { WizardTestHost(profile) { WizardScreen(state, formatter, {}) } }
        val first = compose.onNodeWithTag("wizard.choice.question.false").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("wizard.choice.question.true").getUnclippedBoundsInRoot()
        if (scale == 1f) {
            assertEquals(64f, first.height.value, .5f)
            assertEquals(64f, second.height.value, .5f)
        } else {
            assertTrue(first.height.value >= 72f)
            assertTrue(second.height.value >= 72f)
        }
        Guards.assertTextFits(compose, profile)
        val hint = compose.onNodeWithTag("wizard.question.hint").getUnclippedBoundsInRoot()
        assertTrue(hint.bottom.value <= profile.heightDp - profile.insets.bottom + .5f)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "scale-{0}")
        fun cases() = listOf(1f, 1.15f, 1.3f)
    }
}

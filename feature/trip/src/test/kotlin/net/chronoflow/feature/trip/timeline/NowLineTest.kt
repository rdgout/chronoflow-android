package net.chronoflow.feature.trip.timeline

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.plan.trippage.NowLineValue
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/** The now line node exists only with a clock value, reads its description from the resource of the active locale and sits at its offset. */
@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class NowLineTest(
    private val case: LayoutCase,
) {
    private val composeRule = createComposeRule()
    private val clock = DayClocks().clock("d")

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(composeRule)

    private fun show() {
        composeRule.setContent {
            ProfileHost(case.profile) {
                Box(Modifier.size(379.dp, 400.dp)) { NowLine(clock, "d", 379.0) }
            }
        }
    }

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun description(value: String): String = context.getString(R.string.plan_now_x, value)

    @Test fun existsWhenTheClockHasAValueAndNotWhenItIsNull() {
        show()
        val node = hasContentDescription(description("08:00"))
        composeRule.onNode(node).assertDoesNotExist()
        composeRule.runOnIdle { clock.value.value = NowLineValue(100.0, "08:00", 0L) }
        composeRule.onNode(node).assertExists()
        composeRule.runOnIdle { clock.value.value = null }
        composeRule.onNode(node).assertDoesNotExist()
    }

    @Test fun theLocaleOfTheRunIsTheLocaleOfTheCase() {
        assertEquals(
            case.language.tag,
            context.resources.configuration.locales[0]
                .language,
        )
    }

    @Test fun itsTopIsTheOffsetMinusHalfItsHeight() {
        show()
        composeRule.runOnIdle { clock.value.value = NowLineValue(100.0, "08:00", 0L) }
        composeRule.waitForIdle()
        val node = composeRule.onNode(hasContentDescription(description("08:00"))).fetchSemanticsNode()
        val expected = 100.0 * case.profile.density - node.size.height / 2f
        assertEquals(0.0, abs(node.boundsInRoot.top - expected), 1.0)
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> =
            LayoutMatrix.parameters(
                listOf(
                    LayoutCase(DeviceProfiles.pixel8, net.chronoflow.core.model.config.AppLanguage.En),
                    LayoutCase(DeviceProfiles.pixel8, net.chronoflow.core.model.config.AppLanguage.Nl),
                ),
            )
    }
}

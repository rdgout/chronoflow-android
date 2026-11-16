package net.chronoflow.core.testing.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class LargeFontEqualsCapTest(
    private val name: String,
    private val expectedScale: Float,
    private val otherScale: Float,
) {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles[name])).around(composeRule)

    @Test
    fun largeFontRendersAtTheCap() {
        var ambientScale = 0f
        composeRule.setContent {
            val ambient = LocalDensity.current
            ambientScale = ambient.fontScale
            Column {
                Text("Aa Bb 12:00", fontSize = 16.sp, modifier = Modifier.testTag("cap.subject"))
                CompositionLocalProvider(LocalDensity provides Density(ambient.density, expectedScale)) {
                    Text("Aa Bb 12:00", fontSize = 16.sp, modifier = Modifier.testTag("cap.expected"))
                }
                CompositionLocalProvider(LocalDensity provides Density(ambient.density, otherScale)) {
                    Text("Aa Bb 12:00", fontSize = 16.sp, modifier = Modifier.testTag("cap.other"))
                }
            }
        }
        composeRule.waitForIdle()

        fun height(tag: String) =
            composeRule
                .onNodeWithTag(tag)
                .fetchSemanticsNode()
                .size.height
        assertThat(ambientScale).isEqualTo(expectedScale)
        assertThat(height("cap.subject")).isEqualTo(height("cap.expected"))
        assertThat(height("cap.subject")).isNotEqualTo(height("cap.other"))
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun profiles(): Collection<Array<Any>> = listOf(arrayOf("largeFont", 1.3f, 1.0f), arrayOf("pixel8", 1.0f, 1.3f))
    }
}

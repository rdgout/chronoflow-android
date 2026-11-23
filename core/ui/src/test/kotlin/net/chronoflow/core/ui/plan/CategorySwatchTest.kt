package net.chronoflow.core.ui.plan

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class CategorySwatchTest(
    private val size: CategorySwatchSize,
    private val dimension: Int,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun sizeAndDecorativeSemantics() {
        compose.setContent {
            ChronoFlowTheme {
                Box(
                    Modifier.testTag("wrapper"),
                ) { CategorySwatch(BlockType.Sleep, size = size) }
            }
        }
        val node =
            compose
                .onNodeWithTag(
                    "wrapper",
                ).assertWidthIsEqualTo(dimension.dp)
                .assertHeightIsEqualTo(dimension.dp)
                .fetchSemanticsNode()
        assertTrue(node.children.all { !it.config.iterator().hasNext() })
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(arrayOf(CategorySwatchSize.Small, 28), arrayOf(CategorySwatchSize.Regular, 32))
    }
}

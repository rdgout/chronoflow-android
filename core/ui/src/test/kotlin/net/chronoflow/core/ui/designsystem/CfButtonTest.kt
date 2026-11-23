@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfIconButton
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfButtonTest : PrimitiveTestFixture() {
    @Test fun iconSizeKeepsTheDefaultAndTheTouchTarget() {
        host {
            Column {
                CfIconButton(
                    MaterialSymbols.MoreVert,
                    stringResource(R.string.plan_trip_actions),
                    {},
                    Modifier.testTag("icon.default"),
                )
                CfIconButton(
                    MaterialSymbols.MoreVert,
                    stringResource(R.string.plan_trip_actions),
                    {},
                    Modifier.testTag("icon.custom"),
                    iconSize = 20.dp,
                )
            }
        }
        val default = compose.onNodeWithTag("icon.default", useUnmergedTree = true)
        val custom = compose.onNodeWithTag("icon.custom", useUnmergedTree = true)
        val defaultIcon = default.onChild().getUnclippedBoundsInRoot()
        val customIcon = custom.onChild().getUnclippedBoundsInRoot()
        assertEquals(AuthTokens.icon, (defaultIcon.right - defaultIcon.left).value, 0.5f)
        assertEquals(20f, (customIcon.right - customIcon.left).value, 0.5f)
        assertTrue(default.fetchSemanticsNode().layoutInfo.width >= 48 * compose.density.density - 1)
        assertTrue(custom.fetchSemanticsNode().layoutInfo.width >= 48 * compose.density.density - 1)
    }

    @Test fun clickFiresOnce() {
        var calls = 0
        host { CfButton(stringResource(R.string.auth_sign_in), { calls++ }, Modifier.testTag("button")) }
        compose.onNodeWithTag("button").performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun disabledAndLoadingButtonsDoNotClick() {
        var calls = 0
        host {
            Column {
                CfButton(
                    stringResource(R.string.auth_sign_in),
                    { calls++ },
                    Modifier.testTag("disabled"),
                    enabled = false,
                )
                CfButton(
                    stringResource(R.string.auth_sign_in),
                    { calls++ },
                    Modifier.testTag("loading"),
                    loading = true,
                    loadingDescription = stringResource(R.string.settings_saved),
                )
            }
        }
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performClick()
        val loading = compose.onNodeWithTag("loading").assertIsNotEnabled()
        loading.performClick()
        assertTrue(loading.fetchSemanticsNode().config.contains(SemanticsProperties.StateDescription))
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun smallVisualKeepsAnAndroidTouchTarget() {
        host { CfButton(stringResource(R.string.screens_skip), {}, Modifier.testTag("small"), size = CfButtonSize.Sm) }
        val node = compose.onNodeWithTag("small").fetchSemanticsNode()
        assertEquals(32f * compose.density.density, node.boundsInRoot.height, 1f)
        assertTrue(node.layoutInfo.height >= 48f * compose.density.density)
    }
}

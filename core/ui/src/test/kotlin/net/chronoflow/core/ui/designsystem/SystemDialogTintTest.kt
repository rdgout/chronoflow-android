@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.system.CfAlertDialog
import net.chronoflow.core.designsystem.system.CfDialogAction
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.system.CfSnackbarHost
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class SystemDialogTintTest(
    override val dark: Boolean,
) : PrimitiveTestFixture() {
    @Test fun sheetKeepsTheCardSurface() {
        host { CfModalBottomSheet({}, Modifier.testTag("sheet")) { Box(Modifier.size(100.dp, 100.dp)) } }
        val p = compose.onNode(isDialog()).captureToImage().toPixelMap()
        assertTokenColor(p[p.width / 2, p.height - (24 * compose.density.density).toInt()], colors.card)
    }

    @Test fun alertKeepsThePopoverSurface() {
        host {
            CfAlertDialog(
                stringResource(R.string.settings_change_password),
                null,
                CfDialogAction(stringResource(R.string.settings_done), {}),
                {},
            )
        }
        val p = compose.onNode(isDialog()).captureToImage().toPixelMap()
        assertTokenColor(p[p.width - 4, p.height / 2], colors.popover)
    }

    @Test fun snackbarContainerBodyAndActionUseInverseTokens() {
        val state = SnackbarHostState()
        host {
            val message = stringResource(R.string.settings_saved)
            val action = stringResource(R.string.auth_try_again)
            LaunchedEffect(Unit) { state.showSnackbar(message, action, duration = SnackbarDuration.Indefinite) }
            CfSnackbarHost(state, Modifier.testTag("snackbar"))
        }
        val p = pixels("snackbar")
        assertTokenColor(p[p.width / 2, (16 * compose.density.density).toInt()], colors.foreground)
        val action = compose.onNode(hasAnyAncestor(hasTestTag("snackbar")) and hasClickAction())
        assertEquals(
            colors.background,
            action
                .textLayout()
                .layoutInput.style.color,
        )
        val bodyMatcher = hasAnyAncestor(hasTestTag("snackbar")) and !hasAnyAncestor(hasClickAction())
        val body =
            compose.onNode(
                bodyMatcher and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            )
        assertEquals(
            colors.background,
            body
                .textLayout()
                .layoutInput.style.color,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}

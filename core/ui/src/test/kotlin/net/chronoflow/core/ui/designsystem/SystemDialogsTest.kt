@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.chronoflow.core.designsystem.system.CfAlertDialog
import net.chronoflow.core.designsystem.system.CfDialogAction
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.system.CfSnackbarHost
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
class SystemDialogsTest : PrimitiveTestFixture() {
    @Test fun alertConfirmAndDismissCallbacksRemainNativeActions() {
        var confirms = 0
        var dismisses = 0
        host {
            CfAlertDialog(
                stringResource(R.string.settings_change_password),
                null,
                CfDialogAction(stringResource(R.string.settings_done), { confirms++ }),
                {},
                CfDialogAction(stringResource(R.string.settings_cancel), { dismisses++ }),
            )
        }
        compose.onNodeWithTag("cf.dialog.confirm").performClick()
        compose.onNodeWithTag("cf.dialog.dismiss").performClick()
        compose.runOnIdle {
            assertEquals(1, confirms)
            assertEquals(1, dismisses)
        }
    }

    @Test fun destructiveConfirmPropagatesTheDestructiveTextColor() {
        host {
            CfAlertDialog(
                stringResource(R.string.settings_change_password),
                null,
                CfDialogAction(stringResource(R.string.settings_done), {}, destructive = true),
                {},
            )
        }
        val label =
            compose.onNode(
                hasAnyAncestor(hasTestTag("cf.dialog.confirm")) and
                    SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            )
        assertEquals(
            colors.destructive,
            label
                .textLayout()
                .layoutInput.style.color,
        )
    }

    @Test fun snackbarReportsEachPresentationOnceAcrossRecomposition() {
        val state = SnackbarHostState()
        var counter = 0
        var revision by mutableIntStateOf(0)
        lateinit var scope: CoroutineScope
        lateinit var message: String
        host {
            scope = rememberCoroutineScope()
            message = stringResource(R.string.settings_saved)
            CfSnackbarHost(state, Modifier.testTag("host.$revision"), onPresented = { counter++ })
        }
        compose.runOnIdle { scope.launch { state.showSnackbar(message, duration = SnackbarDuration.Indefinite) } }
        compose.waitUntil(3000) { counter == 1 }
        compose.runOnIdle { revision++ }
        compose.runOnIdle {
            assertEquals(1, counter)
            state.currentSnackbarData!!.dismiss()
        }
        compose.runOnIdle { scope.launch { state.showSnackbar(message, duration = SnackbarDuration.Indefinite) } }
        compose.waitUntil(3000) { counter == 2 }
        compose.runOnIdle { state.currentSnackbarData!!.dismiss() }
    }

    @Test
    @Config(qualifiers = "w841dp-h701dp-420dpi")
    fun sheetContentRespectsTheWideWindowCap() {
        host {
            CfModalBottomSheet(
                {},
                Modifier.testTag("sheet"),
            ) { Box(Modifier.size(100.dp, 48.dp).testTag("sheet.content")) }
        }
        assertTrue(
            compose
                .onNodeWithTag("sheet")
                .fetchSemanticsNode()
                .boundsInRoot.width <= 640f * compose.density.density + 1,
        )
    }
}

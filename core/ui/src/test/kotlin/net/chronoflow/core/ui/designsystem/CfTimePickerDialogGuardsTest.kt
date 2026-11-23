package net.chronoflow.core.ui.designsystem

import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.system.CfTimePickerDialog
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.designsystem.GallerySection
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises CfTimePickerDialog through the shared isolated gallery section. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class CfTimePickerDialogGuardsTest(
    case: LayoutCase,
) : PrimitiveGuard(case, GallerySection.TimePicker) {
    @Composable override fun GuardContent() {
        CfTimePickerDialog(
            rememberTimePickerState(initialHour = 22, initialMinute = 30, is24Hour = true),
            {},
            {},
            stringResource(R.string.settings_done),
            stringResource(R.string.settings_cancel),
            supporting = stringResource(R.string.wizard_local_time),
        )
    }

    @Test fun supportingAndCustomActionsAreStructuralSlots() {
        val showSupporting = mutableStateOf(true)
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(darkTheme = false, reduceMotion = true) {
                    CfTimePickerDialog(
                        rememberTimePickerState(initialHour = 22, initialMinute = 30, is24Hour = true),
                        {},
                        {},
                        stringResource(R.string.settings_done),
                        stringResource(R.string.settings_cancel),
                        supporting = if (showSupporting.value) stringResource(R.string.wizard_local_time) else null,
                        confirmTestTag = "custom.done",
                        dismissTestTag = "custom.cancel",
                    )
                }
            }
        }
        compose.onNodeWithTag("cf.time.supporting", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("custom.done").assertExists()
        compose.onNodeWithTag("custom.cancel").assertExists()
        compose.runOnIdle { showSupporting.value = false }
        compose.onNodeWithTag("cf.time.supporting", useUnmergedTree = true).assertDoesNotExist()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = LayoutMatrix.parameters(LayoutMatrix.phone)
    }
}

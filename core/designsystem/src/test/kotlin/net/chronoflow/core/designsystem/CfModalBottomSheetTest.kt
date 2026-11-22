@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.designsystem

import android.view.KeyEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfModalBottomSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun escapeFromTheSheetWindowCallsItsDismissRequest() = checkWindowKey(true, KeyEvent.KEYCODE_ESCAPE, 1)

    @Test fun busySheetDoesNotDismissOnEscape() = checkWindowKey(false, KeyEvent.KEYCODE_ESCAPE, 0)

    @Test fun enterDoesNotDismissTheSheet() = checkWindowKey(true, KeyEvent.KEYCODE_ENTER, 0)

    private fun checkWindowKey(
        enabled: Boolean,
        key: Int,
        expected: Int,
    ) {
        var dismissals = 0
        lateinit var content: View
        compose.setContent {
            ChronoFlowTheme(reduceMotion = true) {
                CfModalBottomSheet(
                    { dismissals++ },
                    properties =
                        androidx.compose.material3.ModalBottomSheetProperties(
                            shouldDismissOnBackPress = enabled,
                        ),
                ) {
                    content = LocalView.current
                    Box(Modifier.size(PlanUITokens.actionMinHeight.dp).testTag("sheet.focus"))
                }
            }
        }
        compose.runOnIdle {
            val window =
                generateSequence(content as View?) { it.parent as? View }
                    .filterIsInstance<androidx.compose.ui.window.DialogWindowProvider>()
                    .first()
                    .window
            window.callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key))
            window.callback.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key))
        }
        assertEquals(expected, dismissals)
    }

    @Test fun sheetWindowExportsItsOwnAndDescendantResourceIds() {
        lateinit var activityView: View
        lateinit var sheetView: View
        compose.setContent {
            val currentActivityView = LocalView.current
            SideEffect { activityView = currentActivityView }
            ChronoFlowTheme(reduceMotion = true) {
                Box(Modifier.semantics { testTagsAsResourceId = true }) {
                    CfModalBottomSheet({}, Modifier.testTag("trip.sheet")) {
                        val currentSheetView = LocalView.current
                        SideEffect { sheetView = currentSheetView }
                        Text(stringResource(android.R.string.ok), Modifier.testTag("test.sheet.child"))
                    }
                }
            }
        }
        val sheet = compose.onNodeWithTag("trip.sheet").fetchSemanticsNode()
        val child = compose.onNodeWithTag("test.sheet.child").fetchSemanticsNode()
        compose.runOnIdle {
            assertNotSame(activityView, sheetView)
            val provider = checkNotNull(sheetView.accessibilityNodeProvider)
            assertEquals("trip.sheet", provider.createAccessibilityNodeInfo(sheet.id)?.viewIdResourceName)
            assertEquals("test.sheet.child", provider.createAccessibilityNodeInfo(child.id)?.viewIdResourceName)
        }
    }
}

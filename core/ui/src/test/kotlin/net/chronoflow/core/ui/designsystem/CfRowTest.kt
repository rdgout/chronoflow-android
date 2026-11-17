@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.component.CfRow
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfRowTest : PrimitiveTestFixture() {
    @Test fun disabledRowHasButtonSemanticsAndDoesNotClick() {
        var calls = 0
        host { CfRow(stringResource(R.string.settings_saved), enabled = false, onClick = { calls++ }, testTag = "row") }
        compose
            .onNodeWithTag("row")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun lockAndChevronCanAppearTogether() {
        host { CfRow(stringResource(R.string.settings_saved), lock = true, onClick = {}, testTag = "row") }
        compose.onNodeWithTag("cf.row.lock", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("cf.row.chevron", useUnmergedTree = true).assertExists()
    }
}

package net.chronoflow.feature.settings.components

import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsPageWideTest(
    private val width: Int,
    private val expected: Int,
) : SettingsScreenTest(DeviceProfiles.pixel8.copy(widthDp = width)) {
    @Test fun columnUsesTheWideCapAndIsCentered() {
        show { SettingsPage(R.string.settings_account, {}, "page") { Text(stringResource(R.string.settings_account)) } }
        val page = compose.onNodeWithTag("page").fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("page.content").fetchSemanticsNode().boundsInRoot
        assertEquals(expected * profile.density, content.width, 1f)
        assertEquals(page.center.x, content.center.x, 1f)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "width={0}-column={1}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(840, 560), arrayOf(599, 567))
    }
}

package net.chronoflow.feature.launch.welcome

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.SafeInsetsDp

internal val board390 =
    DeviceProfile("board390", 390, 844, 3f, 1f, "gesture", SafeInsetsDp(0f, 0f, 0f, 0f), "Main board")
internal val welcomeTags =
    listOf(
        WelcomeTags.WORDMARK,
        WelcomeTags.EYEBROW,
        WelcomeTags.HEADLINE,
        WelcomeTags.COPY,
        WelcomeTags.PLAN,
        WelcomeTags.SIGN_IN,
    )

internal fun ComposeContentTestRule.bounds(tag: String): DpRect =
    onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

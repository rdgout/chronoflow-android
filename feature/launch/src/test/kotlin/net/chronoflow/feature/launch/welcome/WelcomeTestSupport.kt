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

internal class CountingWelcomeClock : net.chronoflow.feature.launch.model.LaunchClock {
    private val manual =
        net.chronoflow.feature.launch.model
            .ManualLaunchClock()
    var reads = 0
        private set
    val pendingSleeps get() = manual.pendingSleeps
    override val now: kotlin.time.Duration get() {
        reads++
        return manual.now
    }

    override suspend fun sleepUntil(deadline: kotlin.time.Duration) = manual.sleepUntil(deadline)

    fun advance(to: kotlin.time.Duration) = manual.advance(to)
}

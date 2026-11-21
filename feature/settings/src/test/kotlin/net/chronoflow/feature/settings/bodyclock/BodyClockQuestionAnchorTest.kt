package net.chronoflow.feature.settings.bodyclock

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BodyClockQuestionAnchorTest : SettingsScreenTest(DeviceProfiles.small) {
    @Test fun firstMissingShiftWorkAlignsTheQuestionWithTheViewportTop() {
        val state = bodyClockUi(
            readiness = """{"variant":"needs-answers","missing":[{"question":"shift_work"}]}""",
        ).copy(scroll = BodyClockScroll(63, BodyClockGroup.Health, "shiftWork"))
        var acknowledged: Long? = null
        show {
            BodyClockScreen(BodyClockScreenState(state, TextFieldState()), {
                if (it is BodyClockEvent.ScrollHandled) acknowledged = it.id
            })
        }
        val viewport = compose.onNodeWithTag("settings.bodyClock.screen.content").fetchSemanticsNode()
        val question = compose.onNodeWithTag("settings.bodyClock.shiftWork.question", true).fetchSemanticsNode()
        assertEquals(viewport.positionInRoot.y + PlanUITokens.space1 * profile.density, question.positionInRoot.y, 1f)
        assertEquals(63L, acknowledged)
    }
}

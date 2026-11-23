package net.chronoflow.app.guest

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.BoardCaptureCell
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class)
class GuestSignupCaptureTest(
    private val board: String,
    private val cell: BoardCaptureCell,
) {
    private val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(cell.profile, cell.language, cell.dark),
            ).around(compose)

    @Test fun captureGuestSignupBoard() {
        val fixture = GuestSignupScreenFixture(board, cell.profile, cell.language, cell.dark)
        Dispatchers.setMain(fixture.dispatcher)
        GuestSignupTestActivity.fixture = fixture
        val controller = Robolectric.buildActivity(GuestSignupTestActivity::class.java).setup()
        try {
            compose.waitUntil(2_000) {
                fixture.scheduler.runCurrent()
                compose.onNodeWithTag("guest-signup.name").isDisplayed()
            }
            BoardCapture.capture(compose, "${board}_${cell.suffix}")
        } finally {
            controller.pause().stop().destroy()
            runTest(fixture.dispatcher, timeout = 60.seconds) {
                fixture.vm
                    ?.viewModelScope
                    ?.coroutineContext
                    ?.get(kotlinx.coroutines.Job)
                    ?.cancelAndJoin()
                fixture.job.cancelAndJoin()
            }
            fixture.root.deleteRecursively()
            GuestSignupTestActivity.fixture = null
            Dispatchers.resetMain()
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun boards(): List<Array<Any>> =
            listOf("GuestSignup", "GuestSignupFailed", "GuestSignupRetry", "GuestSignupReview").flatMap { board ->
                BoardCaptureCell.standard.map { cell -> arrayOf<Any>(board, cell) }
            }
    }
}

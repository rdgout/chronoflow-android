package net.chronoflow.app.guest

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class)
class GuestSignupScreenTest {
    private val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun testGuestSignupReady() =
        withSheet("GuestSignup") {
            compose.onNodeWithTag("guest-signup.name").assertExists()
            compose.onNodeWithTag("guest-signup.password").assertExists()
            compose.onNodeWithTag("guest-signup.confirmation").assertExists()
            compose.onNodeWithTag("auth.banner").assertDoesNotExist()
        }

    @Test fun testGuestSignupFailed() =
        withSheet("GuestSignupFailed") {
            compose.onNodeWithTag("auth.banner").assertExists()
        }

    @Test fun testGuestSignupRetry() =
        withSheet("GuestSignupRetry") {
            compose.onNodeWithTag("auth.banner").assertExists()
        }

    @Test fun testGuestSignupReview() =
        withSheet("GuestSignupReview") {
            compose.onNodeWithTag("auth.banner.action").performClick()
            scheduler.runCurrent()
            scheduler.advanceTimeBy(1_500)
            scheduler.runCurrent()
            compose.waitUntil(1_000) {
                scheduler.runCurrent()
                compose.onNodeWithTag("wizard.ready.continue").isDisplayed()
            }
            compose.onNodeWithTag("wizard.ready.continue").assertIsEnabled()
            compose.onNodeWithTag("guest-signup.name").assertDoesNotExist()
        }

    @Test fun testSignInInstead() =
        withSheet("GuestSignup") {
            compose.onNodeWithTag("guest-signup.sign-in").performScrollTo().performClick()
            compose.waitUntil(1_000) {
                scheduler.runCurrent()
                compose.onNodeWithTag("sign-in.email").isDisplayed()
            }
            compose.onNodeWithTag("sign-in.email").assertExists()
            runTest(dispatcher, timeout = 60.seconds) { assertTrue(store.recover()?.state?.signingIn == true) }
        }

    private fun withSheet(
        board: String,
        block: GuestSignupScreenFixture.() -> Unit,
    ) {
        val fixture = GuestSignupScreenFixture(board, DeviceProfiles.pixel8)
        Dispatchers.setMain(fixture.dispatcher)
        GuestSignupTestActivity.fixture = fixture
        val controller = Robolectric.buildActivity(GuestSignupTestActivity::class.java).setup()
        try {
            compose.waitUntil(2_000) {
                fixture.scheduler.runCurrent()
                compose.onNodeWithTag("guest-signup.name").isDisplayed()
            }
            fixture.block()
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
}

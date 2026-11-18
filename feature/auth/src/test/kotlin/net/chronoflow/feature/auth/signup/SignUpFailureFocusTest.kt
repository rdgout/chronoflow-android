package net.chronoflow.feature.auth.signup

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignUpFailureFocusTest(
    profile: DeviceProfile,
) : SignUpTestFixture(profile) {
    @Test fun testMixedFailureFocusesAndRevealsTheConfirmationMessage() {
        host()
        mixed()
        compose.onNodeWithTag("sign-up.confirmation").assertIsFocused()
        inside("sign-up.confirmation.group")
    }

    @Test fun bannerOnlyClearsFocusAndRevealsTheBanner() {
        host()
        compose.runOnIdle { model.form.receive(DataFailure.Server(503)) }
        flush()
        compose.onNodeWithTag("sign-up.name").assertIsNotFocused()
        compose.onNodeWithTag("sign-up.email").assertIsNotFocused()
        compose.onNodeWithTag("sign-up.password").assertIsNotFocused()
        compose.onNodeWithTag("sign-up.confirmation").assertIsNotFocused()
        inside("auth.banner")
    }

    private fun inside(tag: String) {
        val bounds = compose.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("sign-up.scroll").getUnclippedBoundsInRoot()
        assertTrue(bounds.top >= viewport.top && bounds.bottom <= viewport.bottom)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = listOf(DeviceProfiles.pixel8, DeviceProfiles.largeFont)
    }
}

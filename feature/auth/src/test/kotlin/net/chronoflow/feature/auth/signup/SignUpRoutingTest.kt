package net.chronoflow.feature.auth.signup

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignUpRoutingTest : SignUpTestFixture() {
    @Test fun drawnBackReturnsToSignInOnce() = back("auth.back")

    @Test fun footerReturnsToSignInOnce() = back("sign-up.back")

    private fun back(tag: String) {
        host()
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        assertEquals(Route.SignIn, navigator.state.value.route)
        assertEquals(2L, navigator.state.value.revision)
    }

    @Test fun systemBackReturnsToSignInOnce() {
        host()
        Espresso.pressBack()
        assertEquals(Route.SignIn, navigator.state.value.route)
        assertEquals(2L, navigator.state.value.revision)
    }
}

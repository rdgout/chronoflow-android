package net.chronoflow.feature.auth.signin

import android.view.View
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.AuthTestFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignInScreenTest : SignInTestFixture() {
    @Test fun unchangedPasswordPreservesServerFeedbackUntilTheValueChanges() {
        val fixture = AuthTestFixture(dispatcher)
        host(fixture)
        compose.onNodeWithTag("sign-in.password").performTextInput("same")
        compose.runOnIdle {
            fixture.model.form.receive(
                DataFailure.Validation(
                    mapOf(
                        "password" to FieldMessage("Please fill this in."),
                        "device_id" to FieldMessage("Please fill this in."),
                    ),
                    422,
                ),
            )
        }
        flush()
        compose.onNodeWithTag("sign-in.password").performTextReplacement("same")
        flush()
        assertEquals(AuthBannerKind.Server, fixture.model.form.state.value.banner)
        assertNotNull(fixture.model.form.state.value.fields["password"])
        compose.onNodeWithTag("sign-in.password").performTextReplacement("different")
        flush()
        assertNull(fixture.model.form.state.value.banner)
        assertNull(fixture.model.form.state.value.fields["password"])
        compose.onNodeWithTag("sign-in.submit").performScrollTo().performClick()
        flush()
        assertNotNull(fixture.model.form.state.value.failure)
        assertEquals(
            0,
            compose
                .onNodeWithTag(
                    "sign-in.password",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .length,
        )
    }

    @Test fun imeChainMovesEmailToPasswordToSubmit() {
        val fixture = AuthTestFixture(dispatcher)
        host(fixture)
        compose.onNodeWithTag("sign-in.email").performTextInput("demo@example.com")
        compose.onNodeWithTag("sign-in.email").performImeAction()
        compose
            .onNodeWithTag("sign-in.password")
            .assertIsFocused()
            .performTextInput("Password123!")
        compose.onNodeWithTag("sign-in.password").performImeAction()
        flush()
        assertEquals(1, fixture.session.accepted.size)
    }

    @Test fun busyDisablesEveryControlAndIgnoresASecondGo() {
        val gate = CompletableDeferred<String>()
        val fixture = AuthTestFixture(dispatcher, capability = true, identity = { gate.await() })
        host(fixture)
        compose.onNodeWithTag("sign-in.email").performTextInput("demo@example.com")
        compose.onNodeWithTag("sign-in.password").performTextInput("Password123!")
        compose.onNodeWithTag("sign-in.password").performImeAction()
        flush()
        compose.onNodeWithTag("auth.back").assertIsNotEnabled()
        compose.onNodeWithTag("sign-in.passkey").assertIsNotEnabled()
        compose.onNodeWithTag("sign-in.email").assertIsNotEnabled()
        compose.onNodeWithTag("sign-in.password").assertIsNotEnabled()
        compose.onNodeWithTag("sign-in.password").performSemanticsAction(SemanticsActions.OnImeAction) { it() }
        compose.onNodeWithTag("sign-in.submit").assertIsNotEnabled()
        compose.onNodeWithTag("sign-in.forgot").assertIsNotEnabled()
        compose.onNodeWithTag("sign-in.signup").assertIsNotEnabled()
        compose.onNodeWithTag("sign-in.guest").assertIsNotEnabled()
        compose.runOnIdle { gate.complete(AUTH_DEVICE_ID) }
        flush()
        assertEquals(1, fixture.session.accepted.size)
    }

    @Test fun fieldFailureFocusesTheFirstInvalidField() {
        val fixture = AuthTestFixture(dispatcher)
        host(fixture)
        compose.onNodeWithTag("sign-in.submit").performScrollTo().performClick()
        flush()
        compose.onNodeWithTag("sign-in.email").assertIsFocused()
        compose.onNodeWithTag("sign-in.password").assertIsNotFocused()
    }

    @Test fun bannerFailureLeavesNoFocusAndShowsTheBanner() {
        val fixture = AuthTestFixture(dispatcher)
        host(fixture)
        compose.onNodeWithTag("sign-in.email").performTextInput("demo@example.com")
        compose.onNodeWithTag("sign-in.password").performTextInput("wrong")
        compose.onNodeWithTag("sign-in.password").performImeAction()
        flush()
        compose.onNodeWithTag("sign-in.email").assertIsNotFocused()
        compose.onNodeWithTag("sign-in.password").assertIsNotFocused()
        val banner = compose.onNodeWithTag("auth.banner").fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("sign-in.scroll").fetchSemanticsNode().boundsInRoot
        assertTrue(banner.top >= viewport.top && banner.bottom <= viewport.bottom)
    }

    @Test fun autofillContentTypes() {
        host(AuthTestFixture(dispatcher))
        val emailType =
            compose
                .onNodeWithTag(
                    "sign-in.email",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentType]
        val passwordType =
            compose
                .onNodeWithTag(
                    "sign-in.password",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.ContentType]
        assertEquals(setOf(View.AUTOFILL_HINT_USERNAME, View.AUTOFILL_HINT_EMAIL_ADDRESS), emailType.hints())
        assertEquals(setOf(View.AUTOFILL_HINT_PASSWORD), passwordType.hints())
    }

    @Test fun passkeyControlsAreAbsentWhileTheCapabilityIsOff() {
        val displayed = host(AuthTestFixture(dispatcher))
        compose.onNodeWithTag("sign-in.passkey").assertDoesNotExist()
        compose.onNodeWithTag("sign-in.divider").assertDoesNotExist()
        val available = AuthTestFixture(dispatcher, capability = true)
        available.model.form.receive(DataFailure.Server(null))
        compose.runOnIdle { displayed.value = vm(available) }
        flush()
        val banner = compose.onNodeWithTag("auth.banner").fetchSemanticsNode().boundsInRoot
        val passkey = compose.onNodeWithTag("sign-in.passkey").fetchSemanticsNode().boundsInRoot
        val divider = compose.onNodeWithTag("sign-in.divider", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val email = compose.onNodeWithTag("sign-in.email").fetchSemanticsNode().boundsInRoot
        assertTrue(banner.top < passkey.top && passkey.top < divider.top && divider.top < email.top)
        compose.onNodeWithTag("sign-in.passkey").performScrollTo().performClick()
        flush()
        assertEquals(AuthBannerKind.Server, available.model.form.state.value.banner)
        assertTrue(available.session.accepted.isEmpty())
    }

    @Test fun traversalFollowsTheBoardOrder() {
        val fixture = AuthTestFixture(dispatcher, capability = true)
        fixture.model.form.receive(DataFailure.Server(null))
        host(fixture)
        val back =
            compose
                .onNodeWithTag("auth.back")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val brand =
            compose
                .onNodeWithTag("auth.brand")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val heading = compose.onNodeWithTag("auth.heading").fetchSemanticsNode()
        val subtitle =
            compose
                .onNodeWithTag("auth.subtitle")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val banner =
            compose
                .onNodeWithTag("auth.banner")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val passkey =
            compose
                .onNodeWithTag("sign-in.passkey")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val email =
            compose
                .onNodeWithTag("sign-in.email")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val password =
            compose
                .onNodeWithTag("sign-in.password")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val submit =
            compose
                .onNodeWithTag("sign-in.submit")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val forgot =
            compose
                .onNodeWithTag("sign-in.forgot")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val signup =
            compose
                .onNodeWithTag("sign-in.signup")
                .fetchSemanticsNode()
                .boundsInRoot.top
        val footer =
            compose
                .onNodeWithTag("sign-in.guest")
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue(back <= brand && brand < heading.boundsInRoot.top && heading.boundsInRoot.top < subtitle)
        assertTrue(subtitle < banner && banner < passkey && passkey < email && email < password)
        assertTrue(password < submit && submit < forgot && forgot <= signup && signup < footer)
        assertTrue(heading.config.contains(SemanticsProperties.Heading))
        val divider = compose.onNodeWithTag("sign-in.divider", useUnmergedTree = true).fetchSemanticsNode().config
        assertFalse(divider.contains(SemanticsProperties.Text))
        assertFalse(divider.contains(SemanticsProperties.ContentDescription))
    }
}

// Combined ContentType values use identity equality; the platform hints are the autofill contract.
private fun ContentType.hints(): Set<*> =
    (
        Class
            .forName("androidx.compose.ui.autofill.ContentType_androidKt")
            .getMethod("getContentHints", ContentType::class.java)
            .invoke(null, this) as Array<*>
    ).toSet()

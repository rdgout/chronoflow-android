package net.chronoflow.feature.auth.twofactor

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.model.SignInModel
import net.chronoflow.feature.auth.model.TwoFactorModel
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TwoFactorScreenTest : TwoFactorTestFixture() {
    @Test fun bannerIsFirstInCardAndAnnouncedOnce() {
        host()
        failNext(DataFailure.Throttled(60.0, 429))
        compose.onNodeWithTag("two-factor.submit").performScrollTo().performClick()
        flush()
        val banner = compose.onNodeWithTag("auth.banner").fetchSemanticsNode().boundsInRoot
        val field = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertTrue(banner.top < field.top)
        compose.onAllNodesWithTag("auth.announcement").assertCountEquals(1)
        compose
            .onNodeWithTag(
                "auth.announcement",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
        assertNull(model.form.takeAnnouncement { it.key })
    }

    @Test fun headingAndPaneTitle() {
        host()
        compose.onNodeWithTag("auth.heading").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithTag("two-factor.scroll").assertExists()
        // The pane's presence is structural; its translated value is covered by resource tests.
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.PaneTitle)).assertExists()
    }

    @Test fun imeGoSubmitsOnceAndNeverAutoSubmits() {
        host()
        compose.onNodeWithTag(tag).performTextInput("123456")
        flush()
        assertTrue(session.accepted.isEmpty())
        assertFalse(model.form.busy)
        compose.onNodeWithTag(tag).performImeAction()
        flush()
        assertEquals(1, session.accepted.size)
        assertEquals(1, routes.accepted.size)
    }

    @Test fun missingModelReturnsToSignIn() {
        signIn =
            SignInModel(
                source,
                session,
                { AUTH_DEVICE_ID },
                { "test" },
                accepted = routes::acceptedSession,
                onTwoFactor = routes::twoFactor,
            )
        vm = TwoFactorViewModel(signIn, routes, scope)
        compose.setContent { Content() }
        flush()
        assertEquals(1, routes.backToSignIn)
    }
}

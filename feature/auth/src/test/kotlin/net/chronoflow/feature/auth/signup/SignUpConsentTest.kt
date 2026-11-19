package net.chronoflow.feature.auth.signup

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.AuthPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignUpConsentTest(
    private val language: String,
) : SignUpTestFixture() {
    @Test fun twoPolicyAnnotationsOpenOnlyTheirOwnUrls() {
        RuntimeEnvironment.setQualifiers(language)
        host()
        val text =
            compose
                .onNodeWithTag(
                    "sign-up.consent",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .single()
        val links = text.getLinkAnnotations(0, text.length)
        assertEquals(2, links.size)
        val terms = links[0].item as androidx.compose.ui.text.LinkAnnotation.Url
        val privacy = links[1].item as androidx.compose.ui.text.LinkAnnotation.Url
        assertEquals(AuthPolicy.Terms.url, terms.url)
        assertEquals(AuthPolicy.Privacy.url, privacy.url)
        assertTrue(links[0].start < links[0].end && links[0].end < links[1].start && links[1].start < links[1].end)
        terms.linkInteractionListener!!.onClick(terms)
        privacy.linkInteractionListener!!.onClick(privacy)
        assertEquals(listOf(AuthPolicy.Terms.url, AuthPolicy.Privacy.url), opened)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = listOf("en", "nl")
    }
}

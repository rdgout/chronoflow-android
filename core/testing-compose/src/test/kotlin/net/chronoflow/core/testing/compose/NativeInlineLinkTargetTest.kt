package net.chronoflow.core.testing.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeInlineLinkTargetTest {
    private val compose = createComposeRule()
    private val profile = DeviceProfiles.small

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun nativeInlineLinkHasATextSizedLayoutAndAnExpandedTouchTarget() {
        compose.setContent {
            ProfileHost(profile) {
                Box(Modifier.fillMaxSize()) {
                    Text(
                        buildAnnotatedString {
                            withLink(LinkAnnotation.Url("https://chronoflow.net/terms")) { append("Policy") }
                        },
                        fontSize = 12.sp,
                    )
                }
            }
        }
        val link =
            compose
                .onNode(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.LinkTestMarker),
                ).fetchSemanticsNode()
        val min = 48 * profile.density
        assertTrue(link.layoutInfo.height < min)
        assertTrue(
            "native target: ${link.touchBoundsInRoot}",
            link.touchBoundsInRoot.width >= min && link.touchBoundsInRoot.height >= min,
        )
        Guards.assertTouchTargets(compose, profile)
    }

    @Test fun reducedNativeLinkTouchTargetFails() {
        compose.setContent {
            val inherited = LocalViewConfiguration.current
            CompositionLocalProvider(
                LocalViewConfiguration provides
                    object : ViewConfiguration by inherited {
                        override val minimumTouchTargetSize = DpSize(20.dp, 20.dp)
                    },
            ) {
                Text(
                    buildAnnotatedString {
                        withLink(LinkAnnotation.Url("https://chronoflow.net/terms")) { append("Policy") }
                    },
                    fontSize = 12.sp,
                )
            }
        }
        assertThrows(AssertionError::class.java) { Guards.assertTouchTargets(compose, profile) }
    }

    @Test fun smallOrdinaryControlsStillFailDespiteTouchExpansion() {
        compose.setContent { Box(Modifier.size(40.dp).clickable {}) }
        assertThrows(AssertionError::class.java) { Guards.assertTouchTargets(compose, profile) }
    }

    @Test fun wrappedNativeLinksUseTheirTextRegionsForOverlap() {
        compose.setContent {
            Text(
                buildAnnotatedString {
                    append("Read ")
                    withLink(LinkAnnotation.Url("https://chronoflow.net/terms")) { append("terms") }
                    append(" and ")
                    withLink(LinkAnnotation.Url("https://chronoflow.net/privacy")) {
                        append("privacy policy for this application")
                    }
                },
                Modifier.width(160.dp),
                fontSize = 12.sp,
            )
        }
        Guards.assertNoInteractiveOverlap(compose)
    }

    @Test fun overlappingNativeLinkRangesFail() {
        compose.setContent {
            Text(
                buildAnnotatedString {
                    append("Overlapping native links")
                    addLink(LinkAnnotation.Url("https://chronoflow.net/terms"), 0, 12)
                    addLink(LinkAnnotation.Url("https://chronoflow.net/privacy"), 5, 19)
                },
                fontSize = 12.sp,
            )
        }
        assertThrows(AssertionError::class.java) { Guards.assertNoInteractiveOverlap(compose) }
    }
}

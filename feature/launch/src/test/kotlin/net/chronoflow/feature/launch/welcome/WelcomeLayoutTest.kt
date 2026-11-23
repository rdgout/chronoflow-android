package net.chronoflow.feature.launch.welcome

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.MoonAnchorStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WelcomeLayoutTest(
    private val profile: DeviceProfile,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun boardCompositionUsesTheViewportAndScrollsWhenNeeded() {
        val store = MoonAnchorStore()
        compose.setContent {
            ProfileHost(profile) { ChronoFlowTheme(darkTheme = false) { WelcomeScreen(null, store, {}, {}) } }
        }
        val wordmark = compose.bounds(WelcomeTags.WORDMARK)
        val headline = compose.bounds(WelcomeTags.HEADLINE)
        val copy = compose.bounds(WelcomeTags.COPY)
        val plan = compose.bounds(WelcomeTags.PLAN)
        val signIn = compose.bounds(WelcomeTags.SIGN_IN)
        when (profile.name) {
            "board390" -> {
                val anchor = requireNotNull(store.frame)
                assertEquals(24.0, anchor.x, 0.5)
                assertEquals(64.0, anchor.y, 0.5)
                assertEquals(28.0, anchor.width, 0.5)
                assertEquals(28.0, anchor.height, 0.5)
                assertEquals(64f, wordmark.top.value, 0.5f)
                assertEquals(92f, wordmark.bottom.value, 0.5f)
                val eyebrow = compose.bounds(WelcomeTags.EYEBROW)
                assertEquals(164f, eyebrow.top.value, 0.5f)
                assertEquals(180f, eyebrow.bottom.value, 0.5f)
                assertEquals(196f, headline.top.value, 0.5f)
                val layouts = mutableListOf<TextLayoutResult>()
                compose
                    .onNodeWithTag(
                        WelcomeTags.HEADLINE,
                    ).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                val layout = layouts.single()
                assertEquals(
                    "headline breaks: " +
                        (0 until layout.lineCount).map {
                            layout.layoutInput.text.text
                                .substring(layout.getLineStart(it), layout.getLineEnd(it))
                        },
                    3,
                    layout.lineCount,
                )
                assertEquals(144f, (headline.bottom.value - headline.top.value), 0.5f)
                assertEquals(688f, plan.top.value, 0.5f)
                assertEquals(740f, plan.bottom.value, 0.5f)
                assertEquals(752f, signIn.top.value, 0.5f)
                assertEquals(804f, signIn.bottom.value, 0.5f)
                assertEquals(24f, plan.left.value, 0.5f)
                assertEquals(342f, (plan.right.value - plan.left.value), 0.5f)
            }

            "foldInner" -> {
                assertEquals(560f, (copy.right.value - copy.left.value), 0.5f)
                assertEquals(profile.widthDp / 2f, ((copy.left.value + copy.right.value) / 2), 0.5f)
                assertEquals(
                    profile.widthDp - profile.insets.left - profile.insets.right - 48,
                    (
                        plan.right.value -
                            plan.left.value
                    ),
                    0.5f,
                )
            }

            "landscapeSideNav" -> {
                assertEquals(
                    headline.bottom.value + 24 + (copy.bottom.value - copy.top.value) + 12 + 12,
                    plan.top.value,
                    0.5f,
                )
                compose.onNodeWithTag(WelcomeTags.SIGN_IN).performScrollTo()
                val visible = compose.onNodeWithTag(WelcomeTags.SIGN_IN).fetchSemanticsNode().boundsInRoot
                assertTrue(visible.height > 0)
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun profiles(): List<Array<Any>> =
            listOf(board390, DeviceProfiles.foldInner, DeviceProfiles.landscapeSideNav).map { arrayOf<Any>(it) }
    }
}

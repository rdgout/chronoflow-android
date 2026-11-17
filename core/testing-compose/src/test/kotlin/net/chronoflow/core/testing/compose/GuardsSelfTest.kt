package net.chronoflow.core.testing.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import assertk.assertFailure
import assertk.assertions.isInstanceOf
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.LocalSafeInsets
import net.chronoflow.core.ui.semantics.ellipsizable
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private enum class Guard { TextFits, TouchTargets, SafeArea, Overlap, Headings, Ink }

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class GuardsSelfTest {
    private val profile = DeviceProfiles.small
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain.outerRule(ProfileRule(profile, AppLanguage.En)).around(composeRule)

    private val headings = setOf("guard.good.title")

    @Composable
    private fun GoodLayout(
        outside: @Composable () -> Unit = {},
        inside: @Composable () -> Unit = {},
    ) {
        ProfileHost(profile) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                Column(Modifier.windowInsetsPadding(LocalSafeInsets.current)) {
                    Text(
                        "Title",
                        fontSize = 32.sp,
                        modifier = Modifier.testTag("guard.good.title").semantics { heading() },
                    )
                    Box(
                        Modifier.testTag("guard.good.button").minimumInteractiveComponentSize().clickable {},
                    ) { Text("Go") }
                    Text("Short text")
                    Box(Modifier.fillMaxWidth().height(40.dp).background(Color.Black))
                    inside()
                }
                outside()
            }
        }
    }

    private fun run(
        guard: Guard,
        exempt: Set<String> = emptySet(),
        headingTags: Array<String> = arrayOf("guard.good.title"),
    ) = when (guard) {
        Guard.TextFits -> Guards.assertTextFits(composeRule, profile)
        Guard.TouchTargets -> Guards.assertTouchTargets(composeRule, profile, exemptTags = exempt)
        Guard.SafeArea -> Guards.assertInteractiveInSafeArea(composeRule, profile, exempt)
        Guard.Overlap -> Guards.assertNoInteractiveOverlap(composeRule, exempt)
        Guard.Headings -> Guards.assertHeadings(composeRule, *headingTags)
        Guard.Ink -> Guards.assertInk(composeRule, profile)
    }

    private fun assertOnlyFails(
        failing: Guard,
        exempt: Set<String> = emptySet(),
        headingTags: Array<String> = arrayOf("guard.good.title"),
    ) {
        for (guard in Guard.entries) {
            if (guard == failing) {
                assertFailure { run(guard, exempt, headingTags) }.isInstanceOf(AssertionError::class)
            } else {
                run(guard, exempt, headingTags)
            }
        }
    }

    @Test
    fun goodLayoutPassesEveryGuard() {
        composeRule.setContent { GoodLayout() }
        Guards.assertAll(composeRule, profile, headings)
    }

    @Test
    fun textWiderThanTheWindowFailsOnlyTextFits() {
        composeRule.setContent { GoodLayout(inside = { Text("Wide", modifier = Modifier.requiredWidth(600.dp)) }) }
        assertOnlyFails(Guard.TextFits)
    }

    @Test
    fun horizontallyScrollingChipRowPasses() {
        composeRule.setContent {
            GoodLayout(
                inside = {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        repeat(8) { Box(Modifier.minimumInteractiveComponentSize().clickable {}) { Text("Chip $it") } }
                    }
                },
            )
        }
        Guards.assertTextFits(composeRule, profile)
        Guards.assertInteractiveInSafeArea(composeRule, profile)
    }

    @Test
    fun scrollingContainerWiderThanTheWindowStillFails() {
        composeRule.setContent {
            GoodLayout(
                inside = {
                    Box(Modifier.requiredWidth(900.dp)) {
                        Row(Modifier.width(900.dp).horizontalScroll(rememberScrollState())) { Text("Far chip") }
                    }
                },
            )
        }
        assertOnlyFails(Guard.TextFits)
    }

    @Test
    fun ellipsisFailsUnlessMarkedEllipsizable() {
        composeRule.setContent {
            GoodLayout(
                inside = { Text("Long text ".repeat(20), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
        assertOnlyFails(Guard.TextFits)
    }

    @Test
    fun ellipsizableTextPasses() {
        composeRule.setContent {
            GoodLayout(
                inside = {
                    Text(
                        "Long text ".repeat(20),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.ellipsizable(),
                    )
                },
            )
        }
        Guards.assertAll(composeRule, profile, headings)
    }

    @Test
    fun smallTouchTargetFailsOnlyTouchTargets() {
        composeRule.setContent { GoodLayout(inside = { Box(Modifier.size(40.dp).clickable {}) }) }
        assertOnlyFails(Guard.TouchTargets)
    }

    @Test
    fun buttonUnderTheTopInsetFailsOnlySafeArea() {
        composeRule.setContent {
            GoodLayout(
                outside = { Box(Modifier.size(48.dp).clickable {}) },
            )
        }
        assertOnlyFails(Guard.SafeArea)
    }

    @Test
    fun overlappingButtonsFailOnlyOverlapUnlessExempt() {
        composeRule.setContent {
            GoodLayout(
                inside = {
                    Box(Modifier.testTag("overlap.box")) {
                        Box(Modifier.size(48.dp).clickable {})
                        Box(Modifier.offset(y = 10.dp).size(48.dp).clickable {})
                    }
                },
            )
        }
        assertOnlyFails(Guard.Overlap)
        Guards.assertNoInteractiveOverlap(composeRule, setOf("overlap.box"))
    }

    @Test
    fun missingHeadingFailsOnlyHeadings() {
        composeRule.setContent { GoodLayout(inside = { Text("Bad", Modifier.testTag("guard.bad.title")) }) }
        assertOnlyFails(Guard.Headings, headingTags = arrayOf("guard.good.title", "guard.bad.title"))
    }

    @Test
    fun invisibleLayoutFailsOnlyInk() {
        composeRule.setContent {
            Box(Modifier.fillMaxSize().background(Color.White)) { Box(Modifier.alpha(0f)) { GoodLayout() } }
        }
        assertOnlyFails(Guard.Ink)
    }
}

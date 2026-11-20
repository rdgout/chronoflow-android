package net.chronoflow.feature.wizard

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.wizard.model.MemberWizardModel
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.navigationPath
import net.chronoflow.core.wizard.model.progress
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.debug.WizardMemoryDraftStore
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardErrorScrollTest(
    private val case: LayoutCase,
    private val home: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case)).around(compose)

    @Test fun errorsScrollAboveContinue() {
        lateinit var initial: WizardUiState
        runTest(timeout = 60.seconds) {
            var draft =
                WizardBoardCaptureSeed.draft(
                    "WizardOutboundScrollErrors",
                    Locale.forLanguageTag(case.language.tag),
                )
            if (home) {
                draft =
                    draft.copy(
                        form =
                            draft.form.copy(
                                legs =
                                    draft.form.legs +
                                        draft.form.legs.map {
                                            it.copy(
                                                id = UUID.randomUUID().toString(),
                                            )
                                        },
                                stays =
                                    draft.form.stays + draft.form.stays,
                                end = draft.form.end.copy(kind = "return_legs", returnFromLeg = 5),
                            ),
                        state = draft.state.copy(step = MemberWizardStep.returnFlight, returnRouteConfirmed = true),
                        fieldErrors =
                            draft.fieldErrors +
                                mapOf(
                                    "legs.5.arriveLocal" to draft.fieldErrors.getValue("legs.0.arriveLocal"),
                                    "legs.9.departLocal" to draft.fieldErrors.getValue("legs.4.departLocal"),
                                ),
                    )
            }
            val model =
                MemberWizardModel(draft, WizardMemoryDraftStore(), backgroundScope, { WizardBoardCaptureSeed.now })
            try {
                initial =
                    WizardUiState.from(
                        TripWizardUiState.Active(model.snapshots.value, model.progress, model.navigationPath),
                        now = WizardBoardCaptureSeed.now,
                    )
            } finally {
                model.close()
            }
        }
        val state = mutableStateOf(initial)
        compose.setContent { WizardTestHost(case.profile) { WizardScreen(state.value, {}) } }
        val first = if (home) 5 else 0
        val last = first + 4
        val id = if (home) "return-flight" else "outbound"
        compose.onNodeWithTag("wizard.scroll").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("wizard.scroll").getUnclippedBoundsInRoot()
        val footer = compose.onNodeWithTag("wizard.$id.footer").getUnclippedBoundsInRoot()
        val bottom = minOf(viewport.bottom, footer.top)
        val firstBefore = compose.onNodeWithTag("wizard.$first.arrival.error").getUnclippedBoundsInRoot()
        val later = compose.onNodeWithTag("wizard.$last.departure.error").getUnclippedBoundsInRoot()
        val lastCard = compose.onNodeWithTag("wizard.flight.$last").getUnclippedBoundsInRoot()
        assertTrue("first error must start offscreen: $firstBefore / $viewport", firstBefore.bottom < viewport.top)
        assertTrue(
            "later error must be visible: $later / $viewport",
            later.top >= viewport.top - .5.dp && later.bottom <= bottom + .5.dp,
        )
        assertTrue("last card must clear the footer: $lastCard / $footer", lastCard.bottom <= footer.top + .5.dp)
        compose.runOnIdle { state.value = initial.copy(times = initial.times!!.copy(continueFailures = 1)) }
        compose.waitForIdle()
        val returned = compose.onNodeWithTag("wizard.$first.arrival.error").getUnclippedBoundsInRoot()
        assertTrue(
            "first error must be fully revealed: $returned / $viewport",
            returned.top >= viewport.top - .5.dp && returned.bottom <= bottom + .5.dp,
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-home={1}")
        fun cases(): List<Array<Any>> {
            val nl =
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.largeFont,
                    DeviceProfiles.landscapeSideNav,
                ).flatMap { p ->
                    listOf(false, true).map { arrayOf<Any>(LayoutCase(p, AppLanguage.Nl), it) }
                }
            return (
                nl +
                    listOf(
                        false,
                        true,
                    ).map { arrayOf<Any>(LayoutCase(DeviceProfiles.small, AppLanguage.En), it) }
            ).also {
                check(it.size == 12)
            }
        }
    }
}

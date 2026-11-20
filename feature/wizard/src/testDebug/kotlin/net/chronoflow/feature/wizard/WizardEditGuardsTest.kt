package net.chronoflow.feature.wizard

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.junit.measureClassWarmup
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardEditGuardsTest(
    private val case: LayoutCase,
    private val board: String,
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(case, dark)).around(compose)

    @Test fun screenFitsTheProfileAndSafeArea() {
        val state = completionBoard(board, Locale.forLanguageTag(case.language.tag))
        lateinit var formatter: CopyLineFormatter
        runTest(timeout = 60.seconds) { formatter = wizardCopyFormatter() }
        compose.setContent {
            ProfileHost(case.profile) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = true) { WizardScreen(state, formatter, {}) }
            }
        }
        Guards.assertTextFits(compose, case.profile)
        Guards.assertTouchTargets(compose, case.profile)
        Guards.assertInteractiveInSafeArea(compose, case.profile, rootTag = "wizard.window")
        Guards.assertNoInteractiveOverlap(compose, rootTag = "wizard.window")
        Guards.assertHeadings(compose, "wizard.title")
        if (state.discardDialogVisible) {
            compose.onNodeWithTag("cf.dialog.confirm").assertExists()
            compose.onNodeWithTag("cf.dialog.dismiss").assertExists()
        }
        if (case.language == AppLanguage.Nl && board in setOf("WizardEditReturnNoCredits", "WizardEditChanged")) {
            when (case.profile.name) {
                "small" -> testSEEditFooterAtXXLarge()
                "landscapeSideNav" -> testDuoEditFooterAtXXLarge()
                else -> assertEditFooterContained()
            }
        }
    }

    private fun testSEEditFooterAtXXLarge() = assertEditFooterContained()

    private fun testDuoEditFooterAtXXLarge() = assertEditFooterContained()

    private fun assertEditFooterContained() {
        val footer = compose.onNodeWithTag("wizard.ready.footer").getUnclippedBoundsInRoot()
        val tolerance = 1f / case.profile.density
        assertTrue(footer.top.value >= case.profile.insets.top - tolerance)
        assertTrue(footer.bottom.value <= case.profile.heightDp - case.profile.insets.bottom + tolerance)
        if (board == "WizardEditReturnNoCredits") {
            val close = compose.onNodeWithTag("wizard.ready.no-credits").getUnclippedBoundsInRoot()
            assertTrue(close.top >= footer.top && close.bottom <= footer.bottom)
        }
    }

    companion object {
        private val fixtureReady by lazy {
            measureClassWarmup(WizardEditGuardsTest::class.java, 36, "wizard-copy-model") {
                runTest(timeout = 60.seconds) {
                    boardState("WizardEdit", Locale.ENGLISH)
                    wizardCopyFormatter()
                }
            }
        }

        @JvmStatic @BeforeClass
        fun warmFixtureOncePerJvm() {
            fixtureReady
        }

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-dark={2}")
        fun cases(): List<Array<Any>> {
            val states =
                listOf(
                    "Edit",
                    "EditChanged",
                    "EditNoReturn",
                    "EditReturn",
                    "EditReturnNoCredits",
                    "EditWeb",
                    "ReturnWeb",
                    "CheckingEdit",
                    "CheckOfflineEdit",
                    "CheckServerEdit",
                    "ExcludedEdit",
                    "DiscardEdit",
                    "Duplicate",
                )
            val profiles =
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.largeFont,
                )
            val main =
                profiles.flatMap { profile ->
                    listOf(AppLanguage.En, AppLanguage.Nl).flatMap { language ->
                        listOf(false, true).flatMap { dark ->
                            states.map {
                                val stress =
                                    profile.name == "small" && language == AppLanguage.Nl &&
                                        it in setOf("EditReturnNoCredits", "EditChanged")
                                val effective =
                                    if (stress) {
                                        profile.copy(fontScale = DeviceProfiles.largeFont.fontScale)
                                    } else {
                                        profile
                                    }
                                arrayOf<Any>(LayoutCase(effective, language), "Wizard$it", dark)
                            }
                        }
                    }
                }
            val failures =
                profiles.flatMap { profile ->
                    listOf(AppLanguage.En, AppLanguage.Nl).flatMap { language ->
                        listOf("EditSaveOffline", "EditSaveServer").map {
                            arrayOf<Any>(LayoutCase(profile, language), "Wizard$it", false)
                        }
                    }
                }
            val side =
                listOf(false, true).flatMap { dark ->
                    listOf("EditReturnNoCredits", "EditChanged").map {
                        arrayOf<Any>(
                            LayoutCase(
                                DeviceProfiles.landscapeSideNav.copy(fontScale = DeviceProfiles.largeFont.fontScale),
                                AppLanguage.Nl,
                            ),
                            "Wizard$it",
                            dark,
                        )
                    }
                }
            return (main + failures + side).also { check(it.size == 228) }
        }
    }
}

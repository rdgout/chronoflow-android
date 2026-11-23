package net.chronoflow.core.ui.plan

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Capture
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ItemDetailCaptureTest(
    private val state: String,
    private val dutch: Boolean,
) {
    private val case = SheetCase(state, DeviceProfiles.pixel8, if (dutch) AppLanguage.Nl else AppLanguage.En, dutch)
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)

    @Test fun capture() {
        compose.setContent { SheetFixture(case) { StepSheetContent(sheetState(state, dutch), false, {}, {}) } }
        BoardCapture.capture(
            compose,
            if (state ==
                "gone"
            ) {
                "SheetStates_stepGone_pixel8_en_light"
            } else {
                "ItemDetail_${state}_pixel8_${if (dutch) "nl_dark" else "en_light"}"
            },
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("running", false),
                arrayOf("done", false),
                arrayOf("skipped", false),
                arrayOf("gone", false),
                arrayOf("running", true),
            )
    }
}

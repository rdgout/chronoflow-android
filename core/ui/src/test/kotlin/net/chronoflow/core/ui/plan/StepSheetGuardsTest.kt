package net.chronoflow.core.ui.plan

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
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
class StepSheetGuardsTest(
    private val case: SheetCase,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(case.profile, case.language, case.dark),
            ).around(compose)

    @Test fun sheetFits() {
        compose.setContent {
            SheetFixture(case) {
                StepSheetContent(
                    sheetState(
                        case.state,
                        case.language == AppLanguage.Nl,
                    ),
                    false,
                    {},
                    {},
                )
            }
        }
        Guards.assertAll(
            compose,
            case.profile,
            headingTags =
                setOf(
                    if (case.state ==
                        "gone"
                    ) {
                        "step.gone"
                    } else {
                        "step.title"
                    },
                ),
            rootTag = "sheet.fixture",
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = sheetCases(listOf("running", "upcoming", "done", "skipped", "information", "gone"))
    }
}

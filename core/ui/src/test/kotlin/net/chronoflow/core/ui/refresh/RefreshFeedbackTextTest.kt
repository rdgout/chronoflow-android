package net.chronoflow.core.ui.refresh

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.sync.refresh.RefreshFeedback
import net.chronoflow.core.sync.refresh.RefreshFeedbackKey
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class RefreshFeedbackTextTest(
    private val language: AppLanguage,
    private val feedback: List<RefreshFeedback>,
    private val expected: String,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8, language)).around(compose)

    @Test fun statesUseExistingCopy() {
        var actual = ""
        compose.setContent {
            val text = refreshFeedbackText(feedback)
            SideEffect { actual = text }
        }
        compose.runOnIdle { assertEquals(expected, actual) }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{2}")
        fun cases(): List<Array<Any>> {
            fun row(
                language: AppLanguage,
                key: RefreshFeedbackKey,
                count: Int?,
                expected: String,
            ) = arrayOf<Any>(language, listOf(RefreshFeedback(key, count)), expected)
            return listOf(
                row(AppLanguage.En, RefreshFeedbackKey.UpToDate, null, "Up to date"),
                row(AppLanguage.En, RefreshFeedbackKey.NeedsConnection, null, "Needs a connection"),
                row(AppLanguage.En, RefreshFeedbackKey.SomethingWentWrong, null, "Something went wrong"),
                row(AppLanguage.En, RefreshFeedbackKey.TripsAdded, 1, "1 trip added"),
                row(AppLanguage.En, RefreshFeedbackKey.TripsUpdated, 3, "3 trips updated"),
                row(AppLanguage.Nl, RefreshFeedbackKey.UpToDate, null, "Alles is bijgewerkt"),
                row(AppLanguage.Nl, RefreshFeedbackKey.NeedsConnection, null, "Verbinding nodig"),
                row(AppLanguage.Nl, RefreshFeedbackKey.SomethingWentWrong, null, "Er ging iets mis"),
                row(AppLanguage.Nl, RefreshFeedbackKey.TripsAdded, 1, "1 reis toegevoegd"),
                row(AppLanguage.Nl, RefreshFeedbackKey.TripsUpdated, 3, "3 reizen bijgewerkt"),
                arrayOf(
                    AppLanguage.En,
                    listOf(
                        RefreshFeedback(RefreshFeedbackKey.TripsAdded, 3),
                        RefreshFeedback(RefreshFeedbackKey.TripsRemoved, 1),
                    ),
                    "3 trips added · 1 trip removed",
                ),
                arrayOf(
                    AppLanguage.Nl,
                    listOf(
                        RefreshFeedback(RefreshFeedbackKey.TripsAdded, 3),
                        RefreshFeedback(RefreshFeedbackKey.TripsRemoved, 1),
                    ),
                    "3 reizen toegevoegd · 1 reis verwijderd",
                ),
            )
        }
    }
}

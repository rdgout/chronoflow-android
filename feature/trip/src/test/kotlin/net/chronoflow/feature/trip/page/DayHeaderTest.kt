package net.chronoflow.feature.trip.page

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.plan.PreparedNotice
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DayHeaderTest : PageComponentTest() {
    @Test fun warningNoticesStayCollapsedUntilExpanded() {
        val day =
            prepared.days[0].copy(
                warningCount = 2,
                status = prepared.title,
                notices = listOf(PreparedNotice(prepared.summary, true), PreparedNotice(prepared.subtitle, true)),
            )
        host { DayHeader(day) }
        compose
            .onNodeWithTag(
                "trip.dayHeader.${day.id}",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithTag("trip.dayHeader.warnings.${day.id}").assertExists()
        compose.onNodeWithTag("trip.dayHeader.status.${day.id}").assertDoesNotExist()
        compose.onNodeWithTag("trip.dayHeader.notice.${day.id}.0").assertDoesNotExist()
        compose.onNodeWithTag("trip.dayHeader.more.${day.id}").performClick()
        compose.onNodeWithTag("trip.dayHeader.status.${day.id}").assertExists()
        compose.onNodeWithTag("trip.dayHeader.summary.${day.id}").assertExists()
        compose.onNodeWithTag("trip.dayHeader.progress.${day.id}").assertExists()
        compose.onNodeWithTag("trip.dayHeader.notice.${day.id}.0").assertExists()
        compose.onNodeWithTag("trip.dayHeader.notice.${day.id}.1").assertExists()
    }

    @Test fun notesRequireTheirOwnToggle() {
        val day =
            prepared.days[0].copy(
                warningCount = 0,
                notices = listOf(PreparedNotice(prepared.summary, false), PreparedNotice(prepared.subtitle, false)),
            )
        host { DayHeader(day) }
        compose.onNodeWithTag("trip.dayHeader.more.${day.id}").performClick()
        compose.onNodeWithTag("trip.dayHeader.notice.${day.id}.0").assertDoesNotExist()
        compose.onNodeWithTag("trip.dayHeader.notes.${day.id}").performClick()
        compose.onNodeWithTag("trip.dayHeader.notice.${day.id}.0").assertExists()
        compose.onNodeWithTag("trip.dayHeader.notice.${day.id}.1").assertExists()
    }

    @Test fun expansionSurvivesStateRestoration() {
        val day = prepared.days[0]
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            net.chronoflow.core.designsystem.theme
                .ChronoFlowTheme { DayHeader(day) }
        }
        compose.onNodeWithTag("trip.dayHeader.more.${day.id}").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("trip.dayHeader.summary.${day.id}").assertExists()
    }
}

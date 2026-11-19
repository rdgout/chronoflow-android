package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.testing.MockTripTemplates
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** (new) */
class PlanPreparerTests {
    @Test fun nowTextsOnlyForDaysContainingNow() =
        runTest(timeout = 60.seconds) {
            val detail = MockTripTemplates.templates()[2].detail
            val preparer = preparer()
            val layouts = preparer.layouts(detail, emptyList<BlockType>())
            val day = detail.plan.days[2]
            val now = planDate(day.startUtc)!!.plusSeconds(3600)
            val texts = preparer.nowTexts(layouts, now, Language.En, TimeFormat.Value24)
            val expected =
                testFormatter.nowLabel(
                    layouts.getValue(day.id),
                    now,
                    CopyFormat(Language.En, TimeFormat.Value24),
                )
            assertThat(texts).isEqualTo(mapOf(day.id to expected))
            val before = planDate(detail.plan.days[0].startUtc)!!.minusSeconds(60)
            assertThat(preparer.nowTexts(layouts, before, Language.En, TimeFormat.Value24)).isEmpty()
        }
}

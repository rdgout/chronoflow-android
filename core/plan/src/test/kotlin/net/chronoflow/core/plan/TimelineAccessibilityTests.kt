package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.Language
import org.junit.Test

class TimelineAccessibilityTests {
    @Test fun timeOrderLabelsOptionalFilteringAndContinuations() {
        val detail = tripDetail()
        val problems = mutableListOf<String>()
        for (language in listOf(Language.En, Language.Nl)) {
            val copy = PageCopy(language, testPageCopy)
            val prepared = prepare(detail, language)
            for (day in prepared.days) {
                val dates = day.blocks.map { planDate(it.block.startUtc)!! }
                if (dates != dates.sorted()) problems += "${language.raw} ${day.id}: not in time order"
                for (block in day.blocks) {
                    val label = block.accessibilityLabel
                    if (isOptionalStep(block.block)) problems += "${block.id}: optional"
                    for (part in listOf(block.title, block.priority, block.interval)) {
                        if (!label.contains(part)) problems += "${block.id}: label lacks '$part'"
                    }
                    if (block.continuesBefore && !label.contains(copy("Continues from the previous plan day"))) {
                        problems += "${block.id}: no continues-before phrase"
                    }
                    if (block.continuesAfter && !label.contains(copy("Continues into the next plan day"))) {
                        problems += "${block.id}: no continues-after phrase"
                    }
                }
            }
        }
        assertThat(problems).isEqualTo(emptyList<String>())
    }
}

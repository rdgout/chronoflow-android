package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.Language
import org.junit.Test

class TripPageCopyTests {
    private val en = PageCopy(Language.En, testPageCopy)
    private val nl = PageCopy(Language.Nl, testPageCopy)

    @Test fun warningAndNotePluralsFollowTheSelectedLanguage() {
        val cases =
            listOf(
                listOf(en, "warning", "warnings", "note", "notes"),
                listOf(nl, "waarschuwing", "waarschuwingen", "notitie", "notities"),
            )
        val problems = mutableListOf<String>()
        for (case in cases) {
            val copy = case[0] as PageCopy
            val (warning, warnings, note, notes) = case.drop(1).map { it as String }
            if (copy.count("%lld warnings", 1) != "1 $warning") problems += "${copy.language.raw} warnings 1"
            if (copy.count("%lld notes", 1) != "1 $note") problems += "${copy.language.raw} notes 1"
            for (count in listOf(0, 2, 12)) {
                if (copy.count("%lld warnings", count) != "$count $warnings") problems += "warnings $count"
                if (copy.count("%lld notes", count) != "$count $notes") problems += "notes $count"
            }
        }
        assertThat(problems).isEqualTo(emptyList<String>())
    }
}

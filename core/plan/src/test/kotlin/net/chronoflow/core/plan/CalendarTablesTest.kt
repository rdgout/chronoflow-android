package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.Language
import org.junit.Test

class CalendarTablesTest {
    private val f = CopyLineFormatter(testCatalog)
    private val enMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val nlMonths = listOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec")

    @Test fun monthsOfTwentyTwentySix() {
        val actual =
            (1..12).map { month ->
                val date = "2026-" + month.toString().padStart(2, '0') + "-15"
                f.formatDayMonth(date, Language.En) to f.formatDayMonth(date, Language.Nl)
            }
        assertThat(actual).isEqualTo(enMonths.indices.map { "15 ${enMonths[it]}" to "15 ${nlMonths[it]}" })
    }

    @Test fun weekdaysMondayToSunday() {
        val en = (16..22).map { f.formatWeekday("2026-11-$it", Language.En) }
        val nl = (16..22).map { f.formatWeekday("2026-11-$it", Language.Nl) }
        assertThat(en).isEqualTo(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"))
        assertThat(nl).isEqualTo(listOf("ma", "di", "wo", "do", "vr", "za", "zo"))
    }

    @Test fun appleRows() {
        assertThat(f.formatDate("2026-09-15", Language.En)).isEqualTo("15 Sep 2026")
        assertThat(f.formatDate("2026-09-15", Language.Nl)).isEqualTo("15 sep 2026")
        assertThat(f.formatWeekdayDate("2026-09-15", Language.En)).isEqualTo("Tue 15 Sep")
        assertThat(f.formatWeekdayDate("2026-09-15", Language.Nl)).isEqualTo("di 15 sep")
        assertThat(f.formatMonthYear("2026-05-03", Language.En)).isEqualTo("May 2026")
        assertThat(f.formatMonthYear("2026-05-03", Language.Nl)).isEqualTo("mei 2026")
        assertThat(f.formatWeekdayDate("2026-05-03", Language.En, month = false)).isEqualTo("Sun 3")
        assertThat(f.formatWeekdayDate("2026-05-03", Language.Nl, month = false)).isEqualTo("zo 3")
        assertThat(f.formatDayMonth("2026-05-03", Language.En)).isEqualTo("3 May")
        assertThat(f.formatDayMonth("2026-05-03", Language.Nl)).isEqualTo("3 mei")
    }
}

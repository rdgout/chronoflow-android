package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.Language
import org.junit.Test

val testPageCopy: PageCopyTable by lazy { PageCopyTable.load() }

class PageCopyTests {
    private val nl = PageCopy(Language.Nl, testPageCopy)
    private val en = PageCopy(Language.En, testPageCopy)

    /** (new) Cases of iOS `TripPageCopyTests` l.41-47 and `TripPageTests` l.116. */
    @Test fun substitutionAndFallbacks() {
        assertThat(nl("Go to a day")).isEqualTo("Ga naar een dag")
        assertThat(nl("Home · %@", "Amsterdam")).isEqualTo("Thuis · Amsterdam")
        assertThat(nl("There · %@", "Tokyo")).isEqualTo("Daar · Tokyo")
        assertThat(nl("%@ · now", "10:30 AM")).isEqualTo("10:30 AM · nu")
        assertThat(nl("%@ to %@, %@, %@. Go to a day", "Amsterdam", "Tokyo", "Reisdag", "ma 12 okt"))
            .isEqualTo("Amsterdam naar Tokyo, Reisdag, ma 12 okt. Ga naar een dag")
        assertThat(en.count("%lld warnings", 1)).isEqualTo("1 warning")
        assertThat(nl.count("%lld warnings", 2)).isEqualTo("2 waarschuwingen")
        assertThat(en("An unknown %@ key", "x")).isEqualTo("An unknown x key")
        assertThat(PageCopy(Language.Unknown("fr"), testPageCopy)("Go to a day")).isEqualTo("Go to a day")
        assertThat(en == PageCopy(Language.En, PageCopyTable(emptyMap()))).isEqualTo(true)
    }
}

package net.chronoflow.core.plan.trippage

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

class TripPageLayoutTests {
    @Test fun breakpoints() {
        for (width in listOf(599.0, 600.0, 899.0, 900.0, 1200.0)) {
            val layout = TripPageLayout.resolve(width)
            assertThat(layout.isWide).isEqualTo(width >= 600)
            assertThat(layout.listWidth).isEqualTo(
                if (width < 600) {
                    null
                } else if (width < 900) {
                    280.0
                } else {
                    320.0
                },
            )
        }
    }

    @Test fun phoneWidthsStayPhone() {
        for (width in listOf(280.0, 320.0, 360.0, 375.0, 390.0, 393.0, 402.0, 411.0, 420.0, 430.0, 440.0, 466.0)) {
            assertThat(TripPageLayout.resolve(width).isWide).isEqualTo(false)
        }
    }

    @Test fun unfoldedDuoIsWideInBothOrientations() {
        assertThat(TripPageLayout.resolve(669.0).listWidth).isEqualTo(280.0)
        assertThat(TripPageLayout.resolve(951.0).listWidth).isEqualTo(320.0)
        assertThat(TripPageLayout.resolve(841.0).listWidth).isEqualTo(280.0)
        assertThat(TripPageLayout.resolve(914.0).listWidth).isEqualTo(320.0)
        assertThat(TripPageLayout.resolve(599.0).listWidth).isNull()
        assertThat(TripPageLayout.resolve(700.0).gutter).isEqualTo(20.0)
        assertThat(TripPageLayout.resolve(400.0).gutter).isEqualTo(16.0)
    }

    @Test fun listSticksBelowTheWholeHeaderAndShrinksAtTheEndOfItsDay() {
        val position = TripWideListPosition(-398.0, 760.0, 850.0, 48.0, 700.0)
        assertThat(position.offset).isEqualTo(446.0)
        assertThat(position.height).isEqualTo(314.0)
        assertThat(-398 + position.offset).isEqualTo(48.0)
        assertThat(-398 + position.offset + position.height).isEqualTo(-398.0 + 760)
    }

    @Test fun shortListStaysBesideItsDayAndTallListScrollsWithinTheViewport() {
        val short = TripWideListPosition(180.0, 1200.0, 850.0, 48.0, 220.0)
        assertThat(short.offset).isEqualTo(0.0)
        assertThat(short.height).isEqualTo(220.0)
        val tall = TripWideListPosition(-100.0, 1200.0, 650.0, 80.0, 1500.0)
        assertThat(tall.offset).isEqualTo(180.0)
        assertThat(tall.height).isEqualTo(570.0)
    }

    @Test fun listDisappearsWhenItsDayPassesTheHeader() {
        val position = TripWideListPosition(-800.0, 760.0, 850.0, 48.0, 700.0)
        assertThat(position.offset).isEqualTo(760.0)
        assertThat(position.height).isEqualTo(0.0)
    }
}

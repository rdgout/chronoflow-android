package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetailLegView
import net.chronoflow.core.testing.MockTripTemplates
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

    @Test fun compactDateRangesAndOutboundFlightNumbers() {
        assertThat(tripDatesText("2026-10-12", "2026-10-18", Language.En)).isEqualTo("12–18 Oct")
        assertThat(tripDatesText("2026-10-12", "2026-10-12", Language.En)).isEqualTo("12 Oct")
        assertThat(tripDatesText("2026-10-12", "2026-11-02", Language.En)).isEqualTo("12 Oct–2 Nov")
        assertThat(tripDatesText("2026-12-30", "2027-01-02", Language.En)).isEqualTo("30 Dec 2026–2 Jan 2027")
        assertThat(tripDatesText("2026-10-12", "2026-10-18", Language.En, long = true)).isEqualTo("12 to 18 October")
        assertThat(tripDatesText("2026-10-12", "2026-10-18", Language.Nl, long = true)).isEqualTo("12 tot 18 oktober")
        assertThat(tripDatesText("2026-09-01", "2026-09-03", Language.En)).isEqualTo("1–3 Sep")
        assertThat(tripDatesText("2026-03-01", "2026-03-02", Language.Nl, long = true)).isEqualTo("1 tot 2 maart")
        assertThat(tripDatesText("2026-10-12", null, Language.Nl)).isEqualTo("12 okt")
        assertThat(tripMetaText("2026-10-12", "2026-10-18", listOf(" KL 861 ", "", "JL 1"), Language.En))
            .isEqualTo("12–18 Oct · KL 861 · JL 1")
    }

    @Test fun outboundFlightNumbersFillTheSubtitle() {
        val detail = MockTripTemplates.templates()[2].detail
        val from = detail.trip.from
        val to = detail.trip.to
        val legs =
            listOf(
                TripDetailLegView(0, " KL 861 ", from, to, returnLeg = false),
                TripDetailLegView(1, "", from, to, returnLeg = false),
                TripDetailLegView(2, "JL 1", from, to, returnLeg = false),
                TripDetailLegView(3, "RETURN", to, from, returnLeg = true),
            )
        val prepared = prepare(detail.copy(trip = detail.trip.copy(legs = legs)))
        assertThat(prepared.subtitle.endsWith(" · KL 861 · JL 1")).isEqualTo(true)
        assertThat(prepared.subtitle.contains("RETURN")).isEqualTo(false)
    }

    @Test fun preparationPreservesNoticeTones() {
        val detail = MockTripTemplates.templates()[2].detail
        val layouts = layoutPlan(detail.plan, detail.timeline)
        val problems = mutableListOf<String>()
        for (language in listOf(Language.En, Language.Nl)) {
            for (day in prepare(detail, language, layouts = layouts).days) {
                val notices = noticeItems(detail.plan, detail.notices, day.day)
                if (day.notices.map { it.warning } != notices.map { it.warning }) problems += "${day.id}: tones"
                if (day.warningCount != day.notices.count { it.warning }) problems += "${day.id}: warning count"
                if (!day.notices.all { it.text.isNotEmpty() }) problems += "${day.id}: empty text"
                if (day.shownNotices(expanded = false, notesOpen = true).isNotEmpty()) {
                    problems +=
                        "${day.id}: collapsed"
                }
                if (day.shownNotices(expanded = true, notesOpen = true) != day.notices) problems += "${day.id}: open"
                val warningsOnly = if (day.warningCount > 0) day.notices else emptyList()
                if (day.shownNotices(expanded = true, notesOpen = false) !=
                    warningsOnly
                ) {
                    problems += "${day.id}: warnings"
                }
            }
        }
        assertThat(problems).isEqualTo(emptyList<String>())
    }
}

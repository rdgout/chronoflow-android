package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripPageTests {
    @Test fun pendingOldHashMarksAndUndoOverrideTheNewPlan() =
        runTest(timeout = 60.seconds) {
            var detail = tripDetail()
            val key =
                checkNotNull(
                    detail.plan.blocks
                        .first {
                            it.actionable && it.checkKey != null &&
                                !isOptionalStep(it)
                        }.checkKey,
                )
            detail =
                detail.copy(
                    checkState =
                        detail.checkState.copy(
                            marks = listOf(CheckMark(key, CheckStatus.Done, detail.trip.departureUtc)),
                        ),
                )
            val old =
                CheckOffChange(
                    detail.trip.id,
                    "previous-hash",
                    key.type,
                    key.localDate,
                    key.ordinal,
                    CheckStatus.Skipped,
                )
            val preparer = preparer()
            val marked =
                preparer.prepare(
                    detail,
                    emptyList(),
                    Language.En,
                    TimeFormat.Value24,
                    emptyList(),
                    listOf(old),
                )
            assertThat(
                marked.days
                    .flatMap { it.blocks }
                    .first { it.block.checkKey == key }
                    .status,
            ).isEqualTo(CheckStatus.Skipped)
            val undo = old.copy(status = null)
            val undone =
                preparer.prepare(
                    detail,
                    emptyList(),
                    Language.En,
                    TimeFormat.Value24,
                    emptyList(),
                    listOf(old, undo),
                )
            assertThat(
                undone.days
                    .flatMap { it.blocks }
                    .first { it.block.checkKey == key }
                    .status,
            ).isNull()
            assertThat(preparer.layoutCount).isEqualTo(1)
            detail = detail.copy(detailVersion = detail.detailVersion + "-replacement")
            preparer.prepare(detail, emptyList(), Language.En, TimeFormat.Value24, emptyList())
            assertThat(preparer.layoutCount).isEqualTo(2)
        }

    @Test fun preparationCachesLayoutsAndPreservesMarkLabels() =
        runTest(timeout = 60.seconds) {
            val detail = tripDetail()
            val preparer = preparer()
            val first = preparer.prepare(detail, emptyList(), Language.En, TimeFormat.Value24, emptyList())
            preparer.prepare(detail, emptyList(), Language.En, TimeFormat.Value24, emptyList())
            assertThat(preparer.layoutCount).isEqualTo(1)
            val block = detail.plan.blocks.first { it.actionable && it.checkKey != null && !isOptionalStep(it) }
            val mark = CheckMark(checkNotNull(block.checkKey), CheckStatus.Done, detail.plan.days[0].startUtc)
            val changed = preparer.prepare(detail, emptyList(), Language.Nl, TimeFormat.Value12, listOf(mark))
            assertThat(preparer.layoutCount).isEqualTo(1)
            assertThat(changed.days.size).isEqualTo(first.days.size)
            assertThat(
                changed.days
                    .flatMap { it.blocks }
                    .first { it.id == block.id }
                    .status,
            ).isEqualTo(CheckStatus.Done)
            assertThat(PageCopy(Language.Nl, testPageCopy)("Go to a day")).isEqualTo("Ga naar een dag")
            preparer.evict(detail.trip.id)
            preparer.prepare(detail, emptyList(), Language.En, TimeFormat.Value24, emptyList())
            assertThat(preparer.layoutCount).isEqualTo(2)
        }
}

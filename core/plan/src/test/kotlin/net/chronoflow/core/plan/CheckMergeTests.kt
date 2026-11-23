package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.PlanView
import net.chronoflow.core.model.store.CheckMarkRecord
import org.junit.Test

class CheckMergeTests {
    private val value = tripDetail()
    private val plan = value.plan
    private val hash = plan.meta.inputHash
    private val block: BlockView = plan.blocks.first { it.actionable && it.checkKey != null }
    private val key = block.checkKey!!

    private fun row(
        status: String,
        inputHash: String = hash,
        at: String = "t",
        k: net.chronoflow.core.model.contracts.CheckKeyView = key,
    ) = CheckMarkRecord("trip", inputHash, k.type.raw, k.localDate, k.ordinal, status, at)

    private fun pending(
        status: CheckStatus?,
        inputHash: String = hash,
    ) = CheckOffChange("trip", inputHash, key.type, key.localDate, key.ordinal, status)

    private fun merge(
        rows: List<CheckMarkRecord> = emptyList(),
        pending: List<CheckOffChange> = emptyList(),
        p: PlanView = plan,
    ) = mergedCheckState(p, hash, rows, pending)

    @Test fun pendingDoneOverridesStoredSkippedWithEmptyMarkedAt() {
        val state = merge(listOf(row("skipped")), listOf(pending(CheckStatus.Done)))
        assertThat(state.marks.map { it.status to it.markedAt }).isEqualTo(listOf(CheckStatus.Done to ""))
    }

    @Test fun pendingNullRemovesStoredMark() {
        assertThat(merge(listOf(row("done")), listOf(pending(null))).marks.size).isEqualTo(0)
    }

    @Test fun storedRowWithOtherInputHashIsIgnored() {
        assertThat(merge(listOf(row("done", inputHash = "other"))).marks.size).isEqualTo(0)
    }

    @Test fun pendingChangeWithOtherPlanInputHashStillApplies() {
        assertThat(merge(pending = listOf(pending(CheckStatus.Done, inputHash = "old"))).marks.size).isEqualTo(1)
    }

    @Test fun markWithoutMatchingActionableBlockIsDropped() {
        val stray = key.copy(ordinal = 9_999)
        val state = merge(listOf(row("done", k = stray)))
        assertThat(state.marks.size).isEqualTo(0)
        assertThat(state.days.sumOf { it.done }).isEqualTo(0)
    }

    @Test fun marksAreSortedByIdentity() {
        val blocks = plan.blocks.filter { it.actionable && it.checkKey != null }.take(3)
        val rows = blocks.reversed().map { row("done", k = it.checkKey!!) }
        val identities = merge(rows).marks.map { checkIdentity(it.key) }
        assertThat(identities).isEqualTo(identities.sorted())
    }

    @Test fun unknownStatusIsKeptButCountedNowhere() {
        val state = merge(listOf(row("maybe")))
        assertThat(state.marks.size).isEqualTo(1)
        assertThat(state.days.sumOf { it.done + it.skipped }).isEqualTo(0)
    }

    @Test fun blockStartingAtDayEndCountsOnTheNextDay() {
        val first = plan.days[0]
        val moved = block.copy(startUtc = first.endUtc, endUtc = first.endUtc)
        val edited = plan.copy(blocks = listOf(moved))
        val state = buildCheckState(edited, emptyList())
        assertThat(state.days[0].total).isEqualTo(0)
        assertThat(state.days[1].total).isEqualTo(1)
    }

    @Test fun blockWithUnparseableStartCountsOnNoDay() {
        val edited = plan.copy(blocks = listOf(block.copy(startUtc = "garbage")))
        assertThat(buildCheckState(edited, emptyList()).days.sumOf { it.total }).isEqualTo(0)
    }
}

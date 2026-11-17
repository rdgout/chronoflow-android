package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.testing.MockTripTemplates
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** (new) Unchanged days keep their instance across revisions (04 §9.3, OQ-1b-4). */
class StableDayIdentityTests {
    private val detail: TripDetail get() = MockTripTemplates.templates()[2].detail

    private suspend fun PlanPreparer.run(
        value: TripDetail,
        language: Language = Language.En,
        hidden: List<BlockType> = emptyList(),
        marks: List<CheckMark> = emptyList(),
    ) = prepare(value, hidden, language, TimeFormat.Value24, marks)

    @Test fun aCheckOffRebuildsOnlyItsDay() =
        runTest(timeout = 60.seconds) {
            val value = detail
            val preparer = preparer()
            val first = preparer.run(value)
            assertThat(preparer.run(value) === first).isEqualTo(true)
            val identities = value.plan.blocks.mapNotNull { b -> b.checkKey?.let { checkIdentity(it) } }
            val target =
                first.days
                    .withIndex()
                    .flatMap { (i, day) ->
                        day.blocks
                            .filter {
                                it.block.actionable && it.block.checkKey != null &&
                                    !it.continuesBefore &&
                                    !it.continuesAfter
                            }.map { i to it }
                    }.first { (_, b) ->
                        identities.count { it == checkIdentity(checkNotNull(b.block.checkKey)) } == 1
                    }
            val key = checkNotNull(target.second.block.checkKey)
            val second = preparer.run(value, marks = listOf(CheckMark(key, CheckStatus.Done, value.trip.departureUtc)))
            for (i in first.days.indices) {
                if (i == target.first) {
                    assertThat(second.days[i] === first.days[i]).isEqualTo(false)
                    assertThat(second.days[i].progress).isNotEqualTo(first.days[i].progress)
                } else {
                    assertThat(second.days[i] === first.days[i]).isEqualTo(true)
                }
            }
            val dutch = preparer.run(value, language = Language.Nl)
            assertThat(dutch.days.indices.none { dutch.days[it] === second.days[it] }).isEqualTo(true)
        }

    @Test fun aNewDetailVersionWithTheSameContentKeepsEveryDay() =
        runTest(timeout = 60.seconds) {
            val value = detail
            val preparer = preparer()
            val first = preparer.run(value)
            val second = preparer.run(value.copy(detailVersion = value.detailVersion + "-next"))
            assertThat(second.revision).isNotEqualTo(first.revision)
            assertThat(second.days.indices.all { second.days[it] === first.days[it] }).isEqualTo(true)
        }

    @Test fun hidingALaneRelayoutsAndRebuildsItsDays() =
        runTest(timeout = 60.seconds) {
            val value = detail
            val preparer = preparer()
            val first = preparer.run(value)
            val before = preparer.layoutCount
            val hidden = preparer.run(value, hidden = listOf(BlockType.Sleep))
            assertThat(preparer.layoutCount).isEqualTo(before + 1)
            val sleeping =
                first.days.indices.filter { i ->
                    first.days[i].blocks.any { it.block.type == BlockType.Sleep }
                }
            assertThat(sleeping.isNotEmpty()).isEqualTo(true)
            assertThat(sleeping.all { hidden.days[it] !== first.days[it] }).isEqualTo(true)
        }
}

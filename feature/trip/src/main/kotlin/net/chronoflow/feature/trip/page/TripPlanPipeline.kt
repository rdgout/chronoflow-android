package net.chronoflow.feature.trip.page

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckKeyView
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.drawing.TimelineTraceNames
import net.chronoflow.core.plan.localDate
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.feature.trip.timeline.TimelineTracer
import java.io.IOException

data class PipelineInput(
    val snapshot: TripPlanSnapshot,
    val preferences: PlanPreferences,
    val language: Language,
)

@Immutable
class ReadyPlan(
    val detail: TripDetail,
    val prepared: PreparedPlan,
    val layouts: Map<String, DayLayout>,
    val index: TripListIndex,
    val timeFormat: TimeFormat,
    val language: Language,
    val hasHiddenSteps: Boolean,
)

data class PipelineOutput(
    val trip: TripRecord?,
    val title: TripTitleParts?,
    val noPlan: Boolean,
    val plan: ReadyPlan?,
)

fun interface TripPlanPipeline {
    suspend fun build(
        input: PipelineInput,
        previous: ReadyPlan?,
    ): PipelineOutput
}

class DefaultTripPlanPipeline(
    private val decoder: PlanDecoder,
    private val preparer: PlanPreparer,
    private val tracer: TimelineTracer = TimelineTracer.Android,
    private val reportFailure: () -> Unit = { Log.w("CfTrip", "plan preparation failed") },
) : TripPlanPipeline {
    private var firstInput = true
    private var decodedTripId: String? = null

    override suspend fun build(
        input: PipelineInput,
        previous: ReadyPlan?,
    ): PipelineOutput {
        val snapshot = input.snapshot
        val trip = snapshot.trip
        val first = firstInput.also { firstInput = false }
        if (trip == null || trip.deletedAt != null || trip.deletedLocally || snapshot.plan == null) {
            (trip?.id ?: decodedTripId)?.let { preparer.evict(it) }
            previous
                ?.detail
                ?.trip
                ?.id
                ?.takeIf { it != trip?.id }
                ?.let { preparer.evict(it) }
            return PipelineOutput(trip, cardTitle(trip), false, null)
        }
        return try {
            val detail = traced(first, TimelineTraceNames.DECODE, 1) { checkNotNull(decoder.detail(snapshot)) }
            decodedTripId = detail.trip.id
            val preferences = input.preferences
            val ready =
                traced(first, TimelineTraceNames.PREPARE, 2) {
                    val hidden = preferences.hiddenCategories
                    val marks =
                        snapshot.checks.map {
                            CheckMark(
                                CheckKeyView(BlockType.of(it.type), it.localDate, it.ordinal),
                                CheckStatus.of(it.status),
                                it.markedAt,
                            )
                        }
                    val layouts = preparer.layouts(detail, hidden)
                    val prepared =
                        preparer.prepare(
                            detail,
                            hidden,
                            input.language,
                            preferences.timeFormat,
                            marks,
                            snapshot.pendingChecks,
                        )
                    ReadyPlan(
                        detail,
                        prepared,
                        layouts,
                        TripListIndex(prepared.days.map { it.id }, hidden.isNotEmpty()),
                        preferences.timeFormat,
                        input.language,
                        hidden.isNotEmpty(),
                    )
                }
            val from = detail.trip.from
            val to = detail.trip.to
            PipelineOutput(
                trip,
                TripTitleParts(from.iata, to.iata, from.city ?: from.iata, to.city ?: to.iata),
                detail.plan.status == PlanStatus.NoPlan,
                ready,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            failed(trip, previous)
        } catch (_: RuntimeException) {
            failed(trip, previous)
        }
    }

    private fun failed(
        trip: TripRecord?,
        previous: ReadyPlan?,
    ): PipelineOutput {
        reportFailure()
        val from = previous?.detail?.trip?.from
        val to = previous?.detail?.trip?.to
        val title =
            if (from != null &&
                to != null
            ) {
                TripTitleParts(from.iata, to.iata, from.city ?: from.iata, to.city ?: to.iata)
            } else {
                null
            }
        return PipelineOutput(trip, title, previous?.detail?.plan?.status == PlanStatus.NoPlan, previous)
    }

    private suspend fun cardTitle(trip: TripRecord?): TripTitleParts? {
        if (trip == null) return null
        return try {
            val card = decoder.card(trip)
            TripTitleParts(card.from.iata, card.to.iata, card.from.city ?: card.from.iata, card.to.city ?: card.to.iata)
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
            null
        }
    }

    private suspend fun <T> traced(
        first: Boolean,
        name: String,
        cookie: Int,
        block: suspend () -> T,
    ): T {
        if (!first) return block()
        tracer.beginAsync(name, cookie)
        return try {
            block()
        } finally {
            tracer.endAsync(name, cookie)
        }
    }
}

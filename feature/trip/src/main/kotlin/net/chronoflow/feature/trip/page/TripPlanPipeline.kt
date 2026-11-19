package net.chronoflow.feature.trip.page

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.contracts.AdaptationView
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckKeyView
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PageCopy
import net.chronoflow.core.plan.PageCopyTable
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.clockShiftText
import net.chronoflow.core.plan.drawing.TimelineTraceNames
import net.chronoflow.core.plan.localDate
import net.chronoflow.core.plan.tripMetaText
import net.chronoflow.core.plan.trippage.DayListRows
import net.chronoflow.core.plan.trippage.ListRows
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
    val listRows: List<DayListRows>,
)

@Immutable
data class TripPageData(
    val card: TripCardView? = null,
    val adaptation: AdaptationView? = null,
    val metadata: String = "",
    val clockShift: String? = null,
    val language: Language = Language.En,
    val timeFormat: TimeFormat = TimeFormat.Value24,
)

data class PipelineOutput(
    val trip: TripRecord?,
    val title: TripTitleParts?,
    val noPlan: Boolean,
    val plan: ReadyPlan?,
    val page: TripPageData = TripPageData(),
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
    private val catalogs: CopyCatalogProvider,
    private val io: CoroutineDispatcher,
    private val tracer: TimelineTracer = TimelineTracer.Android,
    private val reportFailure: () -> Unit = { Log.w("CfTrip", "plan preparation failed") },
) : TripPlanPipeline {
    private var pageTable: PageCopyTable? = null
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
            return PipelineOutput(trip, cardTitle(trip), false, null, pageData(input, null))
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
                        ListRows.build(prepared, pageCopy(input.language)),
                    )
                }
            val from = detail.trip.from
            val to = detail.trip.to
            PipelineOutput(
                trip,
                TripTitleParts(from.iata, to.iata, from.city ?: from.iata, to.city ?: to.iata),
                detail.plan.status == PlanStatus.NoPlan,
                ready,
                pageData(input, detail),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            failed(input, previous)
        } catch (_: RuntimeException) {
            failed(input, previous)
        }
    }

    private suspend fun failed(
        input: PipelineInput,
        previous: ReadyPlan?,
    ): PipelineOutput {
        reportFailure()
        val trip = input.snapshot.trip
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
        return PipelineOutput(
            trip,
            title,
            previous?.detail?.plan?.status == PlanStatus.NoPlan,
            previous,
            pageData(input, previous?.detail),
        )
    }

    private suspend fun pageData(
        input: PipelineInput,
        detail: TripDetail?,
    ): TripPageData {
        val record = input.snapshot.trip
        val card =
            record?.let {
                try {
                    decoder.card(it)
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    null
                }
            }
        val adaptation =
            record?.let {
                try {
                    decoder.adaptation(it)
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (_: RuntimeException) {
                    null
                }
            }
        val copy = pageCopy(input.language)
        val flights =
            detail
                ?.trip
                ?.legs
                ?.filter { !it.returnLeg }
                ?.mapNotNull { it.flightNumber }
                ?: listOfNotNull(card?.flightNumber)
        return TripPageData(
            card = card,
            adaptation = adaptation,
            metadata =
                card
                    ?.let {
                        tripMetaText(
                            it.departLocal,
                            detail
                                ?.plan
                                ?.days
                                ?.lastOrNull()
                                ?.labelDate ?: record.planEndDate,
                            flights,
                            input.language,
                            copy,
                        )
                    }.orEmpty(),
            clockShift =
                adaptation?.let {
                    clockShiftText(
                        it,
                        CopyLineFormatter(catalogs.catalog()),
                        CopyFormat(input.language, input.preferences.timeFormat),
                        copy,
                    )
                },
            language = input.language,
            timeFormat = input.preferences.timeFormat,
        )
    }

    private suspend fun pageCopy(language: Language): PageCopy {
        val table = pageTable ?: withContext(io) { PageCopyTable.load() }.also { pageTable = it }
        return PageCopy(language, table)
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

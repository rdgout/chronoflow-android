package net.chronoflow.feature.trip.timeline

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.drawing.DayDrawing
import net.chronoflow.core.plan.drawing.DayDrawingCache
import net.chronoflow.core.plan.drawing.DayDrawingKey
import net.chronoflow.core.plan.drawing.TimelineTarget
import net.chronoflow.core.testing.FakeTextMetrics

/** One S3 day with a real repository on an unconfined build dispatcher; [gate] holds the build back until completed. */
class DayTimelineRig(
    language: Language = Language.En,
    gated: Boolean = false,
    window: Int = 411,
) {
    private val fixture = TimelineTestPlans.prepared("S3", language)
    val gate: CompletableDeferred<Unit>? = if (gated) CompletableDeferred() else null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    val cache = DayDrawingCache()
    val repository =
        TimelineDrawingRepository(
            cache,
            palette = TimelineTestPlans::palette,
            metrics = {
                gate?.await()
                FakeTextMetrics()
            },
            buildDispatcher = Dispatchers.Unconfined,
            scope = scope,
            tracer = TimelineTracer.None,
        )
    val resources = TimelineResources(TimelineTestPlans.DENSITY, false, TimelineTestPlans.palette(false))
    val clock = DayClocks().clock("rig")

    val day: PreparedDay
    val layout: DayLayout
    val key: DayDrawingKey
    val reference: DayDrawing

    // The first S3 day whose first block target lies in the top of the day, so a test click lands inside the window.
    init {
        var found: Triple<PreparedDay, DayLayout, DayDrawing>? = null
        for (candidate in fixture.plan.days) {
            val candidateKey = TimelineTestPlans.key(candidate, fixture.plan.revision, window, language = language)
            val built =
                TimelineTestPlans.build(
                    candidate,
                    fixture.layouts.getValue(candidate.id),
                    candidateKey,
                    FakeTextMetrics(),
                )
            val first = built.targets.firstOrNull { it.kind == TimelineTarget.Kind.BLOCK }
            if (first != null && first.bottom < 420 * TimelineTestPlans.DENSITY) {
                found = Triple(candidate, fixture.layouts.getValue(candidate.id), built)
                break
            }
        }
        val (d, l, r) = checkNotNull(found) { "no S3 day has a block in its first 420 dp" }
        day = d
        layout = l
        reference = r
        key = r.key
    }

    val blockTargets: List<TimelineTarget> get() = reference.targets.filter { it.kind == TimelineTarget.Kind.BLOCK }

    fun close() = scope.cancel()
}

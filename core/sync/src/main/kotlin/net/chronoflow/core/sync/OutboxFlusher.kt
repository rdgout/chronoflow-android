package net.chronoflow.core.sync

import kotlinx.coroutines.CancellationException
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.StoreTime
import java.time.Instant

data class OutboxFlushResult(
    var refusals: List<SyncRefusal> = emptyList(),
    var retryAfter: Double? = null,
    var serverRetryAfter: Double? = null,
    var parked: Int = 0,
    var offline: Boolean = false,
    var deferredWalkthroughAuthorization: Boolean = false,
)

typealias PrepareCheck = suspend (OutboxRecord, SyncEpoch) -> OutboxRecord?

class OutboxFlusher(
    private val database: SyncDatabase,
    private val source: DataSource,
    private val now: () -> Instant,
    prepareCheck: PrepareCheck? = null,
    refreshCheck: PrepareCheck? = null,
) {
    private val reconciler = CheckOffReconciler(FullPull(database, source, now = now))
    private val prepareCheck: PrepareCheck = prepareCheck ?: { row, epoch -> reconciler.prepare(row, epoch) }
    private val refreshCheck: PrepareCheck =
        refreshCheck ?: { row, epoch -> reconciler.prepare(row, epoch, refresh = true) }

    suspend fun run(epoch: SyncEpoch): OutboxFlushResult {
        database.validate(epoch)
        epoch.check()
        val rows = database.pending()
        epoch.check()
        val blocked = mutableSetOf<String>()
        val result = OutboxFlushResult()
        for (initial in rows) {
            epoch.check()
            if (OutboxPayload.dependency(initial) in blocked) continue
            val current = database.outboxRow(initial.id)
            epoch.check()
            if (current == null) continue
            var row: OutboxRecord = current
            try {
                if (OutboxPayload.change(row) is OutboxChange.Unsupported) {
                    blocked += OutboxPayload.dependency(row)
                    result.parked += 1
                    continue
                }
                if (row.kind == "check-off") {
                    val prepared = prepareCheck(row, epoch)
                    epoch.check()
                    if (prepared == null) continue
                    row = prepared
                }
                for (attempt in 0 until 2) {
                    try {
                        epoch.check()
                        val receipt = source.send(OutboxPayload.change(row))
                        epoch.check()
                        database.applyReceipt(epoch, receipt, row)
                        epoch.check()
                        break
                    } catch (failure: DataFailure) {
                        epoch.check()
                        if (row.kind != "check-off" || !isStale(failure) || attempt == 1) throw failure
                        val refreshed = refreshCheck(row, epoch)
                        epoch.check()
                        if (refreshed == null) break
                        row = refreshed
                    }
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (error: Exception) {
                epoch.check()
                val failure = error as? DataFailure ?: DataFailure.Server(null)
                if (failure is DataFailure.Unauthorized && row.kind != "walkthrough") throw failure
                if (failure is DataFailure.EmailUnverified) throw failure
                if (failure is DataFailure.Unsupported) {
                    result.parked += 1
                    blocked += OutboxPayload.dependency(row)
                    continue
                }
                if (OutboxFailurePolicy.isFinal(failure) && row.kind != "walkthrough" &&
                    !(row.kind == "check-off" && isStale(failure))
                ) {
                    val refusal =
                        SyncRefusal(
                            row.id,
                            row.kind,
                            failure.status,
                            StoreTime.seconds(now()),
                            OutboxFailurePolicy.sanitized(failure),
                            SyncRefusalNotice.from(OutboxPayload.change(row)),
                        )
                    database.refuse(epoch, row, refusal)
                    epoch.check()
                    result.refusals += refusal
                } else {
                    val attempts =
                        database.recordTransientFailure(
                            epoch,
                            row.id,
                            OutboxFailurePolicy.sanitized(failure),
                        )
                    epoch.check()
                    val serverDelay = if (failure is DataFailure.Throttled) failure.retryAfterSeconds else 0.0
                    if (failure is DataFailure.Throttled) {
                        result.serverRetryAfter =
                            maxOf(result.serverRetryAfter ?: 0.0, serverDelay)
                    }
                    result.retryAfter =
                        maxOf(
                            result.retryAfter ?: 0.0,
                            OutboxFailurePolicy.retryDelay((attempts - 1).coerceAtLeast(0), serverDelay),
                        )
                    blocked += OutboxPayload.dependency(row)
                    if (failure is DataFailure.Unauthorized) {
                        result.deferredWalkthroughAuthorization = true
                        return result
                    }
                    if (failure is DataFailure.Offline) {
                        result.offline = true
                        return result
                    }
                }
            }
        }
        return result
    }

    companion object {
        fun isStale(failure: DataFailure): Boolean =
            failure is DataFailure.Conflict ||
                (
                    failure is DataFailure.Validation && (
                        "plan_input_hash" in failure.fields ||
                            failure.fields["form"]?.key == "This plan has changed. Refresh it before marking this step."
                    )
                )
    }
}

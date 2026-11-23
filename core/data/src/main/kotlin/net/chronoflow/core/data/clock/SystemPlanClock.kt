package net.chronoflow.core.data.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import net.chronoflow.core.plan.PlanClock
import java.time.Instant

/**
 * Port of iOS `SystemPlanClock` and its `MinuteClockDriver`: emits on collection, at every wall minute and on time, zone
 * and date changes. The receiver exists only while the flow is collected; the page collects while the app is started.
 */
class SystemPlanClock(
    context: Context,
    private val wallMs: () -> Long = System::currentTimeMillis,
) : PlanClock {
    private val app = context.applicationContext

    override fun values(): Flow<Instant> =
        callbackFlow {
            val lock = Any()
            var timer: Job? = null
            var lastEmittedMs: Long? = null

            fun refresh(event: ClockEvent) {
                synchronized(lock) {
                    val now = wallMs()
                    if (shouldEmit(event, lastEmittedMs, now)) {
                        lastEmittedMs = now
                        trySend(Instant.ofEpochMilli(now))
                    }
                    timer?.cancel()
                    timer =
                        launch {
                            delay(delayToNextMinuteMs(now))
                            refresh(ClockEvent.TIMER)
                        }
                }
            }

            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context,
                        intent: Intent,
                    ) {
                        val event =
                            when (intent.action) {
                                Intent.ACTION_TIME_TICK -> ClockEvent.TIME_TICK
                                Intent.ACTION_TIME_CHANGED -> ClockEvent.TIME_CHANGED
                                Intent.ACTION_TIMEZONE_CHANGED -> ClockEvent.TIMEZONE_CHANGED
                                Intent.ACTION_DATE_CHANGED -> ClockEvent.DATE_CHANGED
                                else -> return
                            }
                        refresh(event)
                    }
                }
            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_TIME_TICK)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    addAction(Intent.ACTION_DATE_CHANGED)
                }
            ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            refresh(ClockEvent.START)
            awaitClose {
                synchronized(lock) { timer?.cancel() }
                app.unregisterReceiver(receiver)
            }
        }.conflate()
}

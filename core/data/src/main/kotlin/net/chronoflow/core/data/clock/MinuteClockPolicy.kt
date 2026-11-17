package net.chronoflow.core.data.clock

enum class ClockEvent { START, TIMER, TIME_TICK, TIME_CHANGED, TIMEZONE_CHANGED, DATE_CHANGED }

private const val MINUTE_MS = 60_000L

// The 15 ms put the wake-up after the boundary: a timer that fires early would show the old minute for a whole minute.
private const val AFTER_BOUNDARY_MS = 15L

fun delayToNextMinuteMs(nowMs: Long): Long = MINUTE_MS - Math.floorMod(nowMs, MINUTE_MS) + AFTER_BOUNDARY_MS

/**
 * iOS emits on every refresh; Android has two sources for the same minute (the system tick and the timer), so the
 * minute sources emit once per minute. Time, zone and date changes and the start always emit.
 */
fun shouldEmit(
    event: ClockEvent,
    lastEmittedMs: Long?,
    nowMs: Long,
): Boolean =
    when (event) {
        ClockEvent.TIMER, ClockEvent.TIME_TICK -> {
            lastEmittedMs == null || Math.floorDiv(nowMs, MINUTE_MS) != Math.floorDiv(lastEmittedMs, MINUTE_MS)
        }

        ClockEvent.START, ClockEvent.TIME_CHANGED, ClockEvent.TIMEZONE_CHANGED, ClockEvent.DATE_CHANGED -> {
            true
        }
    }

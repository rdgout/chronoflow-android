package net.chronoflow.core.model.auth

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class LockoutWaitTests(
    private val seconds: Int,
    private val unit: LockoutWait.Unit,
    private val count: Int,
) {
    @Test fun roundsUpAtUnitBoundaries() {
        val wait = LockoutWait(seconds)
        assertEquals(unit, wait.unit)
        assertEquals(count, wait.count)
        val suffix =
            when (unit) {
                LockoutWait.Unit.Seconds -> "seconds"
                LockoutWait.Unit.Minutes -> "minutes"
                LockoutWait.Unit.Hours -> "hours"
            }
        assertEquals("Too many attempts. Try again in %lld $suffix.", wait.localizationKey)
    }

    @Test fun negativeSecondsClampToZero() {
        val wait = LockoutWait(-5)
        assertEquals(LockoutWait.Unit.Seconds, wait.unit)
        assertEquals(0, wait.count)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "seconds={0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(0, LockoutWait.Unit.Seconds, 0),
                arrayOf(1, LockoutWait.Unit.Seconds, 1),
                arrayOf(45, LockoutWait.Unit.Seconds, 45),
                arrayOf(59, LockoutWait.Unit.Seconds, 59),
                arrayOf(60, LockoutWait.Unit.Minutes, 1),
                arrayOf(61, LockoutWait.Unit.Minutes, 2),
                arrayOf(2818, LockoutWait.Unit.Minutes, 47),
                arrayOf(3599, LockoutWait.Unit.Minutes, 60),
                arrayOf(3600, LockoutWait.Unit.Hours, 1),
                arrayOf(3601, LockoutWait.Unit.Hours, 2),
            )
    }
}

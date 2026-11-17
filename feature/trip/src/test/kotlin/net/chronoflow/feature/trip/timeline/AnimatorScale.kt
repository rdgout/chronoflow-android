package net.chronoflow.feature.trip.timeline

import android.animation.ValueAnimator

/** Reduce Motion in a test: `ValueAnimator.areAnimatorsEnabled()` is false at scale 0. The setter is hidden API. */
object AnimatorScale {
    fun set(scale: Float) {
        ValueAnimator::class.java.getMethod("setDurationScale", Float::class.javaPrimitiveType).invoke(null, scale)
    }
}

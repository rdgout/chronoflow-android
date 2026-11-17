package net.chronoflow.feature.launch.model

import net.chronoflow.core.model.launch.SessionPresence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class MoonAnchorStoreTests {
    @Test fun publishesValidChangedFramesAndRejectsInvalidGeometry() {
        val store = MoonAnchorStore()
        val frames = mutableListOf<LaunchRect>()
        store.onChange = { frames.add(it) }
        listOf(
            LaunchRect(0.0, 0.0, 0.0, 0.0),
            LaunchRect(Double.POSITIVE_INFINITY, 0.0, 28.0, 28.0),
            LaunchRect(0.0, 0.0, Double.POSITIVE_INFINITY, 28.0),
            LaunchRect(Double.NaN, 0.0, 28.0, 28.0),
            LaunchRect(0.0, 0.0, -28.0, 28.0),
        ).forEach(store::update)
        assertNull(store.frame)
        val first = LaunchRect(24.0, 64.0, 28.0, 28.0)
        store.update(first)
        store.update(first)
        store.update(first.offset(31.0, 16.0))
        assertEquals(listOf(first, first.offset(31.0, 16.0)), frames)
        assertEquals(frames.last(), store.frame)
    }

    @Test fun measuredAnchorRebuildsTheReducerBeforeFlightOnly() {
        val store = MoonAnchorStore()
        var state = reduce(LaunchState(), LaunchEvent.Session(SessionPresence.SignedOut), false).state
        val effects = mutableListOf<LaunchEffect>()
        store.onChange = {
            val step = reduce(state, LaunchEvent.MoonAnchor(it), false)
            state = step.state
            effects.addAll(step.effects)
        }
        val first = LaunchRect(24.0, 64.0, 28.0, 28.0)
        val changed = first.offset(31.0, 16.0)
        store.update(first)
        store.update(changed)
        assertEquals(2, effects.size)
        assertEquals(changed, state.moonTarget)
        state = reduce(state, LaunchEvent.Reached(1600.milliseconds), false).state
        store.update(first)
        assertEquals(2, effects.size)
        assertEquals(changed, state.moonTarget)
    }
}

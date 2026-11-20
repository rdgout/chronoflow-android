package net.chronoflow.core.testing.wizard

import java.time.Instant

class WizardSearchProbe {
    val queries = mutableListOf<String>()
    val times = mutableListOf<Instant>()

    fun record(
        query: String,
        at: Instant,
    ) {
        queries.add(query)
        times.add(at)
    }
}

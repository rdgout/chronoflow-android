package net.chronoflow.feature.settings

object SettingsDebugCounters {
    val languageSaved =
        java.util.concurrent.atomic
            .AtomicInteger()

    fun presented() {
        languageSaved.incrementAndGet()
    }
}

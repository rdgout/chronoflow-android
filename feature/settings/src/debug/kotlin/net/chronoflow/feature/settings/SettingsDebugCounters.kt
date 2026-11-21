package net.chronoflow.feature.settings

object SettingsDebugCounters {
    val securityFeedback =
        java.util.concurrent.atomic
            .AtomicInteger()

    fun securityPresented() {
        securityFeedback.incrementAndGet()
    }

    val languageSaved =
        java.util.concurrent.atomic
            .AtomicInteger()

    fun presented() {
        languageSaved.incrementAndGet()
    }
}

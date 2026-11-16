package net.chronoflow.core.testing.junit

enum class TierName {
    Quick,
    Layout,
    Test,
    Snapshot,
    Capture,
    Timing,
    Targeted,
    ;

    /** [classTier] is the simple name of the class's tier annotation, or null for a plain JVM class. */
    fun includes(classTier: String?): Boolean =
        when (this) {
            Quick -> classTier == null || classTier == "Logic"
            Layout -> classTier == "Layout"
            Test -> classTier == null || classTier == "Logic" || classTier == "Layout"
            Snapshot -> classTier == "Snapshot"
            Capture -> classTier == "Capture"
            Timing -> classTier == "TimingGate"
            Targeted -> true
        }

    companion object {
        fun parse(raw: String?): TierName {
            if (raw == null) return Quick
            val match = entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            if (match == null) System.err.println("cf.tier=$raw unknown; using quick")
            return match ?: Quick
        }
    }
}

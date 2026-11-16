package net.chronoflow.core.testing.junit

private const val TIER_PACKAGE = "net.chronoflow.core.testing.tiers."
private val tierNames = setOf("Logic", "Layout", "Snapshot", "Capture", "Smoke", "Perf", "TimingGate")
internal val robolectricTierNames = setOf("Logic", "Layout", "Snapshot", "Capture")

internal fun Class<*>.annotationNames(): Set<String> = annotations.map { it.annotationClass.java.name }.toSet()

/** The simple name of the class's tier annotation, matched by qualified name so no compile dependency is needed. */
internal fun Class<*>.tierName(): String? =
    annotationNames()
        .filter { it.startsWith(TIER_PACKAGE) }
        .map { it.removePrefix(TIER_PACKAGE) }
        .firstOrNull { it in tierNames }

internal fun Class<*>.hasPerfBudget(): Boolean = (TIER_PACKAGE + "PerfBudget") in annotationNames()

internal fun java.lang.reflect.Method.hasPerfBudget(): Boolean =
    annotations.any { it.annotationClass.java.name == TIER_PACKAGE + "PerfBudget" }

internal fun Class<*>.isRobolectric(): Boolean {
    if (tierName() in robolectricTierNames) return true
    val names = annotationNames()
    if ("org.robolectric.annotation.Config" in names || "org.robolectric.annotation.GraphicsMode" in names) return true
    val runWith = annotations.firstOrNull { it.annotationClass.java.name == "org.junit.runner.RunWith" } ?: return false
    val runner =
        try {
            runWith.annotationClass.java
                .getMethod("value")
                .invoke(runWith) as? Class<*>
        } catch (_: ReflectiveOperationException) {
            null
        }
    val name = runner?.name.orEmpty()
    return "Robolectric" in name || name == "androidx.test.ext.junit.runners.AndroidJUnit4"
}

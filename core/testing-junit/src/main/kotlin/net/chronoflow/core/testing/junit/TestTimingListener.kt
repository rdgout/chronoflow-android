package net.chronoflow.core.testing.junit

import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

internal const val JVM_BUDGET_MS = 1_000L
internal const val ROBOLECTRIC_BUDGET_MS = 1_000L
internal const val ROBOLECTRIC_WARMUP_MS = 20_000L
internal const val ROBOLECTRIC_CLASS_WARMUP_MS = 3_000L

/** Returns the budget in ms (null when exempt) and its warm-up, pass or failure status. */
internal fun classify(
    kind: String,
    sdk: String,
    ms: Long,
    perfBudget: Boolean,
    firstForSdk: Boolean,
    firstForClass: Boolean = false,
): Pair<Long?, String> {
    if (perfBudget) return null to "exempt"
    if (kind == "robolectric" && firstForSdk) {
        return ROBOLECTRIC_WARMUP_MS to if (ms > ROBOLECTRIC_WARMUP_MS) "over" else "warmup"
    }
    if (kind == "robolectric" && firstForClass) {
        return ROBOLECTRIC_CLASS_WARMUP_MS to if (ms > ROBOLECTRIC_CLASS_WARMUP_MS) "over" else "class-warmup"
    }
    val budget = if (kind == "robolectric") ROBOLECTRIC_BUDGET_MS else JVM_BUDGET_MS
    return budget to if (ms > budget) "over" else "ok"
}

private val sdkMarker = Regex("""\[(\d+)](?=\[|$)""")

internal fun sdkFromDisplayName(name: String): String = sdkMarker.find(name)?.groupValues?.get(1) ?: "default"

class TestTimingListener(
    private val nowNanos: () -> Long = System::nanoTime,
) : TestExecutionListener {
    private data class TestStart(
        val nanos: Long,
        val source: MethodSource?,
        val testClass: Class<*>?,
        val sdk: String,
        val firstForSdk: Boolean,
        val firstForClass: Boolean,
    )

    private val tier = System.getProperty("cf.tier") ?: "quick"
    private val outDir = System.getProperty("cf.timingsDir")
    private val watchdog =
        hangCapMillis(tier, System.getProperty("cf.testHangCapSeconds"))?.let {
            TestHangWatchdog(it, File(outDir ?: "build/cf-timings/test"))
        }
    private val starts = ConcurrentHashMap<String, TestStart>()
    private val seenSdks = ConcurrentHashMap.newKeySet<String>()
    private val seenClassSdks = ConcurrentHashMap.newKeySet<Pair<String, String>>()
    private val entries = java.util.Collections.synchronizedList(mutableListOf<String>())

    override fun executionStarted(identifier: TestIdentifier) {
        if (identifier.isTest) {
            val source = identifier.source.orElse(null) as? MethodSource
            val clazz =
                source?.let {
                    try {
                        Class.forName(it.className, false, Thread.currentThread().contextClassLoader)
                    } catch (_: ClassNotFoundException) {
                        null
                    } catch (_: LinkageError) {
                        null
                    }
                }
            val sdk = sdkFromDisplayName(identifier.displayName)
            val first = clazz?.isRobolectric() == true && seenSdks.add(sdk)
            val firstForClass = clazz?.isRobolectric() == true && seenClassSdks.add(clazz.name to sdk)
            starts[identifier.uniqueId] = TestStart(nowNanos(), source, clazz, sdk, first, firstForClass)
            watchdog?.started(identifier.uniqueId, first)
        }
    }

    override fun executionFinished(
        identifier: TestIdentifier,
        result: org.junit.platform.engine.TestExecutionResult,
    ) {
        if (!identifier.isTest) return
        watchdog?.finished(identifier.uniqueId)
        val start = starts.remove(identifier.uniqueId) ?: return
        val ended = nowNanos()
        val ms = (ended - start.nanos) / 1_000_000
        val source = start.source ?: return
        val clazz = start.testClass ?: return
        val kind = if (clazz.isRobolectric()) "robolectric" else "jvm"
        val sdk = start.sdk
        val exempt =
            clazz.hasPerfBudget() || clazz.tierName() == "TimingGate" ||
                clazz.methods.any { it.name == source.methodName.substringBefore('[') && it.hasPerfBudget() }
        val (budget, status) = classify(kind, sdk, ms, exempt, start.firstForSdk, start.firstForClass)
        entries +=
            "{\"class\":${quote(source.className)},\"method\":${quote(source.methodName)},\"kind\":\"$kind\"," +
            "\"sdk\":\"$sdk\",\"ms\":$ms,\"budgetMs\":${budget ?: "null"},\"status\":\"$status\",\"startedNanos\":${start.nanos},\"endedNanos\":$ended}"
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        watchdog?.close()
        val dir = outDir ?: return
        try {
            val folder = File(dir)
            folder.mkdirs()
            File(folder, "${ProcessHandle.current().pid()}.json")
                .writeText(
                    "{\"tier\":${quote(tier)},\"tests\":[${synchronized(entries) { entries.joinToString(",") }}]}\n",
                )
        } catch (e: IOException) {
            System.err.println("cf timings not written: $e")
        }
    }

    private fun quote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}

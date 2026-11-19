package net.chronoflow.core.testing.junit

import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

internal const val JVM_BUDGET_MS = 1_000L
internal const val ROBOLECTRIC_BUDGET_MS = 5_000L
internal const val ROBOLECTRIC_WARMUP_MS = 20_000L

/** Returns the budget in ms (null when exempt) and the status: ok, over, warmup or exempt. */
internal fun classify(
    kind: String,
    sdk: String,
    ms: Long,
    perfBudget: Boolean,
    firstForSdk: Boolean,
): Pair<Long?, String> {
    if (perfBudget) return null to "exempt"
    if (kind == "robolectric" && firstForSdk) {
        return ROBOLECTRIC_WARMUP_MS to if (ms > ROBOLECTRIC_WARMUP_MS) "over" else "warmup"
    }
    val budget = if (kind == "robolectric") ROBOLECTRIC_BUDGET_MS else JVM_BUDGET_MS
    return budget to if (ms > budget) "over" else "ok"
}

private val sdkSuffix = Regex("""\[(\d+)]$""")

class TestTimingListener : TestExecutionListener {
    private val tier = System.getProperty("cf.tier") ?: "quick"
    private val outDir = System.getProperty("cf.timingsDir")
    private val watchdog =
        hangCapMillis(tier, System.getProperty("cf.testHangCapSeconds"))?.let {
            TestHangWatchdog(it, File(outDir ?: "build/cf-timings/test"))
        }
    private val starts = ConcurrentHashMap<String, Long>()
    private val seenSdks = ConcurrentHashMap.newKeySet<String>()
    private val entries = java.util.Collections.synchronizedList(mutableListOf<String>())

    override fun executionStarted(identifier: TestIdentifier) {
        if (identifier.isTest) {
            watchdog?.started(identifier.uniqueId)
            starts[identifier.uniqueId] = System.nanoTime()
        }
    }

    override fun executionFinished(
        identifier: TestIdentifier,
        result: org.junit.platform.engine.TestExecutionResult,
    ) {
        if (!identifier.isTest) return
        watchdog?.finished(identifier.uniqueId)
        val start = starts.remove(identifier.uniqueId) ?: return
        val ms = (System.nanoTime() - start) / 1_000_000
        val source = identifier.source.orElse(null) as? MethodSource ?: return
        val clazz =
            try {
                Class.forName(source.className, false, Thread.currentThread().contextClassLoader)
            } catch (_: ClassNotFoundException) {
                return
            } catch (_: LinkageError) {
                return
            }
        val kind = if (clazz.isRobolectric()) "robolectric" else "jvm"
        val sdk = sdkSuffix.find(identifier.displayName)?.groupValues?.get(1) ?: "default"
        val exempt =
            clazz.hasPerfBudget() || clazz.tierName() == "TimingGate" ||
                clazz.methods.any { it.name == source.methodName.substringBefore('[') && it.hasPerfBudget() }
        val first = kind == "robolectric" && seenSdks.add(sdk)
        val (budget, status) = classify(kind, sdk, ms, exempt, first)
        entries +=
            "{\"class\":${quote(source.className)},\"method\":${quote(source.methodName)},\"kind\":\"$kind\"," +
            "\"sdk\":\"$sdk\",\"ms\":$ms,\"budgetMs\":${budget ?: "null"},\"status\":\"$status\"}"
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

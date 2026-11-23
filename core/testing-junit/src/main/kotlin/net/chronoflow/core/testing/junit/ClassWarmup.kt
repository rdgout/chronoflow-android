package net.chronoflow.core.testing.junit

import java.nio.file.Files
import java.nio.file.Path

fun <T> measureClassWarmup(
    testClass: Class<*>,
    sdk: Int,
    name: String,
    block: () -> T,
): T = measureClassWarmup(testClass, sdk, name, System::nanoTime, block)

internal fun <T> measureClassWarmup(
    testClass: Class<*>,
    sdk: Int,
    name: String,
    nowNanos: () -> Long,
    block: () -> T,
): T {
    val started = nowNanos()
    try {
        return block()
    } finally {
        val ended = nowNanos()
        val ms = (ended - started) / 1_000_000
        System.getProperty("cf.timingsDir")?.let { directory ->
            val folder = Files.createDirectories(Path.of(directory))
            // Separate files bridge Robolectric's sandbox class loader and the listener's class loader.
            val file = Files.createTempFile(folder, "warmup-${ProcessHandle.current().pid()}-", ".json")
            val entry = "{\"class\":${quote(
                testClass.name,
            )},\"sdk\":\"$sdk\",\"name\":${quote(name)},\"ms\":$ms,\"startedNanos\":$started,\"endedNanos\":$ended}"
            Files.writeString(file, "{\"tests\":[],\"warmups\":[$entry]}\n")
        }
    }
}

private fun quote(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

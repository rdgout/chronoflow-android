package net.chronoflow.tools.tokengen

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Writes every output atomically, or with [check] compares each with the committed file and throws one
 * [GenerationError] naming every stale or missing path. The expected bytes land in `tools/tokengen/build/verify`.
 */
fun writeOutputs(
    outputs: Map<String, ByteArray>,
    root: Path,
    check: Boolean,
) {
    val stale = mutableListOf<String>()
    for (path in outputs.keys.sorted()) {
        val bytes = outputs.getValue(path)
        if (check) {
            atomicWrite(root.resolve("tools/tokengen/build/verify/$path"), bytes)
            val file = root.resolve(path)
            if (!Files.isRegularFile(file) || !Files.readAllBytes(file).contentEquals(bytes)) stale += path
        } else {
            atomicWrite(root.resolve(path), bytes)
        }
    }
    if (stale.isNotEmpty()) throw GenerationError(stale.joinToString("\n") { "stale generated output: $it" })
}

private fun atomicWrite(
    target: Path,
    bytes: ByteArray,
) {
    Files.createDirectories(target.parent)
    val temp = Files.createTempFile(target.parent, ".tokengen", ".tmp")
    Files.write(temp, bytes)
    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
}

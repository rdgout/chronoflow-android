package net.chronoflow.tools.tokengen

import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val rootIndex = args.indexOf("--root")
    val stampIndex = args.indexOf("--stamp")
    if (rootIndex < 0 || rootIndex + 1 >= args.size) {
        System.err.println("TokenGen: --root <repo> is required")
        exitProcess(1)
    }
    val root = Path.of(args[rootIndex + 1]).toAbsolutePath()
    val check = "--check" in args
    try {
        val outputs = tokenOutputs(root)
        writeOutputs(outputs, root, check)
        println("TokenGen: ${outputs.size} outputs ${if (check) "verified" else "written"}")
        if (check && stampIndex >= 0) {
            val stamp = Path.of(args[stampIndex + 1])
            Files.createDirectories(stamp.parent)
            Files.writeString(stamp, "ok\n")
        }
    } catch (error: GenerationError) {
        System.err.println("TokenGen: ${error.message}")
        exitProcess(1)
    }
}

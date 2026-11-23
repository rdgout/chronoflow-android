package net.chronoflow.tools.tokengen

import java.nio.file.Files
import java.nio.file.Path

val repoRoot: Path = Path.of(System.getProperty("cf.repoRoot") ?: error("cf.repoRoot is not set"))

fun iosGolden(name: String): String = Files.readString(repoRoot.resolve("tools/tokengen/src/test/resources/ios/$name"))

/** Joins every run of whitespace into one space, so wrapped Swift calls become one line. */
fun joined(text: String): String = text.replace(Regex("\\s+"), " ")

class ReportRow(
    val name: String,
    val lightHex: String,
    val lightP3: String,
    val darkHex: String,
    val darkP3: String,
)

fun parseReport(text: String): List<ReportRow> =
    text.lines().filter { it.startsWith("| ") && !it.startsWith("| Token") }.map { line ->
        val cells =
            line
                .trim()
                .trim('|')
                .split("|")
                .map { it.trim() }
        ReportRow(cells[0], cells[1], cells[2], cells[3], cells[4])
    }

fun p3Channels(cell: String): List<Double> = cell.split(" ").map { it.toDouble() }

val sources: DesignSource by lazy { DesignSource(repoRoot) }

/** A copy of the given repo-relative files in a fresh directory, to plant a change in one of them. */
fun tempRoot(
    files: List<String>,
    change: (String, String) -> String = { _, text -> text },
): Path {
    val dir = Files.createTempDirectory("tokengen-probe")
    for (file in files) {
        val target = dir.resolve(file)
        Files.createDirectories(target.parent)
        Files.writeString(target, change(file, Files.readString(repoRoot.resolve(file))))
    }
    return dir
}

fun Path.deleteRecursively() {
    Files.walk(this).sorted(Comparator.reverseOrder()).forEach { Files.delete(it) }
}

package net.chronoflow.buildlogic.size

enum class Verdict { OK, WARN, FAIL }

object BundleSize {
    const val WARN_BYTES = 8_000_000L
    const val FAIL_BYTES = 12_000_000L

    fun parseGetSizeTotal(csv: String): Long {
        val lines = csv.lines().map { it.trim() }.filter { it.isNotEmpty() }
        require(lines.size >= 2 && lines[0] == "MIN,MAX") { "unexpected get-size output: '$csv'" }
        val parts = lines[1].split(',')
        require(parts.size == 2) { "unexpected get-size row: '${lines[1]}'" }
        return parts[1].trim().toLongOrNull() ?: throw IllegalArgumentException("MAX is not a number: '${parts[1]}'")
    }

    fun verdict(bytes: Long): Verdict =
        when {
            bytes > FAIL_BYTES -> Verdict.FAIL
            bytes > WARN_BYTES -> Verdict.WARN
            else -> Verdict.OK
        }

    // androidx.graphics:graphics-path ships with Compose UI and cannot be dropped (OQ-0a-22).
    val allowedNativeLibraries = setOf("libandroidx.graphics.path.so")

    private val languageSplit = Regex("""^splits/base-[a-z]{2,3}(_[A-Z]{2})?\.apk$""")

    fun languageSplits(apksEntryNames: List<String>): List<String> = apksEntryNames.filter(languageSplit::matches)

    fun nativeLibraries(
        entryNames: List<String>,
        allowed: Set<String> = emptySet(),
    ): List<String> = entryNames.filter { it.endsWith(".so") && it.substringAfterLast('/') !in allowed }
}

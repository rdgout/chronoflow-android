package net.chronoflow.core.testing

object Fixtures {
    fun bytes(path: String): ByteArray =
        Fixtures::class.java.getResourceAsStream("/fixtures/$path")?.use { it.readBytes() }
            ?: throw IllegalArgumentException("missing fixture: $path")

    fun text(path: String): String = bytes(path).decodeToString()

    fun manifest(): List<FixtureEntry> =
        text("fixtures.sha256")
            .lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map {
                val (sha256, resource, iosSource) = it.split("  ")
                FixtureEntry(sha256, resource, iosSource)
            }
}

data class FixtureEntry(
    val sha256: String,
    val resource: String,
    val iosSource: String,
)

package net.chronoflow.buildlogic

import java.util.Properties

data class AppVersion(val name: String, val code: Int)

object VersionProperties {
    fun parse(text: String): AppVersion {
        val props = Properties().apply { load(text.reader()) }
        val major = read(props, "versionMajor", 0..2099)
        val minor = read(props, "versionMinor", 0..99)
        val patch = read(props, "versionPatch", 0..99)
        val build = read(props, "versionBuild", 0..99)
        return AppVersion(
            name = "$major.$minor.$patch",
            code = major * 1_000_000 + minor * 10_000 + patch * 100 + build,
        )
    }

    private fun read(props: Properties, key: String, range: IntRange): Int {
        val raw = props.getProperty(key)?.trim()
            ?: throw IllegalArgumentException("version.properties: $key is missing")
        val value = raw.toIntOrNull()
            ?: throw IllegalArgumentException("version.properties: $key must be a number, was '$raw'")
        require(value in range) { "version.properties: $key must be in $range, was $value" }
        return value
    }
}

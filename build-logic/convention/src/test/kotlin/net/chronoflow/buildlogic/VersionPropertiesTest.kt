package net.chronoflow.buildlogic

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import org.junit.Test

class VersionPropertiesTest {
    private fun props(major: String = "0", minor: String = "1", patch: String = "0", build: String = "0") =
        "versionMajor=$major\nversionMinor=$minor\nversionPatch=$patch\nversionBuild=$build\n"

    @Test
    fun firstVersionIsZeroOneZero() {
        assertThat(VersionProperties.parse(props())).isEqualTo(AppVersion("0.1.0", 10000))
    }

    @Test
    fun codeCombinesAllFourParts() {
        assertThat(VersionProperties.parse(props("1", "2", "3", "4"))).isEqualTo(AppVersion("1.2.3", 1020304))
    }

    @Test
    fun missingKeyNamesTheKey() {
        assertFailure { VersionProperties.parse("versionMajor=0\nversionMinor=1\nversionPatch=0\n") }
            .isInstanceOf<IllegalArgumentException>()
            .messageContains("versionBuild")
    }

    @Test
    fun nonNumberNamesTheKey() {
        assertFailure { VersionProperties.parse(props(patch = "x")) }
            .isInstanceOf<IllegalArgumentException>()
            .messageContains("versionPatch")
    }

    @Test
    fun minorOverNinetyNineFails() {
        assertFailure { VersionProperties.parse(props(minor = "100")) }
            .isInstanceOf<IllegalArgumentException>()
            .messageContains("versionMinor")
    }

    @Test
    fun buildOverNinetyNineFails() {
        assertFailure { VersionProperties.parse(props(build = "100")) }
            .isInstanceOf<IllegalArgumentException>()
            .messageContains("versionBuild")
    }
}

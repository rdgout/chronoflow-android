package net.chronoflow.buildlogic.size

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Test

class BundleSizeTest {
    @Test
    fun parsesTheMaxColumn() {
        assertThat(BundleSize.parseGetSizeTotal("MIN,MAX\n123,456\n")).isEqualTo(456L)
    }

    @Test
    fun malformedOutputThrows() {
        assertFailure { BundleSize.parseGetSizeTotal("nonsense") }
    }

    @Test
    fun eightMegabytesIsStillOk() {
        assertThat(BundleSize.verdict(8_000_000)).isEqualTo(Verdict.OK)
    }

    @Test
    fun justOverEightMegabytesWarns() {
        assertThat(BundleSize.verdict(8_000_001)).isEqualTo(Verdict.WARN)
    }

    @Test
    fun justOverTwelveMegabytesFails() {
        assertThat(BundleSize.verdict(12_000_001)).isEqualTo(Verdict.FAIL)
    }

    @Test
    fun detectsNativeLibraries() {
        assertThat(BundleSize.nativeLibraries(listOf("base/dex/classes.dex", "base/lib/arm64-v8a/libx.so")))
            .containsExactly("base/lib/arm64-v8a/libx.so")
    }

    @Test
    fun allowlistedNativeLibraryIsIgnored() {
        assertThat(
            BundleSize.nativeLibraries(
                listOf("base/lib/arm64-v8a/libandroidx.graphics.path.so"),
                BundleSize.allowedNativeLibraries,
            ),
        ).isEmpty()
    }
}

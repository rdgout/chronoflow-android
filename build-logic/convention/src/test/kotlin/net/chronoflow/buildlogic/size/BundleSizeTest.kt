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

    @Test fun densityAndMasterAreNotLanguageSplits() =
        assertThat(
            BundleSize.languageSplits(listOf("toc.pb", "splits/base-master.apk", "splits/base-xxhdpi.apk")),
        ).isEmpty()

    @Test fun englishSplitIsRejected() =
        assertThat(
            BundleSize.languageSplits(listOf("splits/base-master.apk", "splits/base-en.apk")),
        ).containsExactly("splits/base-en.apk")

    @Test fun dutchAndRegionalSplitsAreRejected() =
        assertThat(
            BundleSize.languageSplits(listOf("splits/base-nl.apk", "splits/base-pt_BR.apk")),
        ).containsExactly("splits/base-nl.apk", "splits/base-pt_BR.apk")

    @Test fun abiIsNotALanguageSplit() =
        assertThat(
            BundleSize.languageSplits(listOf("splits/base-arm64_v8a.apk")),
        ).isEmpty()
}

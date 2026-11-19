package net.chronoflow.core.testing

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class FixtureManifestTest {
    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun entry(resource: String) = Fixtures.manifest().single { it.resource == resource }

    @Test
    fun everyManifestEntryMatchesItsBytes() {
        assertThat(Fixtures.text("fixtures.sha256").lines().first()).isEqualTo("# ios-pin f391a1fa")
        val mismatches =
            Fixtures
                .manifest()
                .filter {
                    sha256(
                        Fixtures.bytes(it.resource),
                    ) != it.sha256
                }.map { it.resource }
        assertThat(mismatches).isEmpty()
    }

    @Test
    fun apiFixtureDirectoriesHoldTheIosSet() {
        val counts =
            Fixtures
                .manifest()
                .map { it.resource.substringBefore('/') }
                .groupingBy { it }
                .eachCount()
        val api = listOf("account", "auth", "errors", "plans", "trips", "wizard")
        assertThat(api.map { counts[it] }).isEqualTo(listOf(2, 5, 13, 7, 3, 16))
    }

    @Test
    fun everyFixtureFileIsInTheManifest() {
        val root = File(FixtureManifestTest::class.java.getResource("/fixtures/fixtures.sha256")!!.toURI()).parentFile
        val onDisk =
            root
                .walkTopDown()
                .filter {
                    it.isFile && it.extension == "json"
                }.map { it.relativeTo(root).invariantSeparatorsPath }
                .toList()
        assertThat(onDisk).containsExactlyInAnyOrder(
            *Fixtures
                .manifest()
                .map {
                    it.resource
                }.filter { it.endsWith(".json") }
                .toTypedArray(),
        )
    }

    @Test
    fun spotHashesMatchTheIosBytes() {
        assertThat(
            entry("plans/S1.json").sha256,
        ).isEqualTo("cb2f1fa5e47997e1397985a142344139f359dceaf60043413ed00bca0c1947eb")
        assertThat(entry("plans/S6-RETURN-10N.json").sha256)
            .isEqualTo("8bb0af18c3e4099bcb8359026b50ffa16472ea3bd77f4198c8c17ec4a6f558c7")
        assertThat(
            entry("trips/detail.json").sha256,
        ).isEqualTo("1c9c87785e608df0d86dbd482568e6a4ce081aac085aed683190f7d98d71d50d")
    }
}

package net.chronoflow.core.plan

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.model.contracts.Language
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class CopyCatalogTableTests {
    private val bytes =
        checkNotNull(
            javaClass.getResourceAsStream("/plan_copy.json"),
        ) { "plan_copy.json missing" }.use { it.readBytes() }
    private val entries: JsonArray =
        Json
            .parseToJsonElement(
                bytes.decodeToString(),
            ).jsonObject
            .getValue("entries")
            .jsonArray

    private fun inline(json: String) = CopyCatalog(json.encodeToByteArray())

    @Test fun allEntriesLoadWithEnglishAndDutch() {
        val catalog = CopyCatalog(bytes)
        assertThat(entries.size).isEqualTo(289)
        val missing =
            entries.filter { entry ->
                val languages = entry.jsonObject.getValue("languages").jsonObject
                "en" !in languages || "nl" !in languages
            }
        assertThat(missing.size).isEqualTo(0)
        assertThat(
            entries.count {
                catalog.contains(
                    it.jsonObject
                        .getValue("key")
                        .jsonPrimitive.content,
                )
            },
        ).isEqualTo(289)
    }

    @Test fun formatIndexesPluralsAndCasesAreConsistent() {
        val problems = mutableListOf<String>()
        val index = Regex("%(\\d+)\\$@")
        for (entry in entries) {
            val obj = entry.jsonObject
            val key = obj.getValue("key").jsonPrimitive.content
            val argumentCount = obj.getValue("arguments").jsonArray.size
            for ((language, value) in obj.getValue("languages").jsonObject) {
                val translation = value.jsonObject
                val kind = translation.getValue("kind").jsonPrimitive.content
                val formats =
                    translation["formats"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content } +
                        translation["cases"]?.jsonArray.orEmpty().map {
                            it.jsonObject
                                .getValue(
                                    "format",
                                ).jsonPrimitive.content
                        }
                if (kind == "plural" &&
                    translation.getValue("formats").jsonArray.size != 2
                ) {
                    problems += "$key/$language: plural"
                }
                if (kind == "coded" &&
                    translation.getValue("cases").jsonArray.isEmpty()
                ) {
                    problems += "$key/$language: coded"
                }
                formats.forEach { format ->
                    if (index.findAll(format).any { it.groupValues[1].toInt() > argumentCount }) {
                        problems +=
                            "$key/$language: index"
                    }
                }
            }
        }
        assertThat(problems).isEqualTo(emptyList<String>())
    }

    @Test fun duplicateKeyThrows() {
        val entry = """{"key":"A","arguments":[],"languages":{"en":{"kind":"single","formats":["a"]}}}"""
        assertFailure { inline("""{"entries":[$entry,$entry]}""") }.isInstanceOf<IllegalArgumentException>()
    }

    @Test fun missingEnglishThrows() {
        val entry = """{"key":"A","arguments":[],"languages":{"nl":{"kind":"single","formats":["a"]}}}"""
        assertFailure { inline("""{"entries":[$entry]}""") }.isInstanceOf<IllegalArgumentException>()
    }

    @Test fun missingLanguageFallsBackToEnglish() {
        val catalog =
            inline(
                """{"entries":[{"key":"Hello :name","arguments":["name"],"languages":{"en":{"kind":"single","formats":["Hello %1$@"]}}}]}""",
            )
        assertThat(catalog.text("Hello :name", Language.Nl, mapOf("name" to "Ada"))).isEqualTo("Hello Ada")
        assertThat(catalog.text("Hello :name", Language.Unknown("fr"), mapOf("name" to "Ada"))).isEqualTo("Hello Ada")
    }

    @Test fun percentEscapesAndSequentialSubstitution() {
        val catalog =
            inline(
                """{"entries":[{"key":"K","arguments":["a","b"],"languages":{"en":{"kind":"single","formats":["%@ and %@ at 100%% %3${'$'}@ %x"]}}}]}""",
            )
        assertThat(catalog.text("K", Language.En, mapOf("a" to "1", "b" to "2"))).isEqualTo("1 and 2 at 100% %3\$@ %x")
    }

    @Test fun unknownKeyInterpolatesOnlyGivenNames() {
        val catalog = inline("""{"entries":[]}""")
        assertThat(catalog.text("Unknown :place and :place2", Language.En, mapOf("place" to "Tokyo")))
            .isEqualTo("Unknown Tokyo and :place2")
    }

    @Test fun codedCasesPickTheFirstMatch() {
        val catalog =
            inline(
                """{"entries":[{"key":"C","arguments":[],"languages":{"en":{"kind":"coded","cases":[
                {"bound":0.5,"exact":true,"format":"half"},{"bound":1,"exact":true,"format":"one"},{"bound":2,"exact":false,"format":"many"}]}}}]}""",
            )
        assertThat(listOf(0.5, 1.0, 2.0, 5.0, 0.0).map { catalog.text("C", Language.En, count = it) })
            .isEqualTo(listOf("half", "one", "many", "many", ""))
    }

    @Test fun providerLoadsOnceOnTheIoDispatcher() =
        runTest(timeout = 60.seconds) {
            val io = CountingDispatcher(StandardTestDispatcher(testScheduler))
            val provider = CopyCatalogProvider(io)
            val a = async { provider.catalog() }
            val b = async { provider.catalog() }
            val first = a.await()
            assertThat(b.await()).isSameInstanceAs(first)
            assertThat(provider.catalog()).isSameInstanceAs(first)
            assertThat(io.dispatches).isGreaterThan(0)
        }
}

package net.chronoflow.core.ui.l10n

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringParityTest {
    private class Res(
        val name: String,
        val plural: Boolean,
        val translatable: Boolean,
        val items: Map<String, String>,
    )

    private class SourceSet(
        val en: Map<String, Res>,
        val nl: Map<String, Res>,
    )

    private fun parse(dir: File): Map<String, Res> {
        val out = linkedMapOf<String, Res>()
        val files =
            dir
                .listFiles { f ->
                    f.name.startsWith("strings_") && f.extension == "xml"
                }.orEmpty()
                .sortedBy { it.name }
        for (file in files) {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val nodes = doc.documentElement.childNodes
            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as? Element ?: continue
                val name = el.getAttribute("name")
                val translatable = el.getAttribute("translatable") != "false"
                val res =
                    when (el.tagName) {
                        "string" -> {
                            Res(name, false, translatable, mapOf("" to el.textContent))
                        }

                        "plurals" -> {
                            val items = el.getElementsByTagName("item")
                            Res(
                                name,
                                true,
                                translatable,
                                (0 until items.length).associate {
                                    val item = items.item(it) as Element
                                    item.getAttribute("quantity") to item.textContent
                                },
                            )
                        }

                        else -> {
                            error("unexpected <${el.tagName}> in ${file.name}")
                        }
                    }
                check(out.put(name, res) == null) { "duplicate $name in ${dir.path}" }
            }
        }
        return out
    }

    private fun set(sourceSet: String) =
        SourceSet(parse(File("src/$sourceSet/res/values")), parse(File("src/$sourceSet/res/values-nl")))

    private val sets by lazy { listOf("main" to set("main"), "debug" to set("debug")) }

    private val placeholder = Regex("%(\\d+)\\$([sd])")

    private fun placeholders(text: String) =
        placeholder
            .findAll(text)
            .map { it.value }
            .sorted()
            .toList()

    @Test fun everyKeyHasDutchText() {
        for ((label, s) in sets) {
            val en =
                s.en.values
                    .filter { it.translatable }
                    .map { it.name }
                    .toSet()
            assertThat(s.nl.keys - en, label).isEmpty()
            assertThat(en - s.nl.keys, label).isEmpty()
            assertThat(
                s.nl.values
                    .filter { r -> r.items.values.any { it.isBlank() } }
                    .map { it.name },
                label,
            ).isEmpty()
        }
    }

    @Test fun untranslatableStringsHaveNoDutch() {
        for ((_, s) in sets) {
            val untranslatable =
                s.en.values
                    .filter { !it.translatable }
                    .map { it.name }
            assertThat(untranslatable.filter { it in s.nl }).isEmpty()
        }
    }

    @Test fun placeholdersMatchPerQuantity() {
        val bad = mutableListOf<String>()
        for ((_, s) in sets) {
            for (nl in s.nl.values) {
                val en = s.en[nl.name] ?: continue
                if (en.plural != nl.plural || en.items.keys != nl.items.keys) bad += "${nl.name}: shape"
                if (nl.plural) {
                    check(nl.items.keys == setOf("one", "other")) { "${nl.name}: quantities ${nl.items.keys}" }
                }
                for ((q, text) in en.items) {
                    if (placeholders(text) != placeholders(nl.items[q].orEmpty())) bad += "${nl.name}/$q"
                }
            }
        }
        assertThat(bad).isEmpty()
    }

    @Test fun positionsAreContiguous() {
        val bad = mutableListOf<String>()
        for ((_, s) in sets) {
            for (res in s.en.values + s.nl.values) {
                val positions =
                    res.items.values
                        .flatMap {
                            placeholder.findAll(it).map { m ->
                                m.groupValues[1].toInt()
                            }
                        }.toSortedSet()
                if (positions.isNotEmpty() &&
                    positions.toList() != (1..positions.size).toList()
                ) {
                    bad += "${res.name}: $positions"
                }
            }
        }
        assertThat(bad).isEmpty()
    }

    @Test fun noUnpositionedArgs() {
        val bare = Regex("%(?!%|\\d+\\$)[sd]")
        val bad = mutableListOf<String>()
        for ((_, s) in sets) {
            for (res in s.en.values + s.nl.values) {
                for (text in res.items.values) {
                    if (bare.containsMatchIn(text)) bad += "${res.name}: bare argument"
                    if (text.contains("%%") && placeholders(text).isEmpty() &&
                        res.items.values.none { placeholders(it).isNotEmpty() }
                    ) {
                        bad += "${res.name}: %% without placeholders"
                    }
                }
            }
        }
        assertThat(bad).isEmpty()
    }

    @Test fun countsAreRecorded() {
        val main = sets.first { it.first == "main" }.second
        val formats =
            setOf(
                "app_name",
                "plan_day_title_date",
                "plan_title_comma_date",
                "ui_refresh_feedback_separator",
                "clipboard_label",
            )
        val ios = main.en.filterKeys { !it.startsWith("android_") && it !in formats }
        assertThat(ios.values.count { !it.plural }).isEqualTo(920)
        assertThat(ios.values.count { it.plural }).isEqualTo(31)
        assertThat(
            main.nl
                .filterKeys { !it.startsWith("android_") }
                .values
                .count { !it.plural },
        ).isEqualTo(919)
    }
}

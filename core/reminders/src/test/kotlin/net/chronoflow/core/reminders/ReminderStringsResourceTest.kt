package net.chronoflow.core.reminders

import assertk.assertThat
import assertk.assertions.isEmpty
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.testing.reminders.FakeReminderStrings
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class ReminderStringsResourceTest {
    @Test fun fakeCopyMatchesGeneratedResources() {
        val root = File(checkNotNull(System.getProperty("chronoflow.repoRoot")))
        val names = ApiJson.response.parseToJsonElement(File(root, "tools/l10n/names.lock.json").readText()).jsonObject
        val strings = FakeReminderStrings()
        val mismatches = ArrayList<String>()
        for ((language, directory) in listOf(Language.En to "values", Language.Nl to "values-nl")) {
            val file = File(root, "core/ui/src/main/res/$directory/strings_ios.xml")
            val nodes =
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(file)
                    .getElementsByTagName("string")
            val resources =
                (0 until nodes.length).associate { index ->
                    val node = nodes.item(index)
                    node.attributes.getNamedItem("name").nodeValue to node.textContent
                }
            for (kind in ReminderKind.entries) {
                val name = names["reminders\t${kind.copyKey}"]?.jsonPrimitive?.content
                if (name == null) {
                    mismatches += "Missing lock name: ${kind.copyKey}"
                    continue
                }
                val actual =
                    resources[name]
                        ?.replace("%1\$s", "{{time}}")
                        ?.replace("\\'", "'")
                        ?.replace("\\\"", "\"")
                val expected = strings.text(kind.copyKey, language, "{{time}}")
                if (actual != expected) mismatches += "$directory/$name: expected $expected, got $actual"
            }
        }
        assertThat(mismatches).isEmpty()
    }
}

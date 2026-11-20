package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WizardCopyKeysTest {
    private val root = File(System.getProperty("chronoflow.repoRoot"))

    @Test fun everyModelKeyIsListed() {
        val source =
            File(root, "core/wizard/src/main/kotlin")
                .walkTopDown()
                .filter {
                    it.extension == "kt"
                }.joinToString("\n") { it.readText() }
        val calls = Regex("(?:FieldMessage\\s*\\(\\s*(?:key\\s*=\\s*)?|\\b(?:t|translate)\\s*\\(\\s*)\"([^\"\\n]+)\"")
        assertEquals(WizardModelCopyKeys.literalCalls, calls.findAll(source).map { it.groupValues[1] }.toSet())
        val phrases =
            Regex("\"([^\"\\n]*)\"")
                .findAll(source)
                .map { it.groupValues[1] }
                .filter {
                    (it.firstOrNull()?.isUpperCase() == true || it.startsWith("{{")) && ' ' in it && '$' !in it
                }.toSet()
        assertEquals(WizardModelCopyKeys.all - "Arrival", phrases + WizardModelCopyKeys.literalCalls)
    }

    @Test fun everyListedKeyResolves() {
        val names =
            ApiJson.response.parseToJsonElement(
                File(root, "tools/l10n/names.lock.json").readText(),
            ) as JsonObject
        val plan =
            ApiJson.response.parseToJsonElement(
                File(root, "core/plan/src/main/resources/plan_copy.json").readText(),
            ) as JsonObject
        val planKeys =
            (plan.getValue("entries") as JsonArray)
                .map {
                    (it as JsonObject).getValue("key").jsonPrimitive.content
                }.toSet()
        val android =
            ApiJson.response.parseToJsonElement(
                File(root, "tools/l10n/android-only.json").readText(),
            ) as JsonObject
        val androidKeys =
            (android.getValue("entries") as JsonArray)
                .map {
                    (it as JsonObject).getValue("en").jsonPrimitive.content
                }.toSet()
        val missing =
            WizardModelCopyKeys.all.filter {
                !names.containsKey("wizard\t$it") && it !in planKeys && it !in androidKeys
            }
        assertTrue("Unresolved wizard copy keys: $missing", missing.isEmpty())
    }
}

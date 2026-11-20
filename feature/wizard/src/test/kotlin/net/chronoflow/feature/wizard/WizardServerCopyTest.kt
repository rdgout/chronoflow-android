package net.chronoflow.feature.wizard

import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.fieldCopy
import net.chronoflow.feature.wizard.components.serverCopy
import org.junit.Assert.assertEquals
import org.junit.Test

class WizardServerCopyTest {
    @Test fun unknownServerKeyKeepsItsRawText() {
        val formatter = CopyLineFormatter(CopyCatalog("""{"entries":[]}""".encodeToByteArray()))
        assertEquals("future", serverCopy(CopyLine("future", emptyList()), formatter, Language.Nl))
    }

    @Test fun flightErrorResolvesThePlanCatalogWithArguments() {
        val formatter =
            CopyLineFormatter(
                CopyCatalog(
                    """{"entries":[{"key":"departure :value","arguments":["value"],"languages":{
                "en":{"kind":"string","formats":["Departure %@"]},
                "nl":{"kind":"string","formats":["Vertrek %@"]}
            }}]}""".encodeToByteArray(),
                ),
            )
        val errors = mapOf("legs.0.departLocal" to FieldMessage("departure :value", mapOf("value" to JsonPrimitive(3))))
        assertEquals("Vertrek 3", fieldCopy(errors.getValue("legs.0.departLocal"), formatter, Language.Nl) { "wizard" })
    }

    @Test fun wizardOnlyErrorKeepsItsKeyAndArguments() {
        val formatter = CopyLineFormatter(CopyCatalog("""{"entries":[]}""".encodeToByteArray()))
        val message = FieldMessage("Check {{field}}", mapOf("field" to JsonPrimitive("departure")))
        assertEquals(
            "Check {{field}}:departure",
            fieldCopy(message, formatter, Language.En) {
                "${it.key}:${it.arguments["field"]}"
            },
        )
    }

    @Test fun missingFormatterUsesTheEmptyCopyFallback() {
        assertEquals("", serverCopy(CopyLine("future", emptyList()), null, Language.En))
    }
}

package net.chronoflow.feature.wizard

import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.serverCopy
import org.junit.Assert.assertEquals
import org.junit.Test

class WizardServerCopyTest {
    @Test fun unknownServerKeyKeepsItsRawText() {
        val formatter = CopyLineFormatter(CopyCatalog("""{"entries":[]}""".encodeToByteArray()))
        assertEquals("future", serverCopy(CopyLine("future", emptyList()), formatter, Language.Nl))
    }

    @Test fun missingFormatterUsesTheEmptyCopyFallback() {
        assertEquals("", serverCopy(CopyLine("future", emptyList()), null, Language.En))
    }
}

package net.chronoflow.core.data.wizard

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class DraftRepositoryTests {
    @Test fun recoveryJsonAlwaysCarriesInstanceAndRevision() {
        val draft = MemberWizardDraft()
        val state = ApiJson.response.parseToJsonElement(MemberWizardRecord.encode(draft).state).jsonObject
        assertEquals(draft.state.instance, state["instance"]?.jsonPrimitive?.content)
        assertEquals("0", state["revision"]?.jsonPrimitive?.content)
    }
}

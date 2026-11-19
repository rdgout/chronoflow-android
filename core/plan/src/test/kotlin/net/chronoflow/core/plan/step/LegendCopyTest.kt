package net.chronoflow.core.plan.step

import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LegendCopyTest {
    @Test fun groupsCoverEveryKnownTypeOnce() {
        val types = legendGroups.flatMap { it.second }
        assertEquals(17, types.size)
        assertEquals(17, types.toSet().size)
        assertFalse(types.any { it is BlockType.Unknown })
    }

    @Test fun descriptionsComeFromIosCatalog() {
        val names =
            ApiJson.response
                .parseToJsonElement(
                    File(System.getProperty("chronoflow.repoRoot"), "tools/l10n/names.lock.json").readText(),
                ).jsonObject
        assertTrue(
            legendGroups.flatMap { it.second }.all {
                names.containsKey("screens\t" + legendDescription(it))
            },
        )
    }
}

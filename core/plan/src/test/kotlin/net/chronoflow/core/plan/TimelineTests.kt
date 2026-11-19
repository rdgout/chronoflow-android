package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class TimelineTests {
    // The layout half of the iOS test (layoutPlan, tiles, ticks) joins this method in Phase 1b.
    @Test fun everyFixturePreservesLayoutAndResolvesCopy() {
        val failures = mutableListOf<String>()
        var lines = 0
        for (name in listOf("S1", "S2", "S3", "S4", "S5", "S6", "S6-RETURN-10N")) {
            val decodedTwice =
                ApiJson.response.decodeFromString(
                    PlanFixture.serializer(),
                    Fixtures.text("plans/$name.json"),
                )
            val original = fixture(name).plan
            if (decodedTwice.plan != original) failures += "$name: decode is not repeatable"
            val once = withoutOptionalSteps(original)
            if (withoutOptionalSteps(once) != once) failures += "$name: withoutOptionalSteps is not idempotent"
            if (original != fixture(name).plan) failures += "$name: plan mutated"
            val root = ApiJson.response.parseToJsonElement(Fixtures.text("plans/$name.json")).jsonObject
            lines += unresolvedCopyLines(root.getValue("plan"), name, failures)
        }
        assertThat(failures).isEqualTo(emptyList<String>())
        assertThat(lines).isEqualTo(1_630)
    }
}

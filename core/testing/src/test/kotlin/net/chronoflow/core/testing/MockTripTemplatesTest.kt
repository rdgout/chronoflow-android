package net.chronoflow.core.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import kotlinx.serialization.Serializable
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.PlanNoticesView
import net.chronoflow.core.model.contracts.PlanView
import net.chronoflow.core.model.contracts.TimelinePresentationView
import org.junit.Test

class MockTripTemplatesTest {
    @Serializable
    private class PlanFile(
        val plan: PlanView,
        val timeline: TimelinePresentationView,
        val notices: PlanNoticesView,
    )

    private fun planFile(name: String) =
        ApiJson.response.decodeFromString(PlanFile.serializer(), Fixtures.text("plans/$name.json"))

    @Test fun sevenTemplatesWithTheIosIds() {
        val templates = MockTripTemplates.templates()
        assertThat(templates.size).isEqualTo(7)
        assertThat(templates.map { it.card.id })
            .isEqualTo((1..7).map { "0199a3c0-0000-7000-8000-00000000000$it" })
    }

    @Test fun templateTwoIsTheSydneyTrip() {
        val s3 = MockTripTemplates.templates()[2]
        assertThat(s3.card.to.iata).isEqualTo("SYD")
        assertThat(s3.detail.plan.days.size).isEqualTo(18)
        assertThat(s3.detail.plan.blocks.size).isEqualTo(138)
        assertThat(s3.detail.detailVersion).startsWith("288dd5aa")
    }

    @Test fun templateZeroIsTheTokyoTrip() {
        val s1 = MockTripTemplates.templates()[0]
        assertThat(s1.detail.plan.days.size).isEqualTo(13)
        assertThat(s1.detail.plan.blocks.size).isEqualTo(139)
    }

    @Test fun templatesMatchThePlanFixtures() {
        for ((index, name) in listOf(0 to "S1", 2 to "S3")) {
            val detail = MockTripTemplates.templates()[index].detail
            val file = planFile(name)
            assertThat(detail.plan).isEqualTo(file.plan)
            assertThat(detail.notices).isEqualTo(file.notices)
            assertThat(detail.timeline).isEqualTo(file.timeline)
        }
    }
}

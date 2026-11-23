package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class TripWizardErrorsTests {
    @Test fun camelKeepsEmptyPieces() {
        assertThat(TripWizardErrors.camel("a__b")).isEqualTo("aB")
        assertThat(TripWizardErrors.camel("_a")).isEqualTo("A")
        assertThat(TripWizardErrors.camel("abc")).isEqualTo("abc")
    }

    @Test fun mapReturnsDomainFailuresFirst() {
        assertThat(mapped("review_expired", "x")).isEqualTo(DataFailure.ReviewExpired)
        assertThat(mapped("credits", "Not enough credits")).isEqualTo(DataFailure.NotEnoughCredits)
        assertThat(mapped("credits", "other")).isEqualTo(validation("credits", "other"))
        assertThat(mapped("review", "Try saving again shortly.")).isEqualTo(DataFailure.SaveCapacity)
        assertThat(TripWizardErrors.map(DataFailure.Offline)).isEqualTo(DataFailure.Offline)
        assertThat(TripWizardErrors.map(DataFailure.Server(500))).isEqualTo(DataFailure.Server(500))
    }

    @Test fun mapRewritesOnlyWithAnOutboundCount() {
        val profile = fixture("profile")
        assertThat((TripWizardErrors.map(profile) as DataFailure.ProfileInvalid).fields.keys)
            .isEqualTo(setOf("profile", "profile.age_band", "profile.habitual_bed"))
        assertThat((TripWizardErrors.map(profile, 1) as DataFailure.ProfileInvalid).fields.keys)
            .isEqualTo(setOf("profile", "profile.ageBand", "profile.habitualBed"))
        val journey = TripWizardErrors.map(fixture("journey"), 1) as DataFailure.Validation
        assertThat(
            journey.fields.keys,
        ).isEqualTo(setOf("stays.0.bedAvailableFromLocal", "legs.1.arriveLocal", "end.nights"))
        assertThat(journey.status).isEqualTo(422)
    }

    @Test fun collisionsKeepTheFirstMessage() {
        val fields =
            linkedMapOf(
                "profile.habitual_bed" to FieldMessage("first"),
                "profile.habitualBed" to FieldMessage("second"),
            )
        assertThat(TripWizardErrors.map(DataFailure.Validation(fields, 422), 1))
            .isEqualTo(DataFailure.ProfileInvalid(mapOf("profile.habitualBed" to FieldMessage("first"))))
    }

    private fun validation(
        field: String,
        key: String,
    ) = DataFailure.Validation(mapOf(field to FieldMessage(key)), 422)

    private fun mapped(
        field: String,
        key: String,
    ) = TripWizardErrors.map(validation(field, key))

    private fun fixture(name: String): DataFailure.Validation {
        val envelope =
            ApiJson.response.decodeFromString(
                ApiErrorEnvelope.serializer(),
                Fixtures.text("wizard/$name.json"),
            )
        return DataFailure.Validation(envelope.errors!!.mapValues { FieldMessage(it.value) }, 422)
    }
}

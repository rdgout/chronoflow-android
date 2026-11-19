package net.chronoflow.core.datamock

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.ApiGuestTripReviewInput
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripInputOverride
import net.chronoflow.core.model.contracts.ProfilePreferences
import java.security.SecureRandom
import java.time.Instant

@Serializable
data class MockGuestDraft(
    val deviceID: String,
    val secretHash: String,
    val draft: MockWizardDraft,
) {
    override fun toString(): String = "MockGuestDraft(***)"
}

object MockGuestReview {
    fun hash(secret: String): String = sha256Hex(secret)

    fun secret(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }.hex()

    fun prepare(
        request: ApiGuestTripReviewInput,
        now: Instant,
    ): Pair<ApiTripInput, AccountSettings> {
        val profile = ProfilePreferences.request(request.profile)
        var settings = mockRegistration("", "", "", AuthLanguage.En, now).settings
        val missing = listOf("habitual_bed", "habitual_wake").filter { profile[it].string == null }
        if (missing.isNotEmpty()) {
            throw DataFailure.Validation(
                missing.associate {
                    "profile.$it" to
                        FieldMessage("Check your bedtime and wake-up time.")
                },
                422,
            )
        }
        try {
            MockSettings.validate(SettingsSection.BodyClock, profile, settings)
        } catch (failure: DataFailure) {
            if (failure is DataFailure.Validation) {
                throw DataFailure.Validation(
                    failure.fields.mapKeys {
                        "profile." +
                            it.key
                    },
                    failure.status,
                )
            }
            throw failure
        }
        settings = MockSettings.apply(profile, SettingsSection.BodyClock, settings)
        settings =
            AccountSettings(
                settings.bodyClock,
                settings.sleepAnswered,
                settings.preferences,
                settings.reminders,
                JsonObject(
                    mapOf("credits" to JsonPrimitive(3), "unlimited" to JsonPrimitive(false)),
                ),
                settings.policies,
            )
        val input =
            request.input.copy(
                `return` = null,
                tripEndKind = "one_way",
                tripEndNights = null,
                tripEndCheckOutDate = null,
                override =
                    ApiTripInputOverride(
                        true,
                        profile["habitual_bed"].string,
                        profile["habitual_wake"].string,
                        profile["chronotype"].string,
                    ),
            )
        for ((index, leg) in input.outbound.legs.withIndex()) {
            if (leg.fromIata ==
                leg.toIata
            ) {
                throw MockSettings.invalid(
                    "outbound.legs.$index.to_iata",
                    "Choose different departure and arrival airports.",
                )
            }
        }
        val itinerary = MockTripGeneration.itinerary(input)
        val arrival = itinerary.legs.lastOrNull()?.arriveUtc
        if (arrival != null &&
            instant(arrival) <= now
        ) {
            throw MockSettings.invalid("review", "A new plan needs an upcoming arrival. Check your flight dates.")
        }
        return input to settings
    }
}

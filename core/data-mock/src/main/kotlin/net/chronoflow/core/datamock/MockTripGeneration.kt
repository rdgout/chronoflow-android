@file:Suppress("ktlint:standard:filename")

package net.chronoflow.core.datamock

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ObjectSerializer
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.contracts.objectAt
import net.chronoflow.core.model.contracts.optionalStringAt
import net.chronoflow.core.model.contracts.require
import net.chronoflow.core.model.contracts.stringAt

@Serializable(with = MockWizardDraft.Serializer::class)
@ConsistentCopyVisibility
data class MockWizardDraft private constructor(
    val owner: String,
    val tripID: String?,
    val input: ApiTripInput,
    val review: ApiTripReview,
    val profile: JsonObject,
    val expiresAt: Double,
) {
    companion object {
        operator fun invoke(
            owner: String,
            tripID: String?,
            input: ApiTripInput,
            review: ApiTripReview,
            profile: JsonObject,
            expiresAt: Double,
        ): MockWizardDraft =
            MockWizardDraft(owner, tripID, input, review, ProfilePreferences.bodyClock(profile), expiresAt)
    }

    override fun toString(): String = "MockWizardDraft(***)"

    object Serializer : ObjectSerializer<MockWizardDraft>("net.chronoflow.core.datamock.MockWizardDraft") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): MockWizardDraft =
            MockWizardDraft(
                obj.stringAt("owner"),
                obj.optionalStringAt("tripID"),
                json.decodeFromJsonElement(ApiTripInput.serializer(), obj.require("input")),
                json.decodeFromJsonElement(ApiTripReview.serializer(), obj.require("review")),
                obj.objectAt("profile"),
                (obj.require("expiresAt") as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull
                    ?: throw SerializationException("expiresAt must be a number"),
            )

        override fun encode(
            json: Json,
            value: MockWizardDraft,
        ): JsonObject =
            JsonObject(
                buildMap {
                    put("owner", JsonPrimitive(value.owner))
                    value.tripID?.let { put("tripID", JsonPrimitive(it)) }
                    put("input", json.encodeToJsonElement(ApiTripInput.serializer(), value.input))
                    put("review", json.encodeToJsonElement(ApiTripReview.serializer(), value.review))
                    put("profile", ProfilePreferences.bodyClock(value.profile))
                    put("expiresAt", JsonPrimitive(value.expiresAt))
                },
            )
    }
}

@Serializable
data class MockWizardReceipt(
    val intent: String,
    val tripID: String,
    val expiresAt: Double,
) {
    override fun toString(): String = "MockWizardReceipt(***)"
}

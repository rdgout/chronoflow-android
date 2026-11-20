@file:Suppress("ktlint:standard:enum-entry-name-case")

package net.chronoflow.core.wizard.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiReviewPrompt
import net.chronoflow.core.model.contracts.ApiStayEnd
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.wizard.generation.TripEditContext
import java.time.Instant
import java.util.UUID

@Serializable
enum class MemberWizardStep {
    whereTo,
    outbound,
    returnChoice,
    returnRoute,
    returnFlight,
    sleep,
    checking,
    question,
    excluded,
    continuation,
    fineTune,
    ready,
    calculating,
}

@Serializable
data class WizardAirportSheet(
    val index: Int,
    val side: Side,
    val replacingStop: Boolean? = null,
) {
    enum class Side { from, to, home, stop }

    val id: String get() = "$index-${side.name}-${replacingStop == true}"
}

@Serializable
data class MemberWizardState(
    val mode: WizardMode = WizardMode.member,
    val instance: String = UUID.randomUUID().toString(),
    val revision: Int = 0,
    val step: MemberWizardStep = MemberWizardStep.whereTo,
    val entered: Boolean = false,
    val fromLastTrip: Boolean = false,
    val firstTrip: Boolean = true,
    val sleepNeeded: Boolean = true,
    val homeChanged: Boolean = false,
    val returnRouteConfirmed: Boolean? = null,
    val profile: JsonObject = JsonObject(emptyMap()),
    val airports: Map<String, ApiAirportSuggestion> = emptyMap(),
    val sheet: WizardAirportSheet? = null,
    val lastInputStep: MemberWizardStep? = null,
    val progressFloor: Double? = null,
    val review: ApiTripReview? = null,
    val questions: List<ApiReviewPrompt>? = null,
    val questionIndex: Int? = null,
    val checkingAnswers: Boolean? = null,
    val sleepCompleted: Boolean? = null,
    val savedProfile: JsonObject? = null,
    val savedTripId: String? = null,
    val checkedInput: String? = null,
    @Serializable(with = InstantMillisSerializer::class)
    val reviewedAt: Instant? = null,
    val revisit: String? = null,
    val returnToReady: Boolean? = null,
    val promptSheetID: String? = null,
    val saveFailure: WizardSaveFailure? = null,
    val updated: Boolean? = null,
    val signingIn: Boolean? = null,
    val edit: TripEditContext? = null,
    val originalForm: WizardForm? = null,
    val originalInput: String? = null,
    val copiedRoute: String? = null,
    val snapshotStayEnd: ApiStayEnd? = null,
    val sourceChange: WizardPresentation.SourceChange? = null,
    @SerialName("pendingDateTimes")
    val pendingDateTimes: Map<String, WizardPendingDateTime> = emptyMap(),
) {
    fun discardRetiredValues(): MemberWizardState =
        copy(
            profile = ProfilePreferences.request(profile),
            savedProfile = savedProfile?.let(ProfilePreferences::bodyClock),
            questions = questions?.let(ProfilePreferences::reviewPrompts),
            review = review?.let { it.copy(prompts = ProfilePreferences.reviewPrompts(it.prompts)) },
        )
}

@Serializable
data class MemberWizardDraft(
    val form: WizardForm = WizardForm(),
    val state: MemberWizardState = MemberWizardState(),
    val fieldErrors: Map<String, FieldMessage> = emptyMap(),
) {
    val airports: Map<String, ApiAirportSuggestion> get() = state.airports
}

interface WizardDraftStore {
    suspend fun persist(draft: MemberWizardDraft)

    suspend fun discard(instance: String)

    suspend fun didSave(
        instance: String,
        firstTrip: Boolean,
    )
}

internal object InstantMillisSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("WizardInstantMillis", PrimitiveKind.LONG)

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) = encoder.encodeLong(value.toEpochMilli())

    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochMilli(decoder.decodeLong())
}

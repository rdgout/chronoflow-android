package net.chronoflow.core.wizard.generation

import kotlinx.coroutines.CancellationException
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.testing.wizard.EffectProbe
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview

internal fun generationState(
    instance: String = "one",
    edit: TripEditContext? = null,
): TripGenerationState = TripGenerationState(instance, ApiTripReviewInput(generationInput()), edit, generationReview())

internal suspend fun withGeneration(block: suspend (GenerationSource, EffectProbe, TripGenerationService) -> Unit) {
    val source = GenerationSource()
    val probe = EffectProbe()
    val service = TripGenerationService(source, probe.effects())
    try {
        block(source, probe, service)
    } finally {
        service.cancel()
    }
}

internal suspend fun generationFailure(block: suspend () -> Unit): Exception {
    try {
        block()
    } catch (
        failure: CancellationException,
    ) {
        return failure
    } catch (failure: Exception) {
        return failure
    }
    error("Expected an operation failure")
}

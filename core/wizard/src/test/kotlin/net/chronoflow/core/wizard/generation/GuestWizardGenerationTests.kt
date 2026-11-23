@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.generation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiTripInputOverride
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class GuestWizardGenerationTests {
    @Test fun firstCallMintsSecretAndUsesSleepAnswersAndDeviceWithoutAccountEffects() =
        runTest(timeout = 60.seconds) {
            withGuest {
                queue += Result.success(guestReview("first", "minted"))
                val base = guestGenerationInput()
                val input =
                    base.copy(
                        profile = JsonObject(checkNotNull(base.profile) + ("pregnancy" to JsonPrimitive(true))),
                        input =
                            base.input.copy(
                                `return` = base.input.outbound,
                                tripEndKind = "return_legs",
                                tripEndNights = 3,
                                override = ApiTripInputOverride(true, null, null, null),
                            ),
                    )
                val result = service.review("wizard", input)
                val sent = source.guestInputs.single()
                assertNull(sent.identity)
                assertNull(sent.guestSecret)
                assertEquals(base.profile, sent.profile)
                assertEquals(deviceID, sent.deviceID)
                assertEquals(1, deviceCalls)
                assertEquals("minted", secrets.get("first"))
                assertNull(result.review.identity.guestSecret)
                assertNull(result.input.identity?.guestSecret)
                assertNull(result.input.input.`return`)
                assertEquals("one_way", result.input.input.tripEndKind)
                assertFalse(result.input.input.override.use)
                assertTrue(source.operations.isEmpty())
                expectFailure(DataFailure.Server(null)) {
                    service.save(TripGenerationState("wizard", input)) { fail("guest must not save") }
                }
            }
        }

    @Test fun knownSecretIsReusedAndANewTokenRotatesIt() =
        runTest(timeout = 60.seconds) {
            withGuest {
                secrets.set("known", "old")
                val input = guestGenerationInput().copy(identity = ApiReviewIdentity("old", 2, 3, null, null))
                queue += Result.success(guestReview("old"))
                queue += Result.success(guestReview("new", "rotated"))
                val first = service.review("wizard", input)
                assertEquals("known", source.guestInputs.first().guestSecret)
                assertEquals("known", secrets.get("old"))
                service.review("wizard", first.input)
                assertNull(secrets.get("old"))
                assertEquals("rotated", secrets.get("new"))
            }
        }

    @Test fun expiryRetriesOnceWithoutIdentityAndRetainsKnownSecretForNewToken() =
        runTest(timeout = 60.seconds) {
            withGuest {
                secrets.set("known", "old")
                val input = guestGenerationInput().copy(identity = ApiReviewIdentity("old", 2, 3, null, null))
                queue += Result.failure(DataFailure.ReviewExpired)
                queue += Result.success(guestReview("fresh"))
                val result = service.review("wizard", input)
                val sent = source.guestInputs
                assertEquals(2, sent.size)
                assertEquals(input.identity, sent[0].identity)
                assertNull(sent[1].identity)
                assertEquals(listOf("known", "known"), sent.map { it.guestSecret })
                assertEquals("fresh", result.input.identity?.token)
                assertEquals("known", secrets.get("fresh"))
                assertNull(secrets.get("old"))
            }
        }

    @Test fun secondExpirySurfacesAndFirstCallExpiryIsNotRetried() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val input = guestGenerationInput().copy(identity = ApiReviewIdentity("old", 1, 1, null, null))
                queue += Result.failure(DataFailure.ReviewExpired)
                queue += Result.failure(DataFailure.ReviewExpired)
                expectFailure(DataFailure.ReviewExpired) { service.review("wizard", input) }
                assertEquals(2, source.guestInputs.size)
                queue += Result.failure(DataFailure.ReviewExpired)
                expectFailure(DataFailure.ReviewExpired) { service.review("wizard", input.copy(identity = null)) }
                assertEquals(3, source.guestInputs.size)
            }
        }

    @Test fun failuresSurfaceWithoutRetry() =
        runTest(timeout = 60.seconds) {
            for (failure in listOf(DataFailure.Offline, DataFailure.Throttled(42.0, 429))) {
                withGuest {
                    queue += Result.failure(failure)
                    expectFailure(failure) { service.review("wizard", guestGenerationInput()) }
                    assertEquals("failure=$failure", 1, source.guestInputs.size)
                }
            }
        }

    @Test fun discardDuringRequestDeletesMintedSecretAndDropsResult() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val gate = CompletableDeferred<Unit>()
                source.guestHandler = {
                    withContext(NonCancellable) { gate.await() }
                    guestReview("late", "late-secret")
                }
                val request = backgroundScope.async { service.review("wizard", guestGenerationInput()) }
                runCurrent()
                assertEquals(1, source.guestInputs.size)
                service.cancel()
                gate.complete(Unit)
                expectCancellation { request.await() }
                assertNull(secrets.get("late"))
                expectCancellation { service.review("wizard", guestGenerationInput()) }
            }
        }

    @Test fun cancelledReviewDoesNotRetryExpiryAndAllowsNextCheck() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val gate = CompletableDeferred<Unit>()
                source.guestHandler = {
                    withContext(NonCancellable) { gate.await() }
                    throw DataFailure.ReviewExpired
                }
                val input = guestGenerationInput().copy(identity = ApiReviewIdentity("old", 1, null, null, null))
                val request = backgroundScope.async { failureOf { service.review("check", input) } }
                runCurrent()
                service.cancelReviews("check")
                gate.complete(Unit)
                val failure = request.await()
                assertTrue(failure is CancellationException || failure == DataFailure.ReviewExpired)
                assertEquals(1, source.guestInputs.size)
                source.guestHandler = { guestReview("next", "secret") }
                service.review("next-check", guestGenerationInput())
                assertEquals("secret", secrets.get("next"))
            }
        }

    @Test fun staleTokenDoesNotRetryAfterAnotherReviewChangesIt() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val gate = CompletableDeferred<Unit>()
                source.guestHandler = {
                    if (it.identity?.token == "old") {
                        gate.await()
                        throw DataFailure.ReviewExpired
                    }
                    guestReview("fresh", "secret")
                }
                val old = guestGenerationInput().copy(identity = ApiReviewIdentity("old", 1, null, null, null))
                val request = backgroundScope.async { failureOf { service.review("wizard", old) } }
                runCurrent()
                service.review("wizard", guestGenerationInput())
                gate.complete(Unit)
                assertEquals(DataFailure.ReviewExpired, request.await())
                assertEquals(2, source.guestInputs.size)
                assertEquals("secret", secrets.get("fresh"))
            }
        }

    @Test fun expiryInsideValidationUsesSharedErrorMapping() =
        runTest(timeout = 60.seconds) {
            withGuest {
                queue +=
                    Result.failure(
                        DataFailure.Validation(mapOf("review_expired" to FieldMessage("Review your trip again.")), 422),
                    )
                queue += Result.success(guestReview("fresh", "secret"))
                val result =
                    service.review(
                        "wizard",
                        guestGenerationInput().copy(identity = ApiReviewIdentity("old", 1, null, null, null)),
                    )
                assertEquals("fresh", result.review.identity.token)
                assertEquals(2, source.guestInputs.size)
            }
        }

    @Test fun discardWhileSecretIsBeingStoredCleansItUp() =
        runTest(timeout = 60.seconds) {
            val paused = PausedGuestSecrets()
            withGuest(paused) {
                queue += Result.success(guestReview("late", "secret"))
                val request = backgroundScope.async { service.review("wizard", guestGenerationInput()) }
                paused.stored.await()
                service.cancel()
                paused.release.complete(Unit)
                expectCancellation { request.await() }
                assertNull(secrets.get("late"))
            }
        }

    @Test fun duplicateReviewsShareOneRequestAndTimeoutIsThirtySeconds() =
        runTest(timeout = 60.seconds) {
            withGuest {
                val gate = CompletableDeferred<Unit>()
                source.guestHandler = {
                    gate.await()
                    guestReview("shared", "secret")
                }
                val input = guestGenerationInput()
                val first = backgroundScope.async { service.review("same", input) }
                val second = backgroundScope.async { service.review("same", input) }
                runCurrent()
                assertEquals(1, source.guestInputs.size)
                gate.complete(Unit)
                assertEquals(first.await(), second.await())
                source.guestHandler = {
                    delay(100_000)
                    guestReview("too-late")
                }
                val started = testScheduler.currentTime
                val request = backgroundScope.async { failureOf { service.review("timeout", input) } }
                runCurrent()
                advanceTimeBy(29_999)
                runCurrent()
                assertFalse(request.isCompleted)
                advanceTimeBy(1)
                runCurrent()
                assertEquals(DataFailure.Server(null), request.await())
                assertEquals(30_000L, testScheduler.currentTime - started)
            }
        }

    private class PausedGuestSecrets : GuestSecretStore {
        private val memory = MemoryGuestSecretStore()
        val stored = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        override suspend fun set(
            secret: String,
            token: String,
        ) = memory.set(secret, token)

        override suspend fun get(token: String) = memory.get(token)

        override suspend fun delete(token: String) = memory.delete(token)

        override suspend fun deleteAll() = memory.deleteAll()

        override suspend fun rotate(
            oldToken: String?,
            newToken: String,
            secret: String,
        ) {
            memory.rotate(oldToken, newToken, secret)
            stored.complete(Unit)
            withContext(NonCancellable) { release.await() }
        }
    }

    private class Fixture(
        scope: TestScope,
        val secrets: GuestSecretStore,
    ) {
        val source = GenerationSource()
        val queue = ArrayDeque<Result<ApiTripReview>>()
        val deviceID = "0199a3c0-0000-7000-8000-000000000001"
        var deviceCalls = 0
        val service =
            GuestWizardGeneration(
                source,
                secrets,
                {
                    deviceCalls++
                    deviceID
                },
                parentContext =
                    scope.backgroundScope.coroutineContext + StandardTestDispatcher(scope.testScheduler),
            )

        init {
            source.guestHandler = { queue.removeFirst().getOrThrow() }
        }
    }

    private suspend fun TestScope.withGuest(
        secrets: GuestSecretStore = MemoryGuestSecretStore(),
        block: suspend Fixture.() -> Unit,
    ) {
        val fixture = Fixture(this, secrets)
        try {
            fixture.block()
        } finally {
            withContext(NonCancellable) { fixture.service.cancel() }
        }
    }

    private suspend fun failureOf(block: suspend () -> Any?): Throwable {
        try {
            block()
        } catch (failure: DataFailure) {
            return failure
        } catch (failure: CancellationException) {
            return failure
        }
        error("Expected a review failure")
    }

    private suspend fun expectFailure(
        expected: DataFailure,
        block: suspend () -> Unit,
    ) {
        try {
            block()
            fail("expected $expected")
        } catch (failure: DataFailure) {
            assertEquals(expected, failure)
        }
    }

    private suspend fun expectCancellation(block: suspend () -> Unit) {
        try {
            block()
            fail("cancelled review returned")
        } catch (_: CancellationException) {
        }
    }
}

private fun guestGenerationInput() =
    ApiTripReviewInput(
        input = generationInput(),
        profile =
            JsonObject(
                mapOf(
                    "habitual_bed" to JsonPrimitive("23:00"),
                    "habitual_wake" to JsonPrimitive("07:00"),
                    "chronotype" to JsonPrimitive("intermediate"),
                    "age_band" to JsonPrimitive("18_64"),
                    "shift_work" to JsonPrimitive(false),
                ),
            ),
    )

private fun guestReview(
    token: String,
    secret: String? = null,
): ApiTripReview =
    generationReview().let {
        it.copy(identity = it.identity.copy(token = token, guestSecret = secret))
    }

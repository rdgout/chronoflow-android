package net.chronoflow.debug

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.contracts.ProfilePreferences
import java.io.File
import java.time.Instant

internal object SettingsBoardCaptureFixture {
    fun seed(
        original: MockServerState,
        seed: TestSeed,
    ): MockServerState {
        if (seed.testId == null) return original
        val state = seed.settingsBoardState ?: return original
        return original.copy(
            accounts =
                original.accounts.mapValues { (_, account) ->
                    if (account.snapshot.email != MockScenarioValues.demoEmail) return@mapValues account
                    val snapshot = account.snapshot
                    val body = snapshot.settings.bodyClock.toMutableMap()
                    val readiness =
                        when (state) {
                            "bodyMissing" -> {
                                body["ageBand"] = JsonNull
                                body["shiftWork"] = JsonNull
                                JsonObject(
                                    mapOf(
                                        "variant" to JsonPrimitive("needs-answers"),
                                        "missing" to
                                            JsonArray(
                                                listOf(
                                                    JsonObject(mapOf("question" to JsonNull)),
                                                    JsonObject(mapOf("question" to JsonPrimitive("shift_work"))),
                                                ),
                                            ),
                                        "invalidSections" to JsonArray(emptyList()),
                                    ),
                                )
                            }

                            "bodyAdjusted" -> {
                                body["ageBand"] = JsonPrimitive("65_plus")
                                body["excludeCaffeine"] = JsonPrimitive(true)
                                body["flightMovementBreaks"] = JsonPrimitive(true)
                                ProfilePreferences.readiness(JsonObject(emptyMap()), JsonObject(body))
                            }

                            "bodyExcluded" -> {
                                body["shiftWork"] = JsonPrimitive(true)
                                JsonObject(
                                    mapOf(
                                        "variant" to JsonPrimitive("excluded"),
                                        "missing" to JsonArray(emptyList()),
                                        "invalidSections" to JsonArray(emptyList()),
                                    ),
                                )
                            }

                            else -> {
                                snapshot.readiness
                            }
                        }
                    val settings =
                        AccountSettings(
                            JsonObject(body),
                            snapshot.settings.sleepAnswered,
                            snapshot.settings.preferences,
                            snapshot.settings.reminders,
                            snapshot.settings.planCredits,
                            snapshot.settings.policies,
                        )
                    account.copy(
                        snapshot =
                            AccountSnapshot(
                                snapshot.id,
                                snapshot.name,
                                snapshot.email,
                                snapshot.hasVerifiedEmail,
                                settings,
                                readiness,
                                snapshot.billing,
                                snapshot.security,
                            ),
                    )
                },
        )
    }

    suspend fun prepare(
        root: File,
        seed: TestSeed,
        now: Instant,
        io: CoroutineDispatcher,
    ) = withContext(io) {
        if (seed.testId == null || seed.settingsBoardState == null || seed.settingsBoardState == "bodyReady") {
            return@withContext
        }
        val original = MockAppSeed.make(now, seed.unverified, seed.walkthroughSeen, seed.termsPending)
        FileMockStateStore(root).save(SettingsBoardCaptureFixture.seed(original, seed))
    }
}

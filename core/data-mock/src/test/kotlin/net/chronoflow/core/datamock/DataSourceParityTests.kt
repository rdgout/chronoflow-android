package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.datamock.support.MockApi
import net.chronoflow.core.datamock.support.json
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.network.ApiDataSource
import net.chronoflow.core.testing.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DataSourceParityTests {
    @get:Rule val server = MockApi()

    @Test fun invalidSettingsReturnEveryFieldReminders() = invalidSettingsReturnEveryField(SettingsSection.Reminders)

    @Test fun invalidSettingsReturnEveryFieldPreferences() =
        invalidSettingsReturnEveryField(
            SettingsSection.Preferences,
        )

    @Test fun invalidSettingsReturnEveryFieldBodyClock() = invalidSettingsReturnEveryField(SettingsSection.BodyClock)

    private fun invalidSettingsReturnEveryField(section: SettingsSection) =
        runTest(timeout = 60.seconds) {
            val patch =
                when (section) {
                    SettingsSection.Reminders -> {
                        JsonObject(
                            mapOf(
                                "lead_min" to JsonPrimitive(45),
                                "light" to JsonPrimitive("x"),
                                "winddown" to JsonPrimitive(true),
                            ),
                        )
                    }

                    SettingsSection.Preferences -> {
                        JsonObject(
                            mapOf(
                                "time_format" to JsonPrimitive("13"),
                                "show_optional_items" to JsonPrimitive("x"),
                            ),
                        )
                    }

                    SettingsSection.BodyClock -> {
                        JsonObject(
                            mapOf(
                                "habitual_bed" to JsonPrimitive("25:00"),
                                "habitual_wake" to JsonPrimitive("99:00"),
                            ),
                        )
                    }
                }
            val fields = patch.keys.filter { it != "winddown" }
            val message =
                if (section ==
                    SettingsSection.BodyClock
                ) {
                    "Enter a valid time."
                } else {
                    "Choose one of the available options."
                }
            val expected = DataFailure.Validation(fields.associateWith { FieldMessage(message) }, 422)
            val body =
                JsonObject(
                    mapOf(
                        "code" to JsonPrimitive("validation_failed"),
                        "message" to JsonPrimitive("The given data was invalid."),
                        "errors" to JsonObject(fields.associateWith { JsonPrimitive(message) }),
                    ),
                ).toString()
            val route =
                when (section) {
                    SettingsSection.Reminders -> "reminders"
                    SettingsSection.Preferences -> "preferences"
                    SettingsSection.BodyClock -> "body-clock"
                }
            server.reply("PATCH", "/v1/account/$route", 422, body)
            val api = ApiDataSource(server.client())
            val mock = mockSource()
            login(mock)
            val before = mock.fetchAccount()
            val keys = patch.keys.sorted()

            suspend fun check(
                source: DataSource,
                order: List<String>,
            ) {
                assertEquals(
                    expected,
                    failure {
                        source.send(
                            OutboxChange.Settings(section, JsonObject(order.associateWith { patch.getValue(it) })),
                        )
                    },
                )
            }
            check(api, keys)
            check(api, keys.reversed())
            check(mock, keys)
            check(mock, keys.reversed())
            assertEquals(before, mock.fetchAccount())
        }

    @Test fun wrongCredentialsMatchBackendValidation() =
        runTest(timeout = 60.seconds) {
            server.reply("POST", "/v1/auth/login", 422, json("errors/wrong_credentials.json"))
            val api = ApiDataSource(server.client())
            val mock = mockSource()
            val expected =
                DataFailure.Validation(
                    mapOf("email" to FieldMessage("These sign-in details don't match our records.")),
                    422,
                )
            assertEquals(expected, failure { api.signIn(passwordInput(password = "wrong")) })
            assertEquals(expected, failure { mock.signIn(passwordInput(password = "wrong")) })
        }

    @Test fun sharedRecordedContract() =
        runTest(timeout = 60.seconds) {
            server.reply("POST", "/v1/auth/login", body = json("auth/session.json"))
            server.reply("POST", "/v1/auth/passkey/login", body = json("auth/session.json"))
            server.reply("POST", "/v1/auth/passkey/options", body = json("auth/passkey-options.json"))
            server.reply("GET", "/v1/users/me", body = json("account/identity.json"))
            server.reply("GET", "/v1/account", body = json("account/snapshot.json"))
            server.reply("GET", "/v1/trips", body = json("trips/list.json"))
            server.reply("GET", "/v1/trips/$demoTripID", body = json("trips/detail.json"))
            val expectedSession =
                ApiJson.response.decodeFromString(
                    SessionResult.serializer(),
                    Fixtures.text("auth/session.json"),
                )
            val expectedSnapshot =
                ApiJson.response.decodeFromString(
                    AccountSnapshot.serializer(),
                    Fixtures.text("account/snapshot.json"),
                )
            val expectedDetail =
                ApiJson.response.decodeFromString(
                    TripDetail.serializer(),
                    Fixtures.text("trips/detail.json"),
                )

            suspend fun check(source: DataSource) {
                val session = login(source)
                assertEquals(expectedSession.account, session.account)
                assertEquals(expectedSession.tripId, session.tripId)
                assertEquals(expectedSession.account, source.fetchIdentity())
                assertEquals(expectedSnapshot, source.fetchAccount())
                assertEquals(
                    demoTripID,
                    source
                        .fetchTrips()
                        .first()
                        .card.id,
                )
                assertEquals(expectedDetail, source.fetchTripDetail(demoTripID).detail)
                val options = source.passkeyOptions()
                assertEquals("app.chronoflow.net", options.options.rpId)
                assertEquals(60000, options.options.timeout)
                assertEquals(expectedSession.account, source.passkeyLogin(passkeyInput(options)).account)
            }
            check(ApiDataSource(server.client()))
            check(mockSource())
        }
}

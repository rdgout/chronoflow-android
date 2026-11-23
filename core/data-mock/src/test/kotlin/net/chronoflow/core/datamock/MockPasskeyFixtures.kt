package net.chronoflow.core.datamock

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.api.ApiPasskeyAttestation
import net.chronoflow.core.model.api.ApiPasskeyAttestationResponse
import net.chronoflow.core.model.api.ApiPasskeyRegistrationChallenge
import net.chronoflow.core.model.api.CredentialType

internal fun passkeyState(): MockServerState =
    scenarioState().updateAccount(demoID) {
        it.copy(
            passkeys =
                listOf(
                    ApiPasskey(1, "iPhone 16", null, "2026-09-14T08:00:00Z", "2026-11-02T08:00:00Z"),
                    ApiPasskey(2, "MacBook Pro", null, "2026-10-02T08:00:00Z", null),
                ),
            snapshot =
                it.snapshot.with(
                    security =
                        JsonObject(it.snapshot.security + ("passkeyCount" to JsonPrimitive(2))),
                ),
        )
    }

internal fun attestation(challenge: ApiPasskeyRegistrationChallenge): ApiPasskeyAttestation =
    ApiPasskeyAttestation(
        "AQID",
        "AQID",
        CredentialType.PublicKey,
        ApiPasskeyAttestationResponse(challenge.options.challenge, "YXR0", listOf("internal")),
    )

internal suspend fun options(
    source: MockDataSource,
    password: String = fixturePassword,
): ApiPasskeyRegistrationChallenge =
    (
        source.accountSecurity(
            AccountSecurityChange.PasskeyCreationOptions(password),
        ) as AccountSecurityResult.PasskeyCreation
    ).challenge

internal suspend fun list(source: MockDataSource): List<ApiPasskey> =
    (source.accountSecurity(AccountSecurityChange.ListPasskeys) as AccountSecurityResult.Passkeys).passkeys

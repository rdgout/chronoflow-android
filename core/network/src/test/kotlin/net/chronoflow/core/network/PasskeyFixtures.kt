@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

internal const val PASSKEY_LIST = """{"passkeys":[{"id":1,"name":"iPhone","authenticator":"Apple Passwords","createdAt":"2026-10-02T10:00:00.000000Z","lastUsedAt":null}]}"""
internal const val PASSKEY_CREATION = """{"challengeId":"c1","options":{"challenge":"Y2g","timeout":60000,"rp":{"id":"app.chronoflow.net","name":"app.chronoflow.net"},"user":{"id":"dQ","name":"n","displayName":"N"},"pubKeyCredParams":[{"type":"public-key","alg":-7},{"type":"public-key","alg":-257}],"authenticatorSelection":{"requireResidentKey":true,"userVerification":"required","residentKey":"required"},"attestation":"none","excludeCredentials":[{"type":"public-key","id":"AQID"}]}}"""
internal const val PASSKEY_ADDED = """{"id":12,"name":"iPhone","authenticator":"Apple Passwords","createdAt":"2026-11-12T09:30:00.000000Z","lastUsedAt":null}"""

internal fun attestationInput() =
    net.chronoflow.core.model.api.ApiPasskeyRegistrationInput(
        "c1",
        "iPhone",
        net.chronoflow.core.model.api.ApiPasskeyAttestation(
            "AQID",
            "AQID",
            net.chronoflow.core.model.api.CredentialType.PublicKey,
            net.chronoflow.core.model.api
                .ApiPasskeyAttestationResponse("Y2Q", "YXR0", listOf("internal")),
        ),
    )

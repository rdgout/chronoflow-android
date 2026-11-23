package net.chronoflow.core.model.passkey

import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyRegistrationChallenge

internal object RegistrationFixture {
    const val OPTIONS = """
        {"challenge":"Y2g","timeout":60000,"rp":{"id":"app.chronoflow.net","name":"ChronoFlow"},
        "user":{"id":"dXNlcg","name":"demo@example.com","displayName":"Demo"},
        "pubKeyCredParams":[{"type":"public-key","alg":-7},{"type":"public-key","alg":-257}],
        "authenticatorSelection":{"requireResidentKey":true,"userVerification":"required","residentKey":"required"},
        "attestation":"none","excludeCredentials":[{"type":"public-key","id":"AQ"}]}"""
    const val RESPONSE = """
        {"id":"AQ","rawId":"AQ==","type":"public-key","authenticatorAttachment":"platform",
        "clientExtensionResults":{},"response":{"clientDataJSON":"Ag==","attestationObject":"+/8=",
        "publicKey":"dropped","transports":["internal","hybrid"]}}"""
    val challenge get() = ApiPasskeyRegistrationChallenge("c1", ApiJson.response.decodeFromString(OPTIONS))
}

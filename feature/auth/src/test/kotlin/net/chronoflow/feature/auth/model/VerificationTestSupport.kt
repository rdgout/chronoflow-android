package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockMail
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import java.time.Instant

class VerificationClock {
    var seconds = 1_800_000_000.0

    fun now(): Instant = Instant.ofEpochMilli((seconds * 1000).toLong())

    fun advance(seconds: Double) {
        this.seconds += seconds
    }
}

class RegisteredVerification(
    val source: MockDataSource,
    val result: SessionResult,
    val input: SignedVerificationInput,
    val clock: VerificationClock,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    val context = VerificationContext(result.account.id, result.account.email, 7)
    var current: VerificationContext? = context
    val stored = mutableListOf<VerificationContext>()
    var routes = 0
    var discarded = 0

    fun model(
        source: DataSource = this.source,
        current: suspend () -> VerificationContext? = { this.current },
        persist: suspend (VerificationContext) -> Boolean = {
            stored += it
            true
        },
        discard: suspend () -> Unit = { discarded++ },
        mail: (suspend () -> SignedVerificationInput?)? = null,
    ) = VerifyEmailModel(context, source, {
        clock.seconds
    }, current, persist, { routes++ }, discard, mail, scope, dispatcher, dispatcher)
}

suspend fun TestScope.registered(
    clock: VerificationClock = VerificationClock(),
    sleep: suspend (Double) -> Unit = {},
): RegisteredVerification {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val source =
        MockDataSource(store = MemoryMockStateStore(), seed = MockServerState(), now = clock::now, delay = {
            0.0
        }, sleep = sleep, debugAppSeed = false, io = dispatcher)
    val result =
        source.signUp(
            RegisterInput(
                "Name",
                "new@example.com",
                "NewPassword123!",
                "NewPassword123!",
                AUTH_DEVICE_ID,
                "test",
                AuthLanguage.Nl,
            ),
        )
    val input = (source.inbox().last() as MockMail.Verification).input
    return RegisteredVerification(source, result, input, clock, backgroundScope, dispatcher)
}

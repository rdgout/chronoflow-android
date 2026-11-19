package net.chronoflow.feature.auth.verify

import androidx.compose.runtime.Composable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.feature.auth.model.VerificationContext
import net.chronoflow.feature.auth.model.VerifyEmailModel
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import java.time.Instant

internal data class VerifySampleState(
    val ui: VerifyEmailUiState,
    val remaining: Int,
)

internal suspend fun TestScope.verifySnapshotModel(
    state: String,
    email: String = "rick@example.com",
): VerifySampleState {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val source =
        MockDataSource(
            store = MemoryMockStateStore(),
            seed = MockServerState(),
            now = { Instant.ofEpochSecond(1_800_000_000) },
            delay = { 0.0 },
            sleep = {},
            debugAppSeed = false,
            io = dispatcher,
        )
    val result =
        source.signUp(
            RegisterInput("Rick", email, "NewPassword123!", "NewPassword123!", AUTH_DEVICE_ID, "test"),
        )
    val context = VerificationContext(result.account.id, result.account.email, 1)
    val model =
        VerifyEmailModel(
            context,
            source,
            { 1_800_000_000.0 },
            { context },
            { true },
            {},
            { throw DataFailure.Offline },
            scope = backgroundScope,
            main = dispatcher,
            io = dispatcher,
        )
    model.activate()
    when (state) {
        "countdown" -> {
            model.resend()
            model.form.clear()
        }

        "failed" -> {
            model.receiveLink(null)
            model.check()
        }

        "failedServer", "failedThrottle", "failedOffline" -> {
            model.receiveLink(null)
            model.check()
            if (state ==
                "failedThrottle"
            ) {
                model.resend()
            } else {
                model.form.receive(
                    if (state ==
                        "failedOffline"
                    ) {
                        DataFailure.Offline
                    } else {
                        DataFailure.Server(503)
                    },
                )
            }
        }

        "server" -> {
            model.form.receive(DataFailure.Server(503))
        }

        "throttle" -> {
            model.resend()
        }

        "offline" -> {
            model.form.receive(DataFailure.Offline)
        }

        "signOutError" -> {
            model.signOut()
        }
    }
    return VerifySampleState(
        model.uiState.value
            .presentation(email)
            .copy(
                form =
                    model.form.state.value
                        .copy(failure = null),
            ),
        model.remaining,
    )
}

@Composable
internal fun VerifySample(value: VerifySampleState) {
    VerifyEmailContent(value.ui, { value.remaining }, {})
}

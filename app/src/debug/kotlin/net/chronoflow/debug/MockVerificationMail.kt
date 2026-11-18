package net.chronoflow.debug

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.app.MockSources
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.feature.auth.model.VerificationContext
import net.chronoflow.feature.auth.verify.VerificationMail
import java.io.IOException

class MockVerificationMail(
    private val sources: MockSources,
    private val source: suspend () -> DataSource,
    private val io: CoroutineDispatcher,
) : VerificationMail {
    override suspend fun latest(context: VerificationContext): SignedVerificationInput? =
        withContext(io) {
            try {
                sources.latestVerificationLink(source(), context)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: DataFailure) {
                null
            } catch (_: IOException) {
                null
            }
        }
}

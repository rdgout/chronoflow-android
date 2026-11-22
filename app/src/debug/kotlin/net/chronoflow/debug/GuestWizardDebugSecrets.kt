package net.chronoflow.debug

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.wizard.model.GuestWizardStore

object GuestWizardDebugSecrets {
    suspend fun hasReviewSecret(
        drafts: GuestWizardStore,
        secrets: GuestSecretStore,
        io: CoroutineDispatcher,
    ): Boolean =
        withContext(io) {
            val token =
                drafts
                    .recover()
                    ?.form
                    ?.identity
                    ?.token ?: return@withContext false
            !secrets.get(token).isNullOrEmpty()
        }
}

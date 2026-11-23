package net.chronoflow.app.guest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppDispatchers
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.guest.GuestSecretStore
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.wizard.generation.GuestWizardGeneration
import net.chronoflow.core.wizard.model.AirportSearchCache
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.guest.GuestSignUpLauncher
import net.chronoflow.feature.wizard.shell.WizardUiOverride
import java.time.Instant
import kotlin.math.roundToLong

internal class GuestWizardDependencies(
    override val store: GuestWizardStore,
    override val source: DataSource,
    private val secrets: GuestSecretStore,
    private val deviceIdentity: suspend () -> String,
    private val presence: () -> SessionPresence?,
    private val dispatchers: AppDispatchers,
    override val writerScope: CoroutineScope,
    override val now: () -> Instant,
    override val signUpLauncher: GuestSignUpLauncher,
    private val catalogs: CopyCatalogProvider? = null,
    private val seed: ((MemberWizardDraft) -> MemberWizardDraft)? = null,
    override val uiOverride: WizardUiOverride? = null,
) : TripWizardDependencies {
    override val guest = true
    override val airportSearch =
        AirportSearchService(
            source,
            now,
            sleep = { delay((it * 1000).roundToLong()) },
            cache = AirportSearchCache(),
        )

    override suspend fun copyFormatter(): CopyLineFormatter? = catalogs?.catalog()?.let(::CopyLineFormatter)

    override suspend fun recover() = store.recover()

    override suspend fun start(): MemberWizardDraft =
        withContext(dispatchers.io) {
            if (presence() != SessionPresence.SignedOut) throw CancellationException()
            var draft = store.start()
            if (draft.state.signingIn == true) {
                draft = draft.copy(state = draft.state.copy(signingIn = false, revision = draft.state.revision + 1))
                store.persist(draft)
            }
            seed?.let {
                draft = it(draft)
                store.persist(draft)
            }
            currentCoroutineContext().ensureActive()
            if (presence() != SessionPresence.SignedOut) throw CancellationException()
            draft
        }

    override suspend fun startTrip(
        id: String,
        action: TripWizardAction,
    ): MemberWizardDraft = throw DataFailure.Server(null)

    override fun newGeneration() =
        GuestWizardGeneration(
            source,
            secrets,
            deviceIdentity,
            parentContext =
                writerScope.coroutineContext + dispatchers.default,
        )
}

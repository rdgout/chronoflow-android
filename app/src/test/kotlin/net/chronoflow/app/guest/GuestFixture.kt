package net.chronoflow.app.guest

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.auth.LazyPublicDataSource
import net.chronoflow.core.model.guest.MemoryGuestSecretStore
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.wizard.model.GuestWizardStore
import net.chronoflow.feature.wizard.guest.GuestSignUpLauncher
import java.io.File
import java.nio.file.Files

internal class GuestFixture(
    scope: TestScope,
) {
    val root: File = Files.createTempDirectory("guest-host").toFile()
    val file = File(root, "guest-wizard.json")
    val clock = WizardTestClock()
    val dispatcher = StandardTestDispatcher(scope.testScheduler)
    val dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher)
    val store = GuestWizardStore({ file }, dispatcher, { clock.now })
    val secrets = MemoryGuestSecretStore()
    val navigator = RootNavigator().apply { navigate(Route.GuestWizard) }
    var presence: SessionPresence? = SessionPresence.SignedOut
    val source = GenerationSource()
    var publicLoads = 0
    val dependencies =
        GuestWizardDependencies(
            store,
            LazyPublicDataSource {
                publicLoads++
                source
            },
            secrets,
            { "0199a3c0-0000-7000-8000-000000000001" },
            { presence },
            dispatchers,
            scope.backgroundScope,
            { clock.now },
            GuestSignUpLauncher { it.signUpDismissed(needsReview = false, onGuestRoute = true) },
        )

    fun exits() = GuestWizardExits(navigator, navigator.state.value.revision, { presence }, store, secrets, dispatchers)
}

package net.chronoflow.debug

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.chronoflow.app.AppGraph
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.toRoute
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.launch.LaunchTrace
import kotlin.time.Duration.Companion.seconds

object ScreenSessionFixture {
    suspend fun route(
        graph: AppGraph,
        audit: String? = null,
        auditState: String? = null,
    ) {
        val presence = graph.readiness.session()
        LaunchTrace.endSession()
        val route =
            if (presence is SessionPresence.SignedIn) {
                graph.readiness.allowFirstLoad()
                graph.readiness.gate().toRoute()
            } else {
                Route.Welcome
            }
        graph.rootNavigator.navigate(route)
        when (audit) {
            "sign-in" -> {
                graph.rootNavigator.navigate(Route.SignIn)
            }

            "sign-up" -> {
                graph.rootNavigator.navigate(Route.SignUp)
            }

            "forgot" -> {
                graph.auth.router.forgot()
                AuthAuditSeedState.forgot(checkNotNull(graph.auth.forgotModel), auditState ?: "form")
            }

            "reset" -> {
                if (auditState == "expired") {
                    graph.auth.openReset(ResetPasswordLink("", MockScenarioValues.demoEmail))
                } else {
                    val source = graph.dataSources.public()
                    source.requestPasswordReset(MockScenarioValues.demoEmail)
                    val link = checkNotNull(graph.mockSources?.latestResetLink(source, MockScenarioValues.demoEmail))
                    withContext(graph.dispatchers.main) {
                        graph.rootNavigator.navigate(Route.SignIn)
                        graph.auth.openReset(link)
                    }
                }
                AuthAuditSeedState.reset(checkNotNull(graph.auth.resetModel), auditState ?: "form")
            }

            "verify" -> {
                val coordinator = graph.auth.verification
                coordinator.prepareVerification()
                val model =
                    checkNotNull(coordinator.model.value) { "verification audit requires an unverified account" }
                checkNotNull(
                    withTimeoutOrNull(5.seconds) {
                        model.uiState.first { it.hasStatus && !it.form.busy }
                    },
                ) { "verification audit status did not become ready" }
                AuthAuditSeedState.verify(model, auditState ?: "waiting")
            }

            "two-factor" -> {
                if (presence !is SessionPresence.SignedIn) {
                    graph.rootNavigator.navigate(Route.SignIn)
                    withContext(graph.dispatchers.main) {
                        graph.auth.signIn.editEmail(MockScenarioValues.twoFactorEmail)
                        graph.auth.signIn.submitPassword(
                            object : SecretField {
                                private var value = MockScenarioValues.password

                                override fun read(): String = value

                                override fun clear() {
                                    value = ""
                                }

                                override fun toString(): String = "SecretField(***)"
                            },
                        ) {}
                    }
                    if (auditState == "recovery") {
                        graph.auth.signIn.twoFactor.value
                            ?.toggleMode()
                    }
                }
            }
        }
        graph.finishLaunch()
        AuditSeedStatus.prepared.set(true)
    }
}

package net.chronoflow.debug

import kotlinx.coroutines.withContext
import net.chronoflow.app.AppGraph
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.toRoute
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.launch.LaunchTrace

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

            "forgot" -> {
                graph.rootNavigator.navigate(Route.Forgot)
            }

            "reset" -> {
                if (auditState != "expired") {
                    val source = graph.dataSources.public()
                    source.requestPasswordReset(MockScenarioValues.demoEmail)
                    val link = checkNotNull(graph.mockSources?.latestResetLink(source, MockScenarioValues.demoEmail))
                    withContext(graph.dispatchers.main) {
                        graph.rootNavigator.navigate(Route.SignIn)
                        graph.auth.openReset(link)
                    }
                }
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
                }
            }
        }
        graph.finishLaunch()
    }
}

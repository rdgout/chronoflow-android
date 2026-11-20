package net.chronoflow.feature.onboarding.support

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.feature.onboarding.OnboardingSession
import net.chronoflow.feature.onboarding.OnboardingSessions

internal class FakeOnboardingSessions(
    enqueue: suspend (OutboxChange) -> Unit = {},
    signOut: suspend (Boolean) -> SignOutResult = { SignOutResult.SignedOut },
) : OnboardingSessions {
    val policies = MutableStateFlow<AccountPolicies?>(null)
    val session =
        OnboardingSession(
            SessionSnapshot(StoredSession("test-token", "0199a3c0-0000-7000-8000-000000000000"), 1),
            enqueue,
            policies,
            signOut,
        )

    override fun current() = flowOf(session)
}

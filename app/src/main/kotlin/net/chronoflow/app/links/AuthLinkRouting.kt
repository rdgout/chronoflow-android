package net.chronoflow.app.links

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.chronoflow.core.model.auth.AuthLink
import net.chronoflow.feature.auth.links.GuestResetLinkRouter
import net.chronoflow.feature.auth.links.VerifyLinkReceiver

class AuthLinkRouting(
    private val scope: CoroutineScope,
    private val resetRouter: GuestResetLinkRouter,
    private val verifyLinks: VerifyLinkReceiver,
) : (AuthLink) -> Unit {
    override fun invoke(link: AuthLink) {
        when (link) {
            is AuthLink.Reset -> scope.launch { resetRouter.receive(link.link) }
            is AuthLink.Verify -> scope.launch { verifyLinks.receiveVerificationLink(link.input) }
        }
    }
}

package net.chronoflow.debug.guest

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import net.chronoflow.app.guest.GuestSignupPrefilling
import net.chronoflow.feature.auth.guest.GuestSignupFields

class GuestSignupPrefill : GuestSignupPrefilling {
    override val name = "Traveller"
    override val email = "guest-journey@example.com"

    override fun fillSecrets(fields: GuestSignupFields) {
        fields.password.setTextAndPlaceCursorAtEnd("NewPassword123!")
        fields.confirmation.setTextAndPlaceCursorAtEnd("NewPassword123!")
    }

    override fun toString(): String = "GuestSignupPrefill(***)"
}

package net.chronoflow.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Welcome : Route

    @Serializable data object SignIn : Route

    @Serializable data object TwoFactor : Route

    @Serializable data object Forgot : Route

    @Serializable data object SignUp : Route

    @Serializable data object Verify : Route

    @Serializable data object GuestWizard : Route

    @Serializable data object Terms : Route

    @Serializable data object Today : Route

    @Serializable data object Trips : Route

    @Serializable data object Settings : Route

    @Serializable data object Profile : Route

    @Serializable data object Account : Route

    @Serializable data object Language : Route

    @Serializable data object Security : Route

    @Serializable data object Password : Route

    @Serializable data object TwoFactorSettings : Route

    @Serializable data object Passkeys : Route

    @Serializable data object BodyClock : Route

    @Serializable data object Preferences : Route

    @Serializable data object Reminders : Route

    @Serializable data object Plan : Route

    @Serializable data class Reset(
        val email: String,
    ) : Route

    @Serializable data class Walkthrough(
        val replay: Boolean = false,
    ) : Route

    @Serializable data class TripPage(
        val tripId: String,
        val dayId: String? = null,
        val blockId: String? = null,
        val awaitingSync: Boolean = false,
        val notificationInputHash: String? = null,
        val notificationToken: String? = null,
        val sheet: TripSheet? = null,
    ) : Route

    @Serializable data class Wizard(
        val mode: WizardMode,
    ) : Route
}

@Serializable sealed interface TripSheet {
    @Serializable data class Step(
        val blockId: String,
    ) : TripSheet

    @Serializable data object Legend : TripSheet

    @Serializable data object TripActions : TripSheet
}

@Serializable sealed interface WizardMode {
    @Serializable data object Create : WizardMode

    @Serializable data class Edit(
        val tripId: String,
    ) : WizardMode

    @Serializable data class Duplicate(
        val tripId: String,
    ) : WizardMode
}

fun Route.debugName(): String =
    when (this) {
        Route.Welcome -> "welcome"
        Route.SignIn -> "signIn"
        Route.TwoFactor -> "twoFactor"
        Route.Forgot -> "forgot"
        Route.SignUp -> "signUp"
        Route.Verify -> "verify"
        Route.GuestWizard -> "guestWizard"
        Route.Terms -> "terms"
        Route.Today -> "today"
        Route.Trips -> "trips"
        Route.Settings -> "settings"
        Route.Profile -> "profile"
        Route.Account -> "account"
        Route.Language -> "language"
        Route.Security -> "security"
        Route.Password -> "password"
        Route.TwoFactorSettings -> "twoFactorSettings"
        Route.Passkeys -> "passkeys"
        Route.BodyClock -> "bodyClock"
        Route.Preferences -> "preferences"
        Route.Reminders -> "reminders"
        Route.Plan -> "plan"
        is Route.Reset -> "reset"
        is Route.Walkthrough -> "walkthrough"
        is Route.TripPage -> "tripPage"
        is Route.Wizard -> "wizard"
    }

fun Route.stateKey(): String =
    when (this) {
        is Route.TripPage -> {
            stateKey(debugName(), tripId, dayId, blockId, awaitingSync.toString(), sheet?.stateKey())
        }

        is Route.Reset -> {
            stateKey(debugName(), email)
        }

        is Route.Walkthrough -> {
            stateKey(debugName(), replay.toString())
        }

        is Route.Wizard -> {
            stateKey(debugName(), mode.stateKey())
        }

        else -> {
            debugName()
        }
    }

private fun TripSheet.stateKey(): String =
    when (this) {
        is TripSheet.Step -> stateKey("step", blockId)
        TripSheet.Legend -> "legend"
        TripSheet.TripActions -> "tripActions"
    }

private fun WizardMode.stateKey(): String =
    when (this) {
        WizardMode.Create -> "create"
        is WizardMode.Edit -> stateKey("edit", tripId)
        is WizardMode.Duplicate -> stateKey("duplicate", tripId)
    }

private fun stateKey(
    name: String,
    vararg identity: String?,
): String =
    buildString {
        append(name)
        identity.forEach { value ->
            append(':')
            if (value == null) append('-') else append(value.length).append(':').append(value)
        }
    }

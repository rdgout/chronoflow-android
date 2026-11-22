package net.chronoflow.feature.auth.model

import android.content.res.Configuration
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Locale

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class GuestSignupStateTest(
    private val state: GuestSignupState,
    private val language: String,
    private val title: String,
    private val message: String,
) {
    @Test fun keysResolveInEnAndNl() {
        val application = RuntimeEnvironment.getApplication()
        val configuration =
            Configuration(application.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            }
        val resources = application.createConfigurationContext(configuration).resources
        assertEquals(
            title,
            resources.getString(checkNotNull(CopyIndex.find(CopyTable.Auth, checkNotNull(state.titleKey))).resId),
        )
        assertEquals(
            message,
            resources.getString(checkNotNull(CopyIndex.find(CopyTable.Auth, checkNotNull(state.messageKey))).resId),
        )
        assertNull(GuestSignupState.Ready.titleKey)
        assertNull(GuestSignupState.Ready.messageKey)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(GuestSignupState.Failed, "en", "We couldn't create your account.", "Try again in a moment."),
                arrayOf(
                    GuestSignupState.Failed,
                    "nl",
                    "We konden je account niet aanmaken.",
                    "Probeer het zo meteen opnieuw.",
                ),
                arrayOf(GuestSignupState.Retry, "en", "Your account is ready.", "Try saving again shortly."),
                arrayOf(GuestSignupState.Retry, "nl", "Je account is klaar.", "Probeer het opslaan zo opnieuw."),
                arrayOf(
                    GuestSignupState.Review,
                    "en",
                    "Your trip needs a new check.",
                    "Review your trip again before creating your account.",
                ),
                arrayOf(
                    GuestSignupState.Review,
                    "nl",
                    "Je reis moet opnieuw worden gecontroleerd.",
                    "Bekijk je reis opnieuw voordat je je account maakt.",
                ),
            )
    }
}

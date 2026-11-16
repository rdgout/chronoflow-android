package net.chronoflow.core.ui.l10n

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class CopyIndexResolutionTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun text(
        table: CopyTable,
        key: String,
    ): String = context.getString(CopyIndex.find(table, key)!!.resId)

    @Test
    @Config(qualifiers = "nl")
    fun earlyBirdDutchDiffersPerTable() {
        assertThat(text(CopyTable.Settings, "Early bird")).isEqualTo("Vroege vogel")
        assertThat(text(CopyTable.Wizard, "Early bird")).isEqualTo("Ochtendmens")
    }

    @Test
    @Config(qualifiers = "nl")
    fun dutchPluralPicksQuantity() {
        val id = CopyIndex.find(CopyTable.Settings, "%lld credits left")!!.resId
        assertThat(context.resources.getQuantityString(id, 1, 1)).isEqualTo("Nog 1 tegoed over")
        assertThat(context.resources.getQuantityString(id, 3, 3)).isEqualTo("Nog 3 tegoeden over")
    }

    @Test
    @Config(qualifiers = "en")
    fun leadingSpaceSurvives() {
        val id = CopyIndex.find(CopyTable.Screens, " · {{time}}")!!.resId
        assertThat(context.getString(id, "14:00")).isEqualTo(" · 14:00")
    }

    @Test
    @Config(qualifiers = "en")
    fun apostropheAndTermsTagsAreLiteral() {
        assertThat(text(CopyTable.Reminders, "Allow them in your phone's settings to get reminders."))
            .isEqualTo("Allow them in your phone's settings to get reminders.")
        assertThat(
            text(
                CopyTable.Auth,
                "By creating an account you agree to the <terms>terms</terms> and <privacy>privacy policy</privacy>.",
            ),
        ).isEqualTo(
            "By creating an account you agree to the <terms>terms</terms> and <privacy>privacy policy</privacy>.",
        )
    }

    @Test
    @Config(qualifiers = "nl")
    fun untranslatableFallsBackToDefault() {
        assertThat(context.getString(R.string.auth_xxxx_xxxx)).isEqualTo("xxxx-xxxx")
    }

    @Test
    @Config(qualifiers = "nl")
    fun overlayDutch() {
        assertThat(
            context.getString(R.string.android_two_factor_intro_b1),
        ).isEqualTo("Je hebt een authenticator-app nodig.")
    }
}

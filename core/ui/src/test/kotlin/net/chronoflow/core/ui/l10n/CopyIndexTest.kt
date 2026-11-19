package net.chronoflow.core.ui.l10n

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import org.junit.Test

class CopyIndexTest {
    @Test fun splitDutchKeysResolvePerTable() {
        assertThat(CopyIndex.find(CopyTable.Settings, "Early bird")!!.resId)
            .isNotEqualTo(CopyIndex.find(CopyTable.Wizard, "Early bird")!!.resId)
        assertThat(CopyIndex.find(CopyTable.Plan, "Sleep")!!.resId)
            .isNotEqualTo(CopyIndex.find(CopyTable.Wizard, "Sleep")!!.resId)
    }

    @Test fun catalogPluralHasCountArg() {
        val entry = CopyIndex.find(CopyTable.Settings, "%lld credits left")!!
        assertThat(entry.isPlural).isTrue()
        assertThat(entry.args).isEqualTo(listOf("%1"))
    }

    @Test fun countSpecialCaseNamesPositionOne() {
        val entry = CopyIndex.find(CopyTable.Screens, "Your {{count}} preparation days start on {{date}}.")!!
        assertThat(entry.args).isEqualTo(listOf("count", "date"))
    }

    @Test fun wizardPairIsOnePlural() {
        assertThat(CopyIndex.find(CopyTable.Wizard, "{{count}} nights")!!.isPlural).isTrue()
        assertThat(CopyIndex.find(CopyTable.Wizard, "{{count}} nights_one")).isNull()
    }

    @Test fun singleReplacementRedirectsToOverlay() {
        val key = "You need an authenticator app, like the one built into Passwords on iPhone."
        assertThat(CopyIndex.find(CopyTable.Settings, key)!!.resId).isEqualTo(R.string.android_two_factor_intro_b1)
    }

    @Test fun multiReplacementIsNull() {
        assertThat(CopyIndex.find(CopyTable.Reminders, "Open your phone's Settings.")).isNull()
    }

    @Test fun debugOnlyKeyIsNotIndexed() {
        assertThat(CopyIndex.find(CopyTable.App, "Debug route: %@")).isNull()
        assertThat(CopyIndex.find(CopyTable.App, "Today")).isNotNull()
    }

    @Test fun overlayIsIndexedByItsEnglishText() {
        val entry = CopyIndex.find(CopyTable.Android, "Recovery codes.txt")!!
        assertThat(entry.resId).isEqualTo(R.string.android_recovery_codes_file_name)
        assertThat(entry.isPlural).isFalse()
    }

    @Test fun unknownKeyIsNull() {
        assertThat(CopyIndex.find(CopyTable.Auth, "not a real key")).isNull()
    }
}

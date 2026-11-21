package net.chronoflow.feature.settings.components

import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPreferenceCopyTest {
    @Test fun drinkActionsResolveTheirCatalogKeys() {
        val fewer = checkNotNull(CopyIndex.find(CopyTable.Settings, "Fewer {{drink}}"))
        val more = checkNotNull(CopyIndex.find(CopyTable.Settings, "More {{drink}}"))
        assertEquals(R.string.settings_fewer_drink, fewer.resId)
        assertEquals(R.string.settings_more_drink, more.resId)
        assertEquals(listOf("drink"), fewer.args)
        assertEquals(listOf("drink"), more.args)
    }
}

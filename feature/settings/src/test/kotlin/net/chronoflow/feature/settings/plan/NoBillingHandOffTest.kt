package net.chronoflow.feature.settings.plan

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NoBillingHandOffTest {
    @Test fun settingsHasNoBillingHandOff() {
        val root =
            generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
                .first { File(it, "settings.gradle.kts").isFile }
        val forbidden =
            listOf(
                "chronoflow.app",
                "http",
                "Intent.ACTION_VIEW",
                "CustomTabsIntent",
                "openUri",
                "settings_update_your_payment_method_on_th_c27b37",
                "settings_update_your_payment_method_on_th_5c660c",
                "Buy credits or a plan",
                "On the website",
                "Continue to the website",
            )
        val violations =
            File(root, "feature/settings/src/main")
                .walkTopDown()
                .filter { it.isFile }
                .flatMap { file ->
                    val text = file.readText()
                    forbidden.filter(text::contains).map { "${file.relativeTo(root)}: $it" }
                }.toList()
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }
}

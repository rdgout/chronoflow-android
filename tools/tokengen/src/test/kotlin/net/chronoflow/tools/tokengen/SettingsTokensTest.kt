package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SettingsTokensTest {
    @Test
    fun settingsMetricDriftIsRejected() {
        val sources =
            parseJson(
                repoRoot.ios("Settings.source.json"),
            ).obj().values.map { it.obj().member("source").string() }
        val files =
            (
                sources +
                    listOf(
                        "SetPlanCredits",
                        "SetProfile",
                        "PasswordPrompt",
                        "Set2FAOn",
                        "SetBodyClock",
                        "SetPreferences",
                    ).map { "_design/canvas/$it.dc.html" } +
                    listOf("tools/tokengen/ios/Settings.source.json")
            ).distinct()
        val account = "_design/canvas/SetAccount.dc.html"
        val root =
            tempRoot(files) { file, text ->
                if (file == account) {
                    val first = text.indexOf("padding: 10px 16px")
                    val second = text.indexOf("padding: 10px 16px", first + 1)
                    text.substring(0, second) + "padding: 11px 16px" +
                        text.substring(second + "padding: 10px 16px".length)
                } else {
                    text
                }
            }
        try {
            val error = assertThrows(GenerationError::class.java) { settingsTokens(root, planUITokens(repoRoot)) }
            assertEquals("Settings metric changed: rowVertical", error.message)
        } finally {
            root.deleteRecursively()
        }
    }
}

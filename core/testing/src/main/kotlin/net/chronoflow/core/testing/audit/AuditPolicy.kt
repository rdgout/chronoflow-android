package net.chronoflow.core.testing.audit

enum class AuditCheck { TextContrast, ImageContrast, TouchTarget, TextSize, Other }

data class AuditFinding(
    val check: AuditCheck,
    val checkName: String,
    val testTag: String?,
    val enabled: Boolean,
    val actionable: Boolean,
    val fullyVisible: Boolean,
)

object AuditPolicy {
    private val welcomeContrast =
        setOf("welcome.eyebrow", "welcome.headline", "welcome.copy", "welcome.plan", "welcome.signIn")

    fun shouldIgnore(f: AuditFinding): Int? {
        val contrast = f.check == AuditCheck.TextContrast || f.check == AuditCheck.ImageContrast
        return when {
            f.check == AuditCheck.TextContrast && f.testTag in welcomeContrast -> 1
            f.check == AuditCheck.TextContrast && f.testTag == "sign-in.guest" -> 2
            !f.enabled && f.actionable && (contrast || f.check == AuditCheck.TouchTarget) -> 3
            contrast && !f.fullyVisible -> 4
            f.check == AuditCheck.TextSize -> 5
            else -> null
        }
    }
}

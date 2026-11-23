package net.chronoflow.konsist

import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.provider.KoContainingDeclarationProvider
import com.lemonappdev.konsist.api.provider.KoFullyQualifiedNameProvider
import com.lemonappdev.konsist.api.provider.KoTextProvider
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens

// Each exception needs the real Android/graph boundary recorded in docs/test-diet.md.
internal val keptAppTestBoundaries =
    mapOf(
        "net.chronoflow.app.AppGraphConstructionTest" to
            "StrictMode and guarded storage prove lazy graph construction.",
        "net.chronoflow.app.AppGraphSignOutWiringTest" to
            "Real teardown callbacks must navigate, erase files and revoke tokens.",
        "net.chronoflow.app.PendingLaunchIntentsTest" to
            "Real graph launch completion drains Android intents only after readiness.",
        "net.chronoflow.app.ViewModelExtrasTest" to
            "MainActivity supplies its default ViewModel creation extras.",
        "net.chronoflow.app.CfRootTest" to
            "Root pixels, input shielding, accessibility and system bars require composition/window state.",
        "net.chronoflow.app.CfRootHandOffTest" to
            "Overlay hand-off changes the actual child accessibility semantics.",
        "net.chronoflow.app.RootContentConstructionTest" to
            "Nav3 must preserve or replace the remembered destination subtree.",
        "net.chronoflow.app.RootContentTest" to
            "Actual destination identity and Activity recreation preserve the process route.",
        "net.chronoflow.app.RootRouteTransitionTest" to
            "Nav3 must remove the outgoing subtree before a crossfade can mask it.",
        "net.chronoflow.app.RootThemeTest" to
            "The Activity frame-commit callback gates font IO and system reduce motion.",
        "net.chronoflow.app.SeededScreenRecreationTest" to
            "Activity recreation must not reapply first-frame seed routing.",
        "net.chronoflow.app.WarmLaunchIntentTest" to
            "A fresh Activity in a warm process hands over its captured intent.",
        "net.chronoflow.app.WelcomeHandOffTest" to
            "Measured moon geometry, font visibility and fully-drawn reporting need layout.",
        "net.chronoflow.app.WelcomeSeamTest" to
            "The real back handler must return to the measured Welcome position.",
        "net.chronoflow.app.ProcessDeathRestoreTest" to
            "Saved-state registry restoration and splash ownership depend on lifecycle callbacks.",
        "net.chronoflow.app.links.LinkRecreationTest" to
            "Actual launch/onNewIntent/recreation callbacks scrub and capture secrets once.",
        "net.chronoflow.app.auth.PasskeyGraphTest" to
            "The real Activity attaches and detaches its credential anchor.",
        "net.chronoflow.app.auth.TwoFactorRoutingTest" to
            "Native back and recreation preserve the in-memory challenge/code.",
        "net.chronoflow.app.auth.VerificationSessionAdapterTest" to
            "The concrete adapter must preserve lazy sync initialization and release leases.",
        "net.chronoflow.app.auth.ForgotRecreationTest" to
            "Activity recreation retains the current form and native back handler.",
        "net.chronoflow.app.auth.ResetRecreationTest" to
            "Locale recreation must clear editable secrets and keep them out of saved state.",
        "net.chronoflow.app.auth.VerifyRoutingTest" to
            "The real system back handler must preserve the unverified session.",
        "net.chronoflow.app.auth.SignUpEntryScopeTest" to
            "Nav3 entry removal clears the model while recreation retains the current entry.",
        "net.chronoflow.app.shell.ShellRecreationTest" to
            "Nav3/Activity recreation retain entry owners, scroll and ViewModels.",
        // Narrow supporting declarations of those kept tests, including direct JVM graph lifetime tests.
        "net.chronoflow.app.SessionTestApplication" to
            "Software-cipher Application fixture for the kept real-graph tests.",
        "net.chronoflow.app.AppSessionFixture" to
            "JVM SessionGraphs epoch/scope and JDBC attach/detach fixture.",
        "net.chronoflow.app.withLinkActivity" to
            "WarmLaunchIntentTest needs lifecycle callbacks after the Intent has been scrubbed.",
        "net.chronoflow.app.auth.AuthResetActivityFixture" to
            "Shared seeded lifecycle/window fixture for Forgot/Reset/Verify recreation and back.",
        "net.chronoflow.app.auth.EntryScopeActivity" to
            "SignUpEntryScopeTest hosts the actual Nav3 entry owner across recreation.",
        "net.chronoflow.app.shell.ShellRecreationActivity" to
            "ShellRecreationTest hosts real Nav3 owners with fake screen dependencies.",
    )

private val boundaryCalls =
    listOf(
        Regex("""\b(?:AppGraph|SessionGraphs?|RootContent|CfRoot|MainActivity)\s*\("""),
        Regex("""\bActivityScenario\s*\.\s*(?:launch|launchActivityForResult)\s*(?:<[^()]*>)?\s*\("""),
        Regex(
            """\b(?:buildActivity|launchActivity)\s*(?:<[^()]*\bMainActivity\s*>\s*\(|""" +
                """\(\s*[^,)]*\bMainActivity\s*::)""",
        ),
    )
private val aliasedBoundary =
    Regex(
        """(?m)^\s*import\s+[\w.]+\.""" +
            """(AppGraph|SessionGraphs?|RootContent|CfRoot|MainActivity|ActivityScenario)\s+as\s+(\w+)""",
    )

private val boundaryNames =
    listOf("AppGraph", "SessionGraph", "MainActivity", "ActivityScenario", "RootContent", "CfRoot")

private fun executableTokens(
    text: String,
    aliases: Map<String, String>,
): String {
    val lexer = KotlinLexer()
    lexer.start(text)
    return buildString {
        while (lexer.tokenType != null) {
            val token = lexer.tokenType
            if (!KtTokens.COMMENTS.contains(token) && token != KtTokens.REGULAR_STRING_PART &&
                token != KtTokens.ESCAPE_SEQUENCE && token != KtTokens.CHARACTER_LITERAL
            ) {
                val value = lexer.tokenText.removeSurrounding("`")
                append(aliases[value] ?: value)
                append(' ')
            }
            lexer.advance()
        }
    }
}

internal fun unitTestsKeepAppBootsAtApprovedBoundaries(scope: KoScope) {
    val violations =
        scope.files
            .filter { file ->
                val module = moduleOf(file.path)
                (module == "app" || module?.startsWith("feature/") == true || module?.startsWith("core/") == true) &&
                    sourceSetOf(file.path)?.startsWith("test") == true
            }.filter { file -> boundaryNames.any(file.text::contains) }
            .flatMap { file ->
                val aliases = aliasedBoundary.findAll(file.text).associate { it.groupValues[2] to it.groupValues[1] }
                file
                    .declarations(includeNested = false, includeLocal = false)
                    .filter {
                        (it as? KoContainingDeclarationProvider)?.containingDeclaration is KoFileDeclaration
                    }.mapNotNull { declaration ->
                        val name = (declaration as? KoFullyQualifiedNameProvider)?.fullyQualifiedName
                        if (name in keptAppTestBoundaries) return@mapNotNull null
                        val text = (declaration as? KoTextProvider)?.text ?: return@mapNotNull null
                        val code = executableTokens(text, aliases)
                        val call = boundaryCalls.firstNotNullOfOrNull { it.find(code)?.value }
                        call?.let { "${file.path}: $name constructs/launches/composes $it" }
                    }
            }
    if (violations.isNotEmpty()) {
        throw AssertionError("unitTestsKeepAppBootsAtApprovedBoundaries violated:\n${violations.joinToString("\n")}")
    }
}

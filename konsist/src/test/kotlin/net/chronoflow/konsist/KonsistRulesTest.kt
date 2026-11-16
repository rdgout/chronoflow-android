package net.chronoflow.konsist

import assertk.all
import assertk.assertFailure
import assertk.assertions.messageContains
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import org.junit.Test

class KonsistRulesTest {
    private fun violations(rule: String): KoScope =
        Konsist.scopeFromDirectory("konsist/src/test/resources/violations/$rule")

    private fun allowed(rule: String): KoScope = Konsist.scopeFromDirectory("konsist/src/test/resources/allowed/$rule")

    @Test fun featuresDoNotImportOtherFeatures() = featuresDoNotImportOtherFeatures(projectScope)

    @Test fun featuresDoNotImportOtherFeaturesRejectsPlantedViolation() {
        assertFailure {
            featuresDoNotImportOtherFeatures(violations("featuresDoNotImportOtherFeatures"))
        }.messageContains("featuresDoNotImportOtherFeatures violated")
    }

    @Test fun featuresDoNotImportOtherFeaturesAllowsControl() =
        featuresDoNotImportOtherFeatures(allowed("featuresDoNotImportOtherFeatures"))

    @Test fun jvmCoreModulesImportNoAndroidOrCompose() = jvmCoreModulesImportNoAndroidOrCompose(projectScope)

    @Test fun jvmCoreModulesImportNoAndroidOrComposeRejectsPlantedViolation() {
        assertFailure {
            jvmCoreModulesImportNoAndroidOrCompose(violations("jvmCoreModulesImportNoAndroidOrCompose"))
        }.all {
            messageContains("UsesCompose.kt: androidx.compose.ui.graphics.Color")
            messageContains("UsesLog.kt: android.util.Log")
        }
    }

    @Test fun onlyAppImportsFeatures() = onlyAppImportsFeatures(projectScope)

    @Test fun onlyAppImportsFeaturesRejectsPlantedViolation() {
        assertFailure {
            onlyAppImportsFeatures(violations("onlyAppImportsFeatures"))
        }.messageContains("onlyAppImportsFeatures violated")
    }

    @Test fun noGlobalScope() = noGlobalScope(projectScope)

    @Test fun noGlobalScopeRejectsPlantedViolation() {
        assertFailure { noGlobalScope(violations("noGlobalScope")) }.messageContains("noGlobalScope violated")
    }

    @Test fun noRunBlockingOutsideTests() = noRunBlockingOutsideTests(projectScope)

    @Test fun noRunBlockingOutsideTestsRejectsPlantedViolation() {
        assertFailure {
            noRunBlockingOutsideTests(violations("noRunBlockingOutsideTests"))
        }.messageContains("noRunBlockingOutsideTests violated")
    }

    @Test fun noRunBlockingOutsideTestsAllowsControl() = noRunBlockingOutsideTests(allowed("noRunBlockingOutsideTests"))

    @Test fun noSleepInTests() = noSleepInTests(projectScope)

    @Test fun noSleepInTestsRejectsPlantedViolation() {
        assertFailure { noSleepInTests(violations("noSleepInTests")) }.all {
            messageContains("noSleepInTests violated")
            messageContains("SleepsUiTest.kt")
            messageContains("SleepsTest.kt")
        }
    }

    @Test fun viewModelsEndWithViewModel() = viewModelsEndWithViewModel(projectScope)

    @Test fun viewModelsEndWithViewModelRejectsPlantedViolation() {
        assertFailure {
            viewModelsEndWithViewModel(violations("viewModelsEndWithViewModel"))
        }.messageContains("viewModelsEndWithViewModel violated")
    }

    @Test fun routesAreSerializable() = routesAreSerializable(projectScope)

    @Test fun routesAreSerializableRejectsPlantedViolation() {
        assertFailure {
            routesAreSerializable(violations("routesAreSerializable"))
        }.messageContains("routesAreSerializable violated")
    }

    @Test fun routesAreSerializableAllowsControl() = routesAreSerializable(allowed("routesAreSerializable"))

    @Test fun noLiteralTextInText() = noLiteralTextInText(projectScope)

    @Test fun noLiteralTextInTextRejectsPlantedViolation() {
        assertFailure { noLiteralTextInText(violations("noLiteralTextInText")) }.all {
            messageContains("noLiteralTextInText violated")
            messageContains("TextLiteral.kt: positional")
            messageContains("TextLiteral.kt: named")
            messageContains("TextLiteral.kt: raw")
            messageContains("TextLiteral.kt: namedNotFirst")
        }
    }

    @Test fun noLiteralTextInTextAllowsControl() = noLiteralTextInText(allowed("noLiteralTextInText"))

    @Test fun insetsOnlyAtRoot() = insetsOnlyAtRoot(projectScope)

    @Test fun insetsOnlyAtRootRejectsPlantedViolation() {
        assertFailure { insetsOnlyAtRoot(violations("insetsOnlyAtRoot")) }.messageContains("insetsOnlyAtRoot violated")
    }

    @Test fun testingNotInMain() = testingNotInMain(projectScope)

    @Test fun testingNotInMainRejectsPlantedViolation() {
        assertFailure { testingNotInMain(violations("testingNotInMain")) }.messageContains("UsesFixtures.kt")
    }

    private companion object {
        val projectScope: KoScope by lazy { projectScope() }
    }
}

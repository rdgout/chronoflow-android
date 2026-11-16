package net.chronoflow.konsist

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
        }.messageContains("jvmCoreModulesImportNoAndroidOrCompose violated")
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
        assertFailure { noSleepInTests(violations("noSleepInTests")) }.messageContains("noSleepInTests violated")
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

    private companion object {
        val projectScope: KoScope by lazy { projectScope() }
    }
}

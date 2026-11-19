package net.chronoflow.konsist

import assertk.all
import assertk.assertFailure
import assertk.assertions.messageContains
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import org.junit.Assert.assertTrue
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

    @Test fun launchModelImportsNoAndroid() = launchModelImportsNoAndroid(projectScope)

    @Test fun launchModelImportsNoAndroidRejectsPlantedViolation() {
        assertFailure { launchModelImportsNoAndroid(violations("launchModelImportsNoAndroid")) }
            .all {
                messageContains("launchModelImportsNoAndroid violated")
                messageContains("CoreLaunch.kt: androidx.lifecycle.ViewModel")
                messageContains("Example.kt: android.")
            }
    }

    @Test fun launchModelImportsNoAndroidAllowsControl() =
        launchModelImportsNoAndroid(
            allowed("launchModelImportsNoAndroid"),
        )

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

    @Test fun noMultiProcessDataStore() = noMultiProcessDataStore(projectScope)

    @Test fun noMultiProcessDataStoreRejectsPlantedViolations() {
        assertFailure { noMultiProcessDataStore(violations("noMultiProcessDataStore")) }.all {
            messageContains("noMultiProcessDataStore violated")
            messageContains("ImportedFactory.kt")
            messageContains("AliasedFactory.kt")
            messageContains("QualifiedFactory.kt")
        }
    }

    @Test fun noMultiProcessDataStoreRejectsCoordinatorEntryPoints() {
        assertFailure { noMultiProcessDataStore(violations("noMultiProcessCoordinator")) }.all {
            messageContains("noMultiProcessDataStore violated")
            messageContains("CreatedCoordinator.kt")
            messageContains("ConstructedCoordinator.kt")
        }
    }

    @Test fun noMultiProcessDataStoreAllowsSingleProcessFactory() =
        noMultiProcessDataStore(allowed("noMultiProcessDataStore"))

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

    @Test fun insetsOnlyAtRootAllowsRootContent() = insetsOnlyAtRoot(allowed("insetsOnlyAtRoot"))

    @Test fun testingNotInMain() = testingNotInMain(projectScope)

    @Test fun testingNotInMainRejectsPlantedViolation() {
        assertFailure { testingNotInMain(violations("testingNotInMain")) }.messageContains("UsesFixtures.kt")
    }

    private companion object {
        val projectScope: KoScope by lazy { projectScope() }
    }

    @Test fun noDefaultLocaleZoneOrClockInLogic() = noDefaultLocaleZoneOrClockInLogic(projectScope)

    @Test fun noDefaultLocaleZoneOrClockInLogicRejectsPlantedViolation() {
        assertFailure { noDefaultLocaleZoneOrClockInLogic(violations("noDefaultLocaleZoneOrClockInLogic")) }.all {
            messageContains("noDefaultLocaleZoneOrClockInLogic violated")
            messageContains("UsesLocaleGetDefault.kt")
            messageContains("UsesTimeZoneGetDefault.kt")
            messageContains("UsesZoneIdSystemDefault.kt")
            messageContains("UsesCurrentTimeMillis.kt")
            messageContains("UsesInstantNow.kt")
            messageContains("UsesDateNow.kt")
            messageContains("UsesClockSystem.kt")
            messageContains("UsesLocalDateTimeNow.kt")
            messageContains("UsesZonedDateTimeNow.kt")
            messageContains("UsesOffsetDateTimeNow.kt")
            messageContains("UsesClockSystemDefaultZone.kt")
            messageContains("UsesOfPatternWithoutLocale.kt")
            messageContains("UsesNumberFormatInstance.kt")
            messageContains("UsesDecimalFormat.kt")
            messageContains("UsesDecimalFormatSymbols.kt")
            messageContains("UsesDecimalFormatSymbolsGetInstance.kt")
            messageContains("UsesLocalTimeNow.kt")
            messageContains("UsesSimpleDateFormat.kt")
            messageContains("UsesCalendarInstance.kt")
            messageContains("UsesLocalizedFormatter.kt")
            messageContains("UsesStringFormat.kt")
            messageContains("UsesLiteralFormat.kt")
            messageContains("SyncUsesLocale.kt")
        }
    }

    @Test fun noDefaultLocaleZoneOrClockInLogicAllowsControl() =
        noDefaultLocaleZoneOrClockInLogic(allowed("noDefaultLocaleZoneOrClockInLogic"))

    @Test fun noMaterialColorSchemeOutsideDesignSystem() = noMaterialColorSchemeOutsideDesignSystem(projectScope)

    @Test fun noMaterialColorSchemeOutsideDesignSystemRejectsPlantedViolation() {
        assertFailure {
            noMaterialColorSchemeOutsideDesignSystem(violations("noMaterialColorSchemeOutsideDesignSystem"))
        }.messageContains("noMaterialColorSchemeOutsideDesignSystem violated")
    }

    @Test fun noMaterialColorSchemeOutsideDesignSystemAllowsControl() =
        noMaterialColorSchemeOutsideDesignSystem(allowed("noMaterialColorSchemeOutsideDesignSystem"))

    @Test fun noRawColorsOutsideDesignSystem() = noRawColorsOutsideDesignSystem(projectScope)

    @Test fun noRawColorsOutsideDesignSystemRejectsPlantedViolation() {
        assertFailure {
            noRawColorsOutsideDesignSystem(violations("noRawColorsOutsideDesignSystem"))
        }.messageContains("noRawColorsOutsideDesignSystem violated")
    }

    @Test fun noRawColorsOutsideDesignSystemAllowsControl() =
        noRawColorsOutsideDesignSystem(allowed("noRawColorsOutsideDesignSystem"))

    @Test fun featureDpLiteralsComeFromBoardMetrics() = featureDpLiteralsComeFromBoardMetrics(projectScope)

    @Test fun featureDpLiteralsComeFromBoardMetricsRejectsPlantedViolation() {
        assertFailure {
            featureDpLiteralsComeFromBoardMetrics(violations("featureDpLiteralsComeFromBoardMetrics"))
        }.messageContains("featureDpLiteralsComeFromBoardMetrics violated")
    }

    @Test fun featureNumericDpConstantsRejectPlantedViolation() {
        val path =
            "konsist/src/test/resources/violations/featureDpLiteralsComeFromBoardMetrics/" +
                "feature/trip/src/main/kotlin/net/chronoflow/Constants.kt"
        assertFailure { featureDpLiteralsComeFromBoardMetrics(Konsist.scopeFromFile(path)) }.all {
            messageContains("SIZE.dp")
            messageContains("COMPANION.sp")
            messageContains("DOUBLE.toFloat().dp")
            messageContains("INT.toInt().sp")
        }
    }

    @Test fun featureDpLiteralsComeFromBoardMetricsAllowsControl() =
        featureDpLiteralsComeFromBoardMetrics(allowed("featureDpLiteralsComeFromBoardMetrics"))

    @Test fun boardMetricsNamesExist() = boardMetricsNamesExist(projectScope)

    @Test fun boardMetricsNamesExistRejectsPlantedViolation() {
        assertFailure {
            boardMetricsNamesExist(violations("boardMetricsNamesExist"))
        }.messageContains("boardMetricsNamesExist violated")
    }

    @Test fun boardMetricsNamesExistAllowsControl() = boardMetricsNamesExist(allowed("boardMetricsNamesExist"))

    @Test fun noMaterialIconsLibrary() = noMaterialIconsLibrary(projectScope)

    @Test fun noMaterialIconsLibraryRejectsPlantedViolation() {
        assertFailure {
            noMaterialIconsLibrary(violations("noMaterialIconsLibrary"))
        }.messageContains("noMaterialIconsLibrary violated")
    }

    @Test fun noMaterialIconsLibraryAllowsControl() = noMaterialIconsLibrary(allowed("noMaterialIconsLibrary"))

    @Test fun secureFieldStateIsNeverSaved() = secureFieldStateIsNeverSaved(projectScope)

    @Test fun secureFieldStateIsNeverSavedRejectsPlantedViolation() {
        assertFailure {
            secureFieldStateIsNeverSaved(violations("secureFieldStateIsNeverSaved"))
        }.messageContains("secureFieldStateIsNeverSaved violated")
    }

    @Test fun secureFieldStateIsNeverSavedAllowsControl() =
        secureFieldStateIsNeverSaved(allowed("secureFieldStateIsNeverSaved"))

    @Test fun designSystemPrimitivesHaveGuards() = designSystemPrimitivesHaveGuards(projectScope)

    @Test fun designSystemPrimitivesHaveGuardsRejectsPlantedViolation() {
        assertFailure {
            designSystemPrimitivesHaveGuards(
                violations("designSystemPrimitivesHaveGuards"),
                java.io.File("build/no-guards"),
            )
        }.messageContains("designSystemPrimitivesHaveGuards violated")
    }

    @Test fun designSystemPrimitivesHaveGuardsRejectsCommentOnlyCoverage() {
        val guards =
            java.io.File(
                projectRoot(),
                "konsist/src/test/resources/violations/designSystemPrimitivesHaveGuards/guards",
            )
        val fixture = java.io.File(guards, "CommentOnlyGuardsTest.kt")
        assertTrue("Missing comment-only guard fixture: $fixture", fixture.isFile)
        assertTrue("Missing planted call mention", fixture.readText().contains("CfOrphan()"))
        assertFailure {
            designSystemPrimitivesHaveGuards(violations("designSystemPrimitivesHaveGuards"), guards)
        }.messageContains("CfOrphan")
    }

    @Test fun designSystemPrimitivesHaveGuardsAllowsExecutableCoverage() {
        val guards =
            java.io.File(
                projectRoot(),
                "konsist/src/test/resources/allowed/designSystemPrimitivesHaveGuards/guards",
            )
        assertTrue("Missing executable guard fixture", java.io.File(guards, "ExecutableGuardsTest.kt").isFile)
        designSystemPrimitivesHaveGuards(allowed("designSystemPrimitivesHaveGuards"), guards)
    }
}

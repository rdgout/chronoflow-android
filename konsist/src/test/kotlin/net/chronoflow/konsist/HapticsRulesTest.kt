package net.chronoflow.konsist

import assertk.assertFailure
import assertk.assertions.messageContains
import com.lemonappdev.konsist.api.Konsist
import org.junit.Test

class HapticsRulesTest : KonsistTest() {
    @Test fun noHapticsInLaunch() = noHapticsInLaunch(projectScope())

    @Test fun noHapticsInLaunchRejectsPlantedViolation() {
        val scope = Konsist.scopeFromDirectory("konsist/src/test/resources/violations/noHapticsInLaunch")
        assertFailure { noHapticsInLaunch(scope) }.messageContains("noHapticsInLaunch violated")
    }

    @Test fun noHapticsInLaunchAllowsControl() =
        noHapticsInLaunch(Konsist.scopeFromDirectory("konsist/src/test/resources/allowed/noHapticsInLaunch"))
}

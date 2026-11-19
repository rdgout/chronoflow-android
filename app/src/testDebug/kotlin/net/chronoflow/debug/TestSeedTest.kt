package net.chronoflow.debug

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.model.config.ThemeChoice
import org.junit.Test

class TestSeedTest {
    private val id = "0a1b2c3d-4e5f-6789-abcd-ef0123456789"

    private fun seed(vararg pairs: Pair<String, String>) = TestSeed.parse(mapOf(*pairs))

    private fun withId(vararg pairs: Pair<String, String>) = seed(TestSeed.TEST_ID_KEY to id, *pairs)

    @Test
    fun canonicalUuidIsKeptAndUpperCaseIsLowered() {
        assertThat(seed(TestSeed.TEST_ID_KEY to id).testId).isEqualTo(id)
        assertThat(seed(TestSeed.TEST_ID_KEY to id.uppercase()).testId).isEqualTo(id)
    }

    @Test
    fun invalidIdsDropEveryCfKey() {
        for (bad in listOf("1-1-1-1-1", "", id + "0")) {
            val result = seed(TestSeed.TEST_ID_KEY to bad, TestSeed.PROFILE_KEY to "screen")
            assertThat(result.testId).isNull()
            assertThat(result.profile).isNull()
            assertThat(result.ignoredKeys).containsExactlyInAnyOrder(TestSeed.TEST_ID_KEY, TestSeed.PROFILE_KEY)
        }
    }

    @Test
    fun cfDotKeysApplyWithoutAnId() {
        assertThat(seed("cf.settings.language" to "nl").language).isEqualTo(LanguageChoice.Nl)
    }

    @Test
    fun profileMustMatchExactly() {
        assertThat(withId(TestSeed.PROFILE_KEY to "screen").profile).isEqualTo(UiTestProfile.Screen)
        assertThat(withId(TestSeed.PROFILE_KEY to "launch").profile).isEqualTo(UiTestProfile.Launch)
        assertThat(withId(TestSeed.PROFILE_KEY to "Screen").profile).isNull()
        assertThat(withId(TestSeed.PROFILE_KEY to "fast").profile).isNull()
    }

    @Test
    fun themesAndLanguagesAreValidated() {
        assertThat(seed("cf.settings.theme" to "dark").theme).isEqualTo(ThemeChoice.Dark)
        assertThat(seed("cf.settings.theme" to "Dark").theme).isNull()
        assertThat(seed("cf.settings.language" to "system").language).isEqualTo(LanguageChoice.System)
        assertThat(seed("cf.settings.language" to "de").language).isNull()
    }

    @Test
    fun reduceMotionAcceptsOnlyBooleans() {
        assertThat(seed("cf.reduceMotion" to "1").reduceMotion).isEqualTo(true)
        assertThat(seed("cf.reduceMotion" to "true").reduceMotion).isEqualTo(true)
        assertThat(seed("cf.reduceMotion" to "0").reduceMotion).isEqualTo(false)
        assertThat(seed("cf.reduceMotion" to "false").reduceMotion).isEqualTo(false)
        assertThat(seed("cf.reduceMotion" to "yes").reduceMotion).isNull()
    }

    @Test
    fun millisecondsStayInRangeAndFinite() {
        assertThat(seed("cf.freezeAt" to "0").freezeAtMs).isEqualTo(0.0)
        assertThat(seed("cf.freezeAt" to "60000").freezeAtMs).isEqualTo(60000.0)
        assertThat(seed("cf.freezeAt" to "1500.5").freezeAtMs).isEqualTo(1500.5)
        for (bad in listOf("60000.1", "-1", "NaN", "Infinity", "abc")) {
            assertThat(seed("cf.freezeAt" to bad).freezeAtMs).isNull()
        }
        assertThat(seed("cf.readyAt" to "4600").readyAtMs).isEqualTo(4600.0)
        assertThat(seed("cf.mainThreadStall" to "250").mainThreadStallMs).isEqualTo(250.0)
    }

    @Test
    fun isoInstantKeepsRawForThePlanClock() {
        val result = seed("cf.freezeAt" to "2026-10-13T08:00:00Z")
        assertThat(result.freezeAtMs).isNull()
        assertThat(result.freezeAtRaw).isEqualTo("2026-10-13T08:00:00Z")
    }

    @Test
    fun nonStringExtrasAreIgnored() {
        val result = TestSeed.parse(mapOf("cf.settings.theme" to "dark"), nonStringKeys = setOf("cf.reduceMotion"))
        assertThat(result.ignoredKeys).containsExactlyInAnyOrder("cf.reduceMotion")
        assertThat(result.reduceMotion).isNull()
    }

    @Test
    fun unknownCfKeysWithAnIdLandInRaw() {
        assertThat(withId("CF_FOO" to "bar").raw["CF_FOO"]).isEqualTo("bar")
        assertThat(withId("CF_FOO" to "bar").ignoredKeys).isEmpty()
    }

    @Test
    fun noExtrasGiveAnEmptySeed() {
        val result = TestSeed.parse(emptyMap())
        assertThat(result.testId).isNull()
        assertThat(result.raw).isEmpty()
    }

    @Test fun holdSplashUsesTheUngatedBooleanSeedParser() {
        for ((raw, expected) in listOf(
            "true" to true,
            "1" to true,
            "false" to false,
            "0" to false,
            "yes" to null,
            "" to null,
        )) {
            org.junit.Assert.assertEquals(expected, TestSeed.parse(mapOf("cf.holdSplash" to raw)).holdSplash)
        }
        org.junit.Assert.assertNull(TestSeed.parse(emptyMap()).holdSplash)
    }
}

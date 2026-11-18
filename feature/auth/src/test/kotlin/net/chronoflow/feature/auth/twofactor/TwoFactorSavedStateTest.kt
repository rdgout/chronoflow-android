package net.chronoflow.feature.auth.twofactor

import android.os.Bundle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TwoFactorSavedStateTest : TwoFactorTestFixture() {
    @Test fun nothingSavedContainsASecret() {
        prepare()
        val registry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
        compose.setContent { CompositionLocalProvider(LocalSaveableStateRegistry provides registry) { Content() } }
        compose.onNodeWithTag(tag).performTextInput("123456")
        flush()
        val saved = flatten(registry.performSave()).joinToString()
        assertFalse(saved.contains("123456"))
        assertFalse(saved.contains(checkNotNull(signIn.navigation.challengeID)))
    }

    @Test fun restorationKeepsTheMemoryCodeAndChallenge() {
        prepare()
        val challenge = signIn.navigation.challengeID
        val restoration = StateRestorationTester(compose)
        restoration.setContent { Content() }
        compose.onNodeWithTag(tag).performTextInput("123456")
        flush()
        restoration.emulateSavedInstanceStateRestore()
        flush()
        assertEquals("123456", fieldValue())
        assertEquals(challenge, signIn.navigation.challengeID)
    }

    @Suppress("DEPRECATION")
    private fun flatten(value: Any?): List<String> =
        when (value) {
            null -> emptyList()
            is Bundle -> value.keySet().flatMap { flatten(value.get(it)) }
            is Map<*, *> -> value.entries.flatMap { flatten(it.key) + flatten(it.value) }
            is Iterable<*> -> value.flatMap(::flatten)
            is Array<*> -> value.flatMap(::flatten)
            is CharArray -> listOf(String(value))
            is ByteArray -> listOf(value.decodeToString())
            else -> listOf(value.toString())
        }
}

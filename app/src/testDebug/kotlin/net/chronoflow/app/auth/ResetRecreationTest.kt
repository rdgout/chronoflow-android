package net.chronoflow.app.auth

import android.os.Bundle
import android.util.SparseArray
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.ResetPasswordModel
import net.chronoflow.feature.auth.model.SecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = SessionTestApplication::class)
class ResetRecreationTest : AuthResetActivityFixture() {
    @Test fun localeRecreationDropsPasswordsKeepsTheLinkAndSavesNoSecrets() {
        launch("reset")
        val model = checkNotNull(graph.auth.resetModel)
        val link = model.link
        val password = "NewPassword12!"
        val confirmation = "NewConfirmation12!"
        compose.onNodeWithTag("reset.password").performTextInput(password)
        compose.onNodeWithTag("reset.confirmation").performTextInput(confirmation)
        val saved = flatten(savedBundle())
        assertFalse(saved.any { it.contains(link.token) || it.contains(password) || it.contains(confirmation) })
        RuntimeEnvironment.setQualifiers("+nl-rNL")
        recreate(Route.Reset(link.email))
        assertSame(model, graph.auth.resetModel)
        assertEquals(link, model.link)
        assertEquals(ResetPasswordModel.State.Form, model.state.value)
        assertEquals(
            0,
            compose
                .onNodeWithTag("reset.password")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .length,
        )
        assertEquals(
            0,
            compose
                .onNodeWithTag(
                    "reset.confirmation",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .length,
        )
    }

    @Test fun doneContinueReturnsToSignIn() {
        done()
        compose.onNodeWithTag("reset.continue").performClick()
        waitFor { graph.rootNavigator.state.value.route == Route.SignIn }
    }

    @Test fun doneSystemBackReturnsToSignIn() {
        done()
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { graph.rootNavigator.state.value.route == Route.SignIn }
    }

    private fun done() {
        launch("reset")
        val model = checkNotNull(graph.auth.resetModel)
        runTest(dispatcher, timeout = 60.seconds) { model.submit(NewSecret(), NewSecret()) }
        waitFor { model.state.value == ResetPasswordModel.State.Done }
        compose.onNodeWithTag("reset.continue").assertExists()
    }

    @Suppress("DEPRECATION")
    private fun flatten(value: Any?): List<String> =
        when (value) {
            null -> emptyList()
            is Bundle -> value.keySet().flatMap { listOf(it) + flatten(value.get(it)) }
            is SparseArray<*> -> (0 until value.size()).flatMap { flatten(value.valueAt(it)) }
            is Map<*, *> -> value.entries.flatMap { flatten(it.key) + flatten(it.value) }
            is Iterable<*> -> value.flatMap(::flatten)
            is Array<*> -> value.flatMap(::flatten)
            is CharArray -> listOf(String(value))
            is ByteArray -> listOf(value.decodeToString())
            else -> listOf(value.toString())
        }
}

private class NewSecret : SecretField {
    private var value = "NewPassword12!"

    override fun read(): String = value

    override fun clear() {
        value = ""
    }

    override fun toString(): String = "NewSecret(<redacted>)"
}

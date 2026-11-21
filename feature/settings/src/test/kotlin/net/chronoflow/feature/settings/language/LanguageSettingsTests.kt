@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.language

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.config.ConfigurationRules
import net.chronoflow.core.model.config.LanguageChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class LanguageSettingsTests {
    @Test fun localLanguageAndThemePersistAndUnsupportedPhoneLanguageFallsBackToEnglish() =
        runTest(timeout = 60.seconds) {
            val storage = mutableMapOf("cf.settings.language" to "system", "cf.settings.theme" to "dark")
            val deps = FakeLanguageDependencies(storage)
            deps.phone = "nl"
            withLanguage(deps) { vm ->
                assertEquals(LanguageChoice.System, vm.ui.value.choice)
                assertEquals("nl", vm.ui.value.phone)
                deps.phone = "fr"
                assertEquals("en", deps.phoneLanguage())
                vm.select(LanguageChoice.Nl)
                runCurrent()
                assertEquals("nl", storage["cf.settings.language"])
                assertEquals("dark", storage["cf.settings.theme"])
            }
            withLanguage(
                FakeLanguageDependencies(storage),
            ) { restored -> assertEquals(LanguageChoice.Nl, restored.ui.value.choice) }
        }

    @Test fun selectingCurrentChoiceDoesNothing() =
        runTest(timeout = 60.seconds) {
            val deps = FakeLanguageDependencies()
            withLanguage(deps) { vm ->
                vm.select(LanguageChoice.System)
                runCurrent()
                assertEquals(0, deps.writes)
                assertNull(deps.feedback.value)
                assertNull(vm.ui.value.recreate)
                assertNull(vm.ui.value.notice)
            }
        }

    @Test fun feedbackIsConsumedOnce() =
        runTest(timeout = 60.seconds) {
            val deps = FakeLanguageDependencies()
            withLanguage(deps) { vm ->
                vm.select(LanguageChoice.Nl)
                runCurrent()
                assertNotNull(vm.ui.value.recreate)
                assertEquals(LanguageChoice.Nl, deps.feedback.value)
                vm.arrived()
                val notice = checkNotNull(vm.ui.value.notice)
                assertEquals(LanguageChoice.Nl, notice.choice)
                assertNull(deps.feedback.value)
                vm.arrived()
                assertEquals(notice, vm.ui.value.notice)
                vm.presented(notice.id + 1)
                assertEquals(notice, vm.ui.value.notice)
                vm.presented(notice.id)
                assertNull(vm.ui.value.notice)
                withLanguage(deps) { next ->
                    next.arrived()
                    assertNull(next.ui.value.notice)
                }
            }
        }

    @Test fun dependencyCancellationDoesNotEscapeSubmit() =
        runTest(timeout = 60.seconds) {
            val deps = FakeLanguageDependencies().apply { failure = CancellationException() }
            withLanguage(deps) { vm ->
                vm.select(LanguageChoice.Nl)
                runCurrent()
                assertNotNull(vm.failure)
                assertFalse(vm.ui.value.busy)
                assertNull(deps.feedback.value)
            }
        }
}

internal class FakeLanguageDependencies(
    private val storage: MutableMap<String, String> = mutableMapOf("cf.settings.language" to "system"),
) : LanguageDependencies {
    override val current =
        MutableStateFlow(LanguageChoice.fromRaw(storage["cf.settings.language"]) ?: LanguageChoice.System)
    override val feedback = MutableStateFlow<LanguageChoice?>(null)
    var phone = "en"
    var writes = 0
    var failure: Throwable? = null

    override fun phoneLanguage() =
        ConfigurationRules.resolveLanguage(LanguageChoice.System, Locale.forLanguageTag(phone)).tag

    override suspend fun apply(choice: LanguageChoice): Boolean {
        failure?.let { throw it }
        writes++
        storage["cf.settings.language"] = choice.raw
        current.value = choice
        return true
    }
}

private suspend fun TestScope.withLanguage(
    deps: LanguageDependencies,
    block: suspend (LanguageSettingsViewModel) -> Unit,
) {
    val vm = LanguageSettingsViewModel(deps, StandardTestDispatcher(testScheduler))
    val store = ViewModelStore().apply { put("language", vm) }
    try {
        runCurrent()
        block(vm)
    } finally {
        withContext(NonCancellable) {
            store.clear()
            vm.viewModelScope.coroutineContext.job
                .cancelAndJoin()
        }
    }
}

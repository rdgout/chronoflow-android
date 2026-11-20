@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.account

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class AccountViewModelTest {
    @Test fun retryRestartsBothObservationsAndRequestsSync() =
        runTest(timeout = 60.seconds) {
            val fake = FakeSettingsDependencies()
            var accounts = 0
            var settings = 0
            val deps =
                object : SettingsDependencies by fake {
                    override fun account() =
                        flow {
                            accounts++
                            if (accounts == 1) throw StoreFailure.Closed
                            emit(fake.account.value)
                            awaitCancellation()
                        }

                    override fun settings() =
                        flow {
                            settings++
                            emit(fake.settings.value)
                            awaitCancellation()
                        }
                }
            val vm = AccountViewModel(deps, StandardTestDispatcher(testScheduler))
            val store = ViewModelStore().apply { put("account", vm) }
            try {
                runCurrent()
                assertTrue(vm.ui.value.failed)
                assertFalse(vm.ui.value.loaded)
                vm.retry()
                runCurrent()
                assertEquals(2, accounts)
                assertEquals(2, settings)
                assertEquals(1, fake.requests)
                assertTrue(vm.ui.value.loaded)
                assertFalse(vm.ui.value.failed)
                assertEquals(fake.account.value, vm.ui.value.account)
            } finally {
                withContext(NonCancellable) {
                    store.clear()
                    vm.viewModelScope.coroutineContext.job
                        .cancelAndJoin()
                }
            }
        }
}

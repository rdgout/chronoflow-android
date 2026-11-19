package net.chronoflow.core.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.database.testing.TestRoots
import net.chronoflow.core.database.testing.accountA
import net.chronoflow.core.model.store.StoreFailure
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class InvalidAccountPathsTests(
    private val value: String,
) {
    private val root = TestRoots.temporaryStoreRoot()

    @After fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun rejectsInvalidPaths() =
        runTest(timeout = 60.seconds) {
            val db = AccountDatabase(root, driverFactory = JdbcAccountDriverFactory(), io = Dispatchers.IO)
            try {
                db.open(value)
                fail()
            } catch (_: StoreFailure.InvalidAccountID) {
            }
            assertEquals(emptyList<String>(), root.list()!!.toList())
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}")
        fun cases() =
            listOf(
                "",
                "../account",
                "/tmp/escape",
                "account",
                "$accountA/x",
                " $accountA",
                "{$accountA}",
            )
    }
}

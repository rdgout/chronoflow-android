package net.chronoflow.core.data.wizard

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.wizard.support.WizardStore
import net.chronoflow.core.database.store.fetchDraft
import net.chronoflow.core.model.store.DraftRecord
import net.chronoflow.core.wizard.model.MemberWizardRecord
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class DraftRepositoryJsonValidationTest(
    private val column: String,
) {
    @Test fun invalidJsonCannotEnterInsertOrReplacement() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardStore(StandardTestDispatcher(testScheduler))
            try {
                val base = DraftRecord(MemberWizardRecord.ID, form = "{}", updatedAt = "now")
                val bad = "{bad"
                val record =
                    when (column) {
                        "form" -> base.copy(form = bad)
                        "baseForm" -> base.copy(baseForm = bad)
                        "review" -> base.copy(review = bad)
                        "wizardReview" -> base.copy(wizardReview = bad)
                        "wizardState" -> base.copy(wizardState = bad)
                        "sourceRevision" -> base.copy(sourceRevision = bad)
                        "saveFailure" -> base.copy(saveFailure = bad)
                        "fieldErrors" -> base.copy(fieldErrors = bad)
                        else -> error("Unknown JSON column")
                    }
                val repository = DraftRepository(fixture.database)
                try {
                    repository.upsert(record)
                    error("Expected invalid JSON rejection")
                } catch (
                    _: IllegalArgumentException,
                ) {
                }
                assertNull(fixture.database.read { it.fetchDraft(MemberWizardRecord.ID) })
                try {
                    repository.createRecovery(record)
                    error("Expected invalid JSON rejection")
                } catch (
                    _: IllegalArgumentException,
                ) {
                }
                assertNull(fixture.database.read { it.fetchDraft(MemberWizardRecord.ID) })
            } finally {
                fixture.close()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "column={0}")
        fun columns(): List<Array<Any>> =
            listOf(
                "form",
                "baseForm",
                "review",
                "wizardReview",
                "wizardState",
                "sourceRevision",
                "saveFailure",
                "fieldErrors",
            ).map { arrayOf(it) }
    }
}

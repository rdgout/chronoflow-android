@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.onboarding

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.feature.onboarding.support.OnboardingFixtureTest
import net.chronoflow.feature.onboarding.support.withOnboardingFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class OnboardingSessionTests : OnboardingFixtureTest() {
    @Test fun enqueueRejectsAStaleSnapshotWithSessionChanged() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture { f ->
                val session = f.session()
                f.controller.signOut(discardPending = true)
                try {
                    session.enqueue(OutboxChange.Walkthrough)
                    fail("stale snapshot wrote")
                } catch (
                    _: SessionChangedException,
                ) {
                }
                assertNull(f.controller.activeSession())
                assertFalse(
                    f.database
                        .file(
                            net.chronoflow.core.model.AccountID
                                .of(f.result.account.id),
                        ).exists(),
                )
            }
        }

    @Test fun policiesFlowDecodesTheStoredSection() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture { f ->
                f.settings.remove("policies")
                f.session().policies.test {
                    assertNull(awaitItem())
                    val policies = AccountPolicies("new", "1")
                    f.settings.upsert(
                        SettingsRecord(
                            "policies",
                            ApiJson.response.encodeToString(AccountPolicies.serializer(), policies),
                        ),
                    )
                    assertEquals(policies, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
}

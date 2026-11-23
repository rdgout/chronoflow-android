@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class EndpointContractCoverageTest {
    @Test fun coversEveryDataSourceMethod() {
        val names =
            DataSource::class.java.declaredMethods
                .filterNot { it.isSynthetic }
                .map { it.name.removeSuffix("\$default") }
                .distinct()
                .toSet()
        assertThat(ENDPOINT_ROWS.map { it.method }.toSet()).isEqualTo(names)
        assertThat(names.size).isEqualTo(25)
        assertThat(ENDPOINT_ROWS.size).isEqualTo(42)
        assertThat(ENDPOINT_ROWS.filter { it.seconds == 90 }.map { "${it.verb} ${it.path}" }.toSet()).isEqualTo(
            setOf(
                "POST auth/register",
                "POST trips/review",
                "POST guest/trips/review",
                "POST trips/trip/review",
                "POST trips",
                "PUT trips/trip",
            ),
        )
        assertThat(
            ENDPOINT_ROWS
                .filter {
                    it.seconds == 3
                }.map { "${it.verb} ${it.path}" },
        ).isEqualTo(listOf("POST auth/logout"))
    }
}

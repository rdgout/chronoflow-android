@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package net.chronoflow.core.navigation

import kotlinx.serialization.descriptors.elementNames
import org.junit.Assert.assertEquals
import org.junit.Test

class ResetRouteSerializationTest {
    @Test fun resetHasNoTokenProperty() {
        assertEquals(
            listOf("email"),
            Route.Reset
                .serializer()
                .descriptor.elementNames
                .toList(),
        )
    }
}

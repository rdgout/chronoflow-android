package net.chronoflow.core.data.sync

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage

internal fun failureClassificationRows(message: String = "secret response"): List<Array<Any?>> =
    listOf(
        arrayOf("server400", DataFailure.Server(400), true, 0, null),
        arrayOf("validation422", DataFailure.Validation(mapOf("name" to FieldMessage(message)), 422), true, 0, null),
        arrayOf("server403", DataFailure.Server(403), true, 0, null),
        arrayOf("server404", DataFailure.Server(404), true, 0, null),
        arrayOf("server408", DataFailure.Server(408), false, 1, "server"),
        arrayOf("server425", DataFailure.Server(425), false, 1, "server"),
        arrayOf("throttled429", DataFailure.Throttled(900.0, 429), false, 1, "throttled"),
        arrayOf("server500", DataFailure.Server(500), false, 1, "server"),
        arrayOf("offline", DataFailure.Offline, false, 1, "offline"),
        arrayOf("unauthorized401", DataFailure.Unauthorized(401), false, 0, null),
        arrayOf("emailUnverified403", DataFailure.EmailUnverified(403), false, 0, null),
    )

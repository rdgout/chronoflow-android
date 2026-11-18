package net.chronoflow.core.datamock

import kotlinx.serialization.json.Json
import net.chronoflow.core.model.api.ApiJson

internal val MockJson =
    Json(ApiJson.response) {
        encodeDefaults = true
        explicitNulls = false
    }

package net.chronoflow.core.model.api

internal fun redacted(value: Any?): String = if (value == null) "null" else "***"

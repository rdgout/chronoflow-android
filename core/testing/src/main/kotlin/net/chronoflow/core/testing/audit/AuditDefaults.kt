package net.chronoflow.core.testing.audit

fun mergeAuditDefaults(
    testSeeds: Map<String, String>,
    args: Map<String, String>,
): Map<String, String> =
    buildMap {
        args["cf.audit.language"]?.let { put("cf.settings.language", it) }
        args["cf.audit.theme"]?.let { put("cf.settings.theme", it) }
        args["cf.audit.reduceMotion"]?.let { put("cf.reduceMotion", it) }
        putAll(testSeeds)
    }

package net.chronoflow.core.model.session

enum class StorageNamespace(
    val directoryName: String,
    val keychainService: String,
) {
    Api("ChronoFlow", "net.chronoflow.app.native"),
    Mock("ChronoFlowMock", "net.chronoflow.app.native.mock"),
    ;

    val sessionKeyAlias: String get() = "$keychainService.session"
}

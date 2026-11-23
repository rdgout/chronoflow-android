package net.chronoflow.core.model.config

enum class LanguageChoice(
    val raw: String,
) {
    System("system"),
    En("en"),
    Nl("nl"),
    ;

    companion object {
        fun fromRaw(raw: String?): LanguageChoice? = entries.firstOrNull { it.raw == raw }
    }
}

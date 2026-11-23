package net.chronoflow.feature.settings.security

interface SecretField {
    fun read(): String

    fun clear()
}

package net.chronoflow.feature.auth.model

interface SecretField {
    fun read(): String

    fun clear()
}

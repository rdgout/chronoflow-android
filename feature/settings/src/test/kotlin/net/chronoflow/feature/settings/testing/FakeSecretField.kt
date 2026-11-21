package net.chronoflow.feature.settings.testing

import net.chronoflow.feature.settings.security.SecretField

class FakeSecretField(
    private var value: String,
) : SecretField {
    var cleared = false
        private set

    override fun read(): String = value

    override fun clear() {
        value = ""
        cleared = true
    }

    override fun toString(): String = "FakeSecretField(***)"
}

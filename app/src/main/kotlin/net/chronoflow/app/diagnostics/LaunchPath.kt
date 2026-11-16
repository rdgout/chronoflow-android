package net.chronoflow.app.diagnostics

class LaunchPath {
    @Volatile
    var isActive: Boolean = true
        private set

    fun end() {
        isActive = false
    }
}

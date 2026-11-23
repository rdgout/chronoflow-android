package net.chronoflow.app

class SessionTestApplication : ChronoFlowApp() {
    override fun createGraph(): AppGraph =
        AppGraph(this, overrides = GraphOverrides(sessionCipher = SoftwareSessionCipher()))
}

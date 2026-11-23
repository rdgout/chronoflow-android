package net.chronoflow.feature.launch

interface LaunchTracing {
    fun beginLaunch()

    fun endLaunch()

    fun beginFirstCommit()

    fun endFirstCommit()
}

internal class LaunchTraceSession(
    private val begin: (String, Int) -> Unit,
    private val end: (String, Int) -> Unit,
) : LaunchTracing {
    private var launchBegan = false
    private var launchEnded = false
    private var commitBegan = false
    private var commitEnded = false

    override fun beginLaunch() {
        if (!launchBegan) {
            launchBegan = true
            begin("launch", 1)
        }
    }

    override fun endLaunch() {
        if (launchBegan && !launchEnded) {
            launchEnded = true
            end("launch", 1)
        }
    }

    override fun beginFirstCommit() {
        if (!commitBegan) {
            commitBegan = true
            begin("first-commit", 2)
        }
    }

    override fun endFirstCommit() {
        if (commitBegan && !commitEnded) {
            commitEnded = true
            end("first-commit", 2)
        }
    }
}

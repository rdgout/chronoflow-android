package net.chronoflow.feature.launch

import android.os.Trace

object LaunchTrace {
    private var launchBegan = false
    private var launchEnded = false
    private var commitBegan = false
    private var commitEnded = false

    fun beginLaunch() {
        if (!launchBegan) {
            launchBegan = true
            Trace.beginAsyncSection("launch", 1)
        }
    }

    fun endLaunch() {
        if (launchBegan && !launchEnded) {
            launchEnded = true
            Trace.endAsyncSection("launch", 1)
        }
    }

    fun beginFirstCommit() {
        if (!commitBegan) {
            commitBegan = true
            Trace.beginAsyncSection("first-commit", 2)
        }
    }

    fun endFirstCommit() {
        if (commitBegan && !commitEnded) {
            commitEnded = true
            Trace.endAsyncSection("first-commit", 2)
        }
    }

    inline fun <T> moonDecode(block: () -> T): T {
        Trace.beginSection("cf.launch.moonDecode")
        try {
            return block()
        } finally {
            Trace.endSection()
        }
    }
}

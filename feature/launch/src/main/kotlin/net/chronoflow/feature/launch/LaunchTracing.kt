package net.chronoflow.feature.launch

interface LaunchTracing {
    fun beginLaunch()

    fun endLaunch()

    fun beginFirstCommit()

    fun endFirstCommit()

    fun beginReady()

    fun endReady()

    fun beginExit()

    fun endExit()
}

internal class LaunchTraceSession(
    private val begin: (String, Int) -> Unit,
    private val end: (String, Int) -> Unit,
) : LaunchTracing {
    private val began = BooleanArray(4)
    private val ended = BooleanArray(4)

    override fun beginLaunch() = beginOnce("launch", 1)

    override fun endLaunch() = endOnce("launch", 1)

    override fun beginFirstCommit() = beginOnce("first-commit", 2)

    override fun endFirstCommit() = endOnce("first-commit", 2)

    override fun beginReady() = beginOnce("ready", 3)

    override fun endReady() = endOnce("ready", 3)

    override fun beginExit() = beginOnce("exit", 4)

    override fun endExit() = endOnce("exit", 4)

    private fun beginOnce(
        name: String,
        cookie: Int,
    ) {
        if (began[cookie - 1]) return
        began[cookie - 1] = true
        begin(name, cookie)
    }

    private fun endOnce(
        name: String,
        cookie: Int,
    ) {
        if (!began[cookie - 1] || ended[cookie - 1]) return
        ended[cookie - 1] = true
        end(name, cookie)
    }
}

package net.chronoflow.feature.launch.model

class MoonAnchorStore {
    var frame: LaunchRect? = null
        private set
    var onChange: ((LaunchRect) -> Unit)? = null

    fun update(frame: LaunchRect) {
        if (!frame.x.isFinite() || !frame.y.isFinite() || !frame.width.isFinite() || !frame.height.isFinite() ||
            frame.width <= 0 || frame.height <= 0 || this.frame == frame
        ) {
            return
        }
        this.frame = frame
        onChange?.invoke(frame)
    }
}

package eu.kanade.tachiyomi.ui.player.viewer

enum class PipState {
    OFF, ON, STARTED;

    companion object {
        internal var mode: PipState = OFF
    }
}

enum class SeekState {
    DOUBLE_TAP, LOCKED, NONE, SCROLL, SEEKBAR;

    companion object {
        internal var mode = NONE
    }
}

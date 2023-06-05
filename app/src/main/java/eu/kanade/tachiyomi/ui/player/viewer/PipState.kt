package eu.kanade.tachiyomi.ui.player.viewer

enum class PipState {
    OFF, ON, STARTED;

    companion object {
        internal var mode: PipState = OFF
    }
}

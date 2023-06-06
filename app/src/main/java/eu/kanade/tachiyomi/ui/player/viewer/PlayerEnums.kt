package eu.kanade.tachiyomi.ui.player.viewer

/**
 * Results of the set as cover feature.
 */
enum class SetAsCover {
    Success, AddToLibraryFirst, Error
}

/**
 * Player's Picture-In-Picture state handler
 */
enum class PipState {
    OFF, ON, STARTED;

    companion object {
        internal var mode: PipState = OFF
    }
}

/**
 * Player's Seek state handler
 */
enum class SeekState {
    DOUBLE_TAP, LOCKED, NONE, SCROLL, SEEKBAR;

    companion object {
        internal var mode = NONE
    }
}

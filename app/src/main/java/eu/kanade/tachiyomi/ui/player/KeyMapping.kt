package eu.kanade.tachiyomi.ui.player

import android.util.SparseArray
import android.view.KeyEvent

// Mapping between Android and mpv keycodes (special keys)

object KeyMapping {
    val map: SparseArray<String> = SparseArray()

    init {
        // cf. https://github.com/mpv-player/mpv/blob/master/input/keycodes.h
        map.put(KeyEvent.KEYCODE_SPACE, "SPACE")
        map.put(KeyEvent.KEYCODE_ENTER, "ENTER")
        map.put(KeyEvent.KEYCODE_TAB, "TAB")
        map.put(KeyEvent.KEYCODE_DEL, "BS")
        map.put(KeyEvent.KEYCODE_FORWARD_DEL, "DEL")
        map.put(KeyEvent.KEYCODE_INSERT, "INS")
        map.put(KeyEvent.KEYCODE_MOVE_HOME, "HOME")
        map.put(KeyEvent.KEYCODE_MOVE_END, "END")
        map.put(KeyEvent.KEYCODE_PAGE_UP, "PGUP")
        map.put(KeyEvent.KEYCODE_PAGE_DOWN, "PGDWN")
        map.put(KeyEvent.KEYCODE_ESCAPE, "ESC")
        map.put(KeyEvent.KEYCODE_SYSRQ, "PRINT")

        map.put(KeyEvent.KEYCODE_DPAD_RIGHT, "RIGHT")
        map.put(KeyEvent.KEYCODE_DPAD_LEFT, "LEFT")
        map.put(KeyEvent.KEYCODE_DPAD_DOWN, "DOWN")
        map.put(KeyEvent.KEYCODE_DPAD_UP, "UP")

        // not bound, let the OS handle these:
        // map.put(KEYCODE_POWER, "POWER");
        // map.put(KEYCODE_MENU, "MENU");
        // map.put(KEYCODE_VOLUME_UP, "VOLUME_UP");
        // map.put(KEYCODE_VOLUME_DOWN, "VOLUME_DOWN");
        // map.put(KEYCODE_VOLUME_MUTE, "MUTE");
        // map.put(KEYCODE_HOME, "HOMEPAGE");
        // map.put(KEYCODE_SLEEP, "SLEEP");
        // map.put(KEYCODE_ENVELOPE, "MAIL");
        // map.put(KEYCODE_SEARCH, "SEARCH");

        map.put(KeyEvent.KEYCODE_MEDIA_PLAY, "PLAYONLY")
        map.put(KeyEvent.KEYCODE_MEDIA_PAUSE, "PAUSEONLY")
        map.put(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "PLAYPAUSE")
        map.put(KeyEvent.KEYCODE_MEDIA_STOP, "STOP")
        map.put(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, "FORWARD")
        map.put(KeyEvent.KEYCODE_MEDIA_REWIND, "REWIND")
        map.put(KeyEvent.KEYCODE_MEDIA_NEXT, "NEXT")
        map.put(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "PREV")
        map.put(KeyEvent.KEYCODE_MEDIA_RECORD, "RECORD")
        map.put(KeyEvent.KEYCODE_CHANNEL_UP, "CHANNEL_UP")
        map.put(KeyEvent.KEYCODE_CHANNEL_DOWN, "CHANNEL_DOWN")
        map.put(KeyEvent.KEYCODE_ZOOM_IN, "ZOOMIN")
        map.put(KeyEvent.KEYCODE_ZOOM_OUT, "ZOOMOUT")

        map.put(KeyEvent.KEYCODE_F1, "F1")
        map.put(KeyEvent.KEYCODE_F2, "F2")
        map.put(KeyEvent.KEYCODE_F3, "F3")
        map.put(KeyEvent.KEYCODE_F4, "F4")
        map.put(KeyEvent.KEYCODE_F5, "F5")
        map.put(KeyEvent.KEYCODE_F6, "F6")
        map.put(KeyEvent.KEYCODE_F7, "F7")
        map.put(KeyEvent.KEYCODE_F8, "F8")
        map.put(KeyEvent.KEYCODE_F9, "F9")
        map.put(KeyEvent.KEYCODE_F10, "F10")
        map.put(KeyEvent.KEYCODE_F11, "F11")
        map.put(KeyEvent.KEYCODE_F12, "F12")

        map.put(KeyEvent.KEYCODE_NUMPAD_0, "KP0")
        map.put(KeyEvent.KEYCODE_NUMPAD_1, "KP1")
        map.put(KeyEvent.KEYCODE_NUMPAD_2, "KP2")
        map.put(KeyEvent.KEYCODE_NUMPAD_3, "KP3")
        map.put(KeyEvent.KEYCODE_NUMPAD_4, "KP4")
        map.put(KeyEvent.KEYCODE_NUMPAD_5, "KP5")
        map.put(KeyEvent.KEYCODE_NUMPAD_6, "KP6")
        map.put(KeyEvent.KEYCODE_NUMPAD_7, "KP7")
        map.put(KeyEvent.KEYCODE_NUMPAD_8, "KP8")
        map.put(KeyEvent.KEYCODE_NUMPAD_9, "KP9")
        map.put(KeyEvent.KEYCODE_NUMPAD_DOT, "KP_DEC")
        map.put(KeyEvent.KEYCODE_NUMPAD_ENTER, "KP_ENTER")
    }
}

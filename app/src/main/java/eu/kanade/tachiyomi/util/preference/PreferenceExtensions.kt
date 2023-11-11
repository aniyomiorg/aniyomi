package eu.kanade.tachiyomi.util.preference

import eu.kanade.core.preference.PreferenceMutableState
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.preference.Preference

operator fun <T> Preference<Set<T>>.plusAssign(item: T) {
    set(get() + item)
}

operator fun <T> Preference<Set<T>>.minusAssign(item: T) {
    set(get() - item)
}

fun Preference<Boolean>.toggle(): Boolean {
    set(!get())
    return get()
}

fun <T> Preference<T>.asState(presenterScope: CoroutineScope): PreferenceMutableState<T> {
    return PreferenceMutableState(this, presenterScope)
}

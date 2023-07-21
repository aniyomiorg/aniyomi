package eu.kanade.tachiyomi.util.preference

import android.widget.CompoundButton
import eu.kanade.core.preference.PreferenceMutableState
import kotlinx.coroutines.CoroutineScope
import tachiyomi.core.preference.Preference

/**
 * Binds a checkbox or switch view with a boolean preference.
 */
fun CompoundButton.bindToPreference(pref: Preference<Boolean>) {
    isChecked = pref.get()
    setOnCheckedChangeListener { _, isChecked -> pref.set(isChecked) }
}

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

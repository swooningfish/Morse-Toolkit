package com.morsetoolkit.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class ThemeMode(val label: String) { System("System"), Light("Light"), Dark("Dark") }

enum class KeyType(val label: String, val description: String) {
    Straight("Straight key", "One button. A short press is a dit, a long press is a dah."),
    Buttons("Dit + Dah", "Two buttons. Tap · for a dit and – for a dah."),
    Iambic("Iambic paddles", "Hold a paddle to repeat. Squeeze both to alternate."),
}

enum class LightMode(val label: String) { Off("Off"), Screen("Screen"), Torch("Torch") }

enum class CourseOrder(val label: String, val description: String) {
    Koch("Koch", "The order radio operators use: start at full speed with two letters, then add one at a time."),
    Alphabetical("A to Z", "Letters in alphabetical order, then numbers and punctuation."),
    Easiest("Easiest first", "Short, simple patterns first (E, T, I, A…), longer ones later."),
}

/** User preferences and learning progress, saved on the device. */
class AppSettings(private val store: Settings = Settings()) {
    var theme by enumPref("theme", ThemeMode.System, ThemeMode.entries)
    var keyType by enumPref("keyType", KeyType.Straight, KeyType.entries)
    var light by enumPref("light", LightMode.Off, LightMode.entries)
    var courseOrder by enumPref("courseOrder", CourseOrder.Koch, CourseOrder.entries)
    var sound by boolPref("sound", true)
    var unlockAll by boolPref("unlockAll", false)
    var showHints by boolPref("showHints", true)

    /** Speed the app plays Morse at. */
    var charWpm by intPref("charWpm", 20)

    /** Overall speed with extra gaps between letters (Farnsworth). Never above [charWpm]. */
    var effectiveWpm by intPref("effectiveWpm", 10)

    /** Speed the app expects you to key at with the straight key or paddles. */
    var tapWpm by intPref("tapWpm", 12)

    /** Pause that ends a letter when using the Dit + Dah buttons. */
    var letterPauseMs by intPref("letterPauseMs", 800)
    var toneHz by intPref("toneHz", 600)

    var customChars by stringPref("customChars", "ETAN")

    // Learning progress. `progressVersion` makes the UI recompose when progress changes.
    private var progressVersion by mutableStateOf(0)

    fun unlockedLevels(order: CourseOrder): Int {
        progressVersion
        return store.getInt("unlocked_${order.name}", 1)
    }

    fun bestScore(order: CourseOrder, levelIndex: Int): Int? {
        progressVersion
        return store.getIntOrNull("best_${order.name}_$levelIndex")
    }

    fun recordResult(order: CourseOrder, levelIndex: Int, percent: Int, passed: Boolean) {
        val best = store.getIntOrNull("best_${order.name}_$levelIndex") ?: -1
        if (percent > best) store.putInt("best_${order.name}_$levelIndex", percent)
        if (passed && unlockedLevels(order) < levelIndex + 2) store.putInt("unlocked_${order.name}", levelIndex + 2)
        progressVersion++
    }

    fun resetProgress() {
        store.keys.filter { it.startsWith("unlocked_") || it.startsWith("best_") }.forEach(store::remove)
        progressVersion++
    }

    private fun intPref(key: String, default: Int) = pref(store.getInt(key, default)) { store.putInt(key, it) }
    private fun boolPref(key: String, default: Boolean) = pref(store.getBoolean(key, default)) { store.putBoolean(key, it) }
    private fun stringPref(key: String, default: String) = pref(store.getString(key, default)) { store.putString(key, it) }
    private fun <T : Enum<T>> enumPref(key: String, default: T, values: List<T>) =
        pref(values.firstOrNull { it.name == store.getStringOrNull(key) } ?: default) { store.putString(key, it.name) }

    private fun <T> pref(initial: T, save: (T) -> Unit) = object : ReadWriteProperty<Any?, T> {
        private val state = mutableStateOf(initial)
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            state.value = value
            save(value)
        }
    }
}

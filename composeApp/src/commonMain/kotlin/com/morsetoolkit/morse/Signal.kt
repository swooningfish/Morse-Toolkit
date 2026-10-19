package com.morsetoolkit.morse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.morsetoolkit.data.AppSettings
import com.morsetoolkit.data.LightMode
import com.morsetoolkit.platform.createTone
import com.morsetoolkit.platform.createTorch

/** Turns the tone and the chosen light on and off together. */
class Signal(private val settings: AppSettings) {
    private val tone = createTone()
    private val torch = createTorch()
    private var torchLit = false

    val torchAvailable: Boolean get() = torch.isAvailable

    /** True while a dit or dah is sounding. */
    var isOn by mutableStateOf(false)
        private set

    /** True while the whole screen should flash (playback with the Screen light). */
    var screenFlash by mutableStateOf(false)
        private set

    fun on(fullScreen: Boolean) {
        isOn = true
        if (settings.sound) tone.on(settings.toneHz)
        when (settings.light) {
            LightMode.Torch -> if (torch.isAvailable) {
                torch.set(true)
                torchLit = true
            }
            LightMode.Screen -> if (fullScreen) screenFlash = true
            LightMode.Off -> Unit
        }
    }

    fun off() {
        isOn = false
        screenFlash = false
        tone.off()
        if (torchLit) {
            torch.set(false)
            torchLit = false
        }
    }
}

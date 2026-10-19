package com.morsetoolkit.morse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.morsetoolkit.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Plays Morse with sound and light. Only one thing plays at a time. */
class MorsePlayer(
    private val scope: CoroutineScope,
    private val signal: Signal,
    private val settings: AppSettings,
) {
    var isPlaying by mutableStateOf(false)
        private set

    private var job: Job? = null

    fun play(text: String, onDone: () -> Unit = {}) = playCodes(MorseCode.encode(text), onDone)

    fun playCodes(codes: List<String?>, onDone: () -> Unit = {}) {
        stop()
        val timing = Timing.of(settings.charWpm, settings.effectiveWpm.coerceAtMost(settings.charWpm))
        val unit = timing.unitMs
        val playJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                for ((i, code) in codes.withIndex()) {
                    if (code == null) continue
                    for ((j, element) in code.withIndex()) {
                        signal.on(fullScreen = true)
                        delay(if (element == '.') unit else unit * 3)
                        signal.off()
                        if (j < code.lastIndex) delay(unit)
                    }
                    if (i < codes.lastIndex) {
                        delay(if (codes[i + 1] == null) timing.wordGapMs else timing.charGapMs)
                    }
                }
                finish()
                onDone()
            } finally {
                if (job === coroutineContext[Job]) finish()
            }
        }
        job = playJob
        isPlaying = true
        playJob.start()
    }

    fun stop() {
        job?.cancel()
        finish()
    }

    private fun finish() {
        job = null
        isPlaying = false
        signal.off()
    }
}

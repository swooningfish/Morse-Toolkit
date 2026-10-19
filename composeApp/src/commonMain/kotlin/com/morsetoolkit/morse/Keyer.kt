package com.morsetoolkit.morse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.morsetoolkit.data.AppSettings
import com.morsetoolkit.data.KeyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Turns key presses into dits, dahs and letters. A letter ends after a short pause and a
 * word space is added after a longer one.
 */
class Keyer(
    private val scope: CoroutineScope,
    private val signal: Signal,
    private val settings: AppSettings,
) {
    /** Elements of the letter being keyed right now, e.g. ".-". */
    var pattern by mutableStateOf("")
        private set

    /** Finished letter codes; `null` marks a word space. */
    val codes = mutableStateListOf<String?>()

    /** Called after each finished letter. */
    var onLetter: (() -> Unit)? = null

    val text: String get() = codes.joinToString("") { it?.let(MorseCode::decode) ?: " " }

    private val unit: Long get() = 1200L / settings.tapWpm.coerceAtLeast(1)
    private var gapJob: Job? = null
    private var beepJob: Job? = null
    private var paddleJob: Job? = null

    // Straight key

    private var downMark: TimeMark? = null

    fun straightDown() {
        gapJob?.cancel()
        downMark = TimeSource.Monotonic.markNow()
        signal.on(fullScreen = false)
    }

    fun straightUp() {
        val mark = downMark ?: return
        downMark = null
        signal.off()
        val heldMs = mark.elapsedNow().inWholeMilliseconds
        add(if (heldMs < unit * 2) '.' else '-')
        scheduleGap(0)
    }

    // Dit + Dah buttons

    fun tap(element: Char) {
        gapJob?.cancel()
        val duration = if (element == '.') unit else unit * 3
        add(element)
        beepJob?.cancel()
        beepJob = scope.launch {
            signal.on(fullScreen = false)
            try {
                delay(duration)
            } finally {
                signal.off()
            }
        }
        scheduleGap(duration)
    }

    // Iambic paddles (mode A with element memory)

    private var ditHeld = false
    private var dahHeld = false
    private var ditMemory = false
    private var dahMemory = false

    fun paddle(isDit: Boolean, down: Boolean) {
        if (isDit) {
            ditHeld = down
            if (down) ditMemory = true
        } else {
            dahHeld = down
            if (down) dahMemory = true
        }
        if (down && paddleJob?.isActive != true) {
            gapJob?.cancel()
            paddleJob = scope.launch {
                var last: Char? = null
                try {
                    while (true) {
                        val next = when {
                            ditMemory && dahMemory -> if (last == '.') '-' else '.'
                            ditMemory -> '.'
                            dahMemory -> '-'
                            else -> break
                        }
                        if (next == '.') ditMemory = false else dahMemory = false
                        signal.on(fullScreen = false)
                        delay(if (next == '.') unit else unit * 3)
                        signal.off()
                        add(next)
                        last = next
                        delay(unit)
                        if (ditHeld) ditMemory = true
                        if (dahHeld) dahMemory = true
                    }
                } finally {
                    signal.off()
                }
                scheduleGap(0)
            }
        }
    }

    // Editing

    fun backspace() {
        gapJob?.cancel()
        if (pattern.isNotEmpty()) {
            pattern = ""
            return
        }
        while (codes.isNotEmpty() && codes.last() == null) codes.removeAt(codes.lastIndex)
        if (codes.isNotEmpty()) codes.removeAt(codes.lastIndex)
    }

    fun clear() {
        gapJob?.cancel()
        pattern = ""
        codes.clear()
    }

    fun stop() {
        gapJob?.cancel()
        beepJob?.cancel()
        paddleJob?.cancel()
        ditHeld = false
        dahHeld = false
        ditMemory = false
        dahMemory = false
        downMark = null
        signal.off()
    }

    private fun add(element: Char) {
        if (pattern.length < 10) pattern += element
    }

    private fun scheduleGap(afterMs: Long) {
        gapJob?.cancel()
        val (letterGap, wordGap) = if (settings.keyType == KeyType.Buttons) {
            val pause = settings.letterPauseMs.toLong()
            pause to pause * 5 / 2
        } else {
            unit * 3 to unit * 7
        }
        gapJob = scope.launch {
            delay(afterMs + letterGap)
            commitLetter()
            delay(wordGap - letterGap)
            if (codes.isNotEmpty() && codes.last() != null) codes.add(null)
        }
    }

    private fun commitLetter() {
        if (pattern.isEmpty()) return
        codes.add(pattern)
        pattern = ""
        onLetter?.invoke()
    }
}

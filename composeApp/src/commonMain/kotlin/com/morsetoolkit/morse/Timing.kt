package com.morsetoolkit.morse

/**
 * Element and gap lengths in milliseconds. When [effectiveWpm] is below [charWpm] the
 * letters keep their full speed and the gaps between them stretch (Farnsworth timing).
 */
data class Timing(val unitMs: Long, val charGapMs: Long, val wordGapMs: Long) {
    companion object {
        fun of(charWpm: Int, effectiveWpm: Int): Timing {
            val unit = 1200L / charWpm.coerceAtLeast(1)
            if (effectiveWpm >= charWpm) return Timing(unit, unit * 3, unit * 7)
            val totalDelaySeconds = (60.0 * charWpm - 37.2 * effectiveWpm) / (charWpm * effectiveWpm)
            val gapUnit = totalDelaySeconds / 19 * 1000
            return Timing(unit, (gapUnit * 3).toLong(), (gapUnit * 7).toLong())
        }
    }
}

package com.morsetoolkit.morse

/** International Morse code table plus helpers to encode text and decode keyed patterns. */
object MorseCode {
    val letters: Map<Char, String> = linkedMapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--..",
    )

    val numbers: Map<Char, String> = linkedMapOf(
        '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....",
        '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.", '0' to "-----",
    )

    val punctuation: Map<Char, String> = linkedMapOf(
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '/' to "-..-.", '=' to "-...-",
        '\'' to ".----.", '!' to "-.-.--", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...",
        ':' to "---...", ';' to "-.-.-.", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
        '"' to ".-..-.", '@' to ".--.-.",
    )

    val chars: Map<Char, String> = letters + numbers + punctuation

    /** Prosigns are sent as one run-together character. */
    val prosigns: Map<String, String> = linkedMapOf(
        "AR" to ".-.-.", "AS" to ".-...", "BK" to "-...-.-", "BT" to "-...-", "CL" to "-.-..-..",
        "CT" to "-.-.-", "HH" to "........", "KN" to "-.--.", "SK" to "...-.-", "SN" to "...-.",
        "SOS" to "...---...",
    )

    private val reverseChars: Map<String, Char> = chars.entries.associate { (c, code) -> code to c }
    private val reverseProsigns: Map<String, String> = prosigns.entries.associate { (p, code) -> code to p }

    fun codeFor(c: Char): String? = chars[c.uppercaseChar()]

    /** Decodes one letter pattern such as ".-" into display text; prosigns render as <SK>. */
    fun decode(pattern: String): String =
        reverseChars[pattern]?.toString()
            ?: reverseProsigns[pattern]?.let { "<$it>" }
            ?: "*"

    /** Letter codes for some text; `null` marks a word space. Text inside <..> is sent as a prosign. */
    fun encode(text: String): List<String?> = encodeParts(text).map { it?.code }

    /** Like [encode] but keeps the label for each letter, e.g. ("K", "-.-"). */
    fun encodeParts(text: String): List<Part?> {
        val out = mutableListOf<Part?>()
        var i = 0
        val upper = text.uppercase()
        while (i < upper.length) {
            val c = upper[i]
            if (c == '<') {
                val end = upper.indexOf('>', i)
                if (end > i) {
                    val name = upper.substring(i + 1, end)
                    prosignCode(name)?.let { out += Part(name, it) }
                    i = end + 1
                    continue
                }
            }
            if (c.isWhitespace()) {
                if (out.isNotEmpty() && out.last() != null) out += null
            } else {
                chars[c]?.let { out += Part(c.toString(), it) }
            }
            i++
        }
        while (out.isNotEmpty() && out.last() == null) out.removeAt(out.lastIndex)
        return out
    }

    data class Part(val label: String, val code: String)

    fun prosignCode(name: String): String? =
        prosigns[name] ?: name.mapNotNull { chars[it] }.takeIf { it.size == name.length }?.joinToString("")

    /** Human friendly code display: ".-" becomes "·–", word spaces become " / ". */
    fun pretty(codes: List<String?>): String =
        codes.joinToString(" ") { it?.let(::prettyCode) ?: "/" }

    fun prettyCode(code: String): String = code.replace('.', '·').replace('-', '–')
}

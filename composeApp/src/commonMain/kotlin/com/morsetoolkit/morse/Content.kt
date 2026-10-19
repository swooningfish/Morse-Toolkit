package com.morsetoolkit.morse

import kotlin.random.Random

/** One thing to practise: a letter, a Q-code, a word, a sentence... */
data class Item(
    val text: String,
    val meaning: String? = null,
    /** Prosigns are keyed as one run-together character. */
    val prosign: Boolean = false,
) {
    /** Letters with their codes; `null` marks a word space. A prosign is a single part. */
    val parts: List<MorseCode.Part?>
        get() = if (prosign) listOfNotNull(MorseCode.prosignCode(text)?.let { MorseCode.Part(text, it) }) else MorseCode.encodeParts(text)

    val codes: List<String?> get() = parts.map { it?.code }
}

enum class AnswerStyle { Choices, Typing }

enum class Category(val title: String, val subtitle: String, val answerStyle: AnswerStyle) {
    Letters("Letters", "A to Z", AnswerStyle.Choices),
    Numbers("Numbers", "0 to 9", AnswerStyle.Choices),
    Punctuation("Punctuation", ". , ? / = and more", AnswerStyle.Choices),
    Prosigns("Prosigns", "AR, SK, BT, KN…", AnswerStyle.Choices),
    QCodes("Q-codes", "QTH, QSL, QRZ…", AnswerStyle.Choices),
    Abbreviations("Abbreviations", "CQ, 73, TNX, FB…", AnswerStyle.Choices),
    Callsigns("Callsigns", "Like G4ABC or W1AW", AnswerStyle.Typing),
    Words("Words", "Common English words", AnswerStyle.Typing),
    Phrases("Phrases", "Short on-air phrases", AnswerStyle.Typing),
    Sentences("Sentences", "Full sentences and proverbs", AnswerStyle.Typing);

    /** Items for this category. Callsigns are generated fresh each time. */
    fun items(): List<Item> = when (this) {
        Letters -> MorseCode.letters.keys.map { Item(it.toString()) }
        Numbers -> MorseCode.numbers.keys.map { Item(it.toString()) }
        Punctuation -> MorseCode.punctuation.keys.map { Item(it.toString(), Content.punctuationNames[it]) }
        Prosigns -> Content.prosigns
        QCodes -> Content.qCodes
        Abbreviations -> Content.abbreviations
        Callsigns -> List(40) { Content.randomCallsign() }
        Words -> Content.words.map { Item(it) }
        Phrases -> Content.phrases.map { Item(it) }
        Sentences -> Content.sentences.map { Item(it) }
    }
}

object Content {
    val punctuationNames = mapOf(
        '.' to "Full stop", ',' to "Comma", '?' to "Question mark", '/' to "Slash", '=' to "Equals / break",
        '\'' to "Apostrophe", '!' to "Exclamation mark", '(' to "Open bracket", ')' to "Close bracket",
        '&' to "Ampersand", ':' to "Colon", ';' to "Semicolon", '+' to "Plus", '-' to "Hyphen",
        '_' to "Underscore", '"' to "Quotation mark", '@' to "At sign",
    )

    val prosigns = listOf(
        Item("AR", "End of message", prosign = true),
        Item("AS", "Wait", prosign = true),
        Item("BK", "Break, back to you", prosign = true),
        Item("BT", "Pause / new paragraph", prosign = true),
        Item("CL", "Closing down, going off air", prosign = true),
        Item("CT", "Start of transmission", prosign = true),
        Item("HH", "Error, I made a mistake", prosign = true),
        Item("KN", "Over, only the station called may reply", prosign = true),
        Item("SK", "End of contact", prosign = true),
        Item("SN", "Understood", prosign = true),
        Item("SOS", "Distress signal", prosign = true),
    )

    val qCodes = listOf(
        Item("QRG", "Your exact frequency is…"),
        Item("QRK", "Readability of your signal is…"),
        Item("QRL", "Is this frequency in use?"),
        Item("QRM", "I am getting interference"),
        Item("QRN", "I am troubled by static"),
        Item("QRO", "Increase power"),
        Item("QRP", "Decrease power / low power"),
        Item("QRQ", "Send faster"),
        Item("QRS", "Send more slowly"),
        Item("QRT", "Stop sending / closing station"),
        Item("QRU", "I have nothing for you"),
        Item("QRV", "I am ready"),
        Item("QRX", "Wait, I will call you again"),
        Item("QRZ", "Who is calling me?"),
        Item("QSB", "Your signal is fading"),
        Item("QSK", "I can hear you between my signals"),
        Item("QSL", "I confirm receipt"),
        Item("QSO", "A contact between stations"),
        Item("QSP", "I will relay a message"),
        Item("QSY", "Change frequency"),
        Item("QTH", "My location is…"),
        Item("QTR", "The correct time is…"),
    )

    val abbreviations = listOf(
        Item("73", "Best regards"),
        Item("88", "Love and kisses"),
        Item("ABT", "About"),
        Item("AGN", "Again"),
        Item("ANT", "Antenna"),
        Item("B4", "Before"),
        Item("BCNU", "Be seeing you"),
        Item("C", "Yes, correct"),
        Item("CFM", "Confirm"),
        Item("CQ", "Calling any station"),
        Item("CUL", "See you later"),
        Item("DE", "From, this is"),
        Item("DX", "Long distance"),
        Item("ES", "And"),
        Item("FB", "Fine business, excellent"),
        Item("GA", "Good afternoon / go ahead"),
        Item("GE", "Good evening"),
        Item("GM", "Good morning"),
        Item("GN", "Good night"),
        Item("HI", "Laughter"),
        Item("HR", "Here"),
        Item("HW", "How copy?"),
        Item("N", "No"),
        Item("NR", "Number"),
        Item("OM", "Old man, fellow operator"),
        Item("OP", "Operator"),
        Item("PSE", "Please"),
        Item("PWR", "Power"),
        Item("R", "Received, roger"),
        Item("RIG", "Radio equipment"),
        Item("RPT", "Repeat / report"),
        Item("RST", "Signal report"),
        Item("SRI", "Sorry"),
        Item("TNX", "Thanks"),
        Item("TU", "Thank you"),
        Item("UR", "Your / you are"),
        Item("WX", "Weather"),
        Item("XYL", "Wife"),
        Item("YL", "Young lady"),
    )

    private val callsignPrefixes = listOf(
        "G", "M", "2E", "GW", "GM", "GI", "EI", "W", "K", "N", "AA", "KD", "VE", "VK", "ZL",
        "DL", "F", "I", "EA", "PA", "ON", "OH", "SM", "LA", "JA", "ZS", "PY",
    )

    fun randomCallsign(random: Random = Random): Item {
        val prefix = callsignPrefixes.random(random)
        val digit = random.nextInt(0, 10)
        val suffix = (1..random.nextInt(1, 4)).map { ('A'..'Z').random(random) }.joinToString("")
        return Item("$prefix$digit$suffix")
    }

    val words = listOf(
        "THE", "AND", "YOU", "THAT", "WAS", "FOR", "ARE", "WITH", "HIS", "THEY", "THIS", "HAVE",
        "FROM", "ONE", "HAD", "WORD", "BUT", "NOT", "WHAT", "ALL", "WERE", "WHEN", "YOUR", "CAN",
        "SAID", "THERE", "USE", "EACH", "WHICH", "SHE", "HOW", "THEIR", "WILL", "OTHER", "ABOUT",
        "OUT", "MANY", "THEN", "THEM", "THESE", "SOME", "WOULD", "MAKE", "LIKE", "TIME", "LOOK",
        "TWO", "MORE", "WRITE", "SEE", "NUMBER", "WAY", "COULD", "PEOPLE", "THAN", "FIRST", "WATER",
        "CALL", "WHO", "NOW", "FIND", "LONG", "DOWN", "DAY", "GET", "COME", "MADE", "MAY", "PART",
        "RADIO", "SIGNAL", "NAME", "GOOD", "WEATHER", "HOME", "WORK", "SUN", "RAIN", "WIND",
    )

    val phrases = listOf(
        "CQ CQ DE G4ABC K",
        "UR RST 599",
        "NAME IS JOHN",
        "QTH LONDON",
        "TNX FER CALL",
        "GM OM",
        "HW CPY",
        "RIG IS 100 W",
        "WX IS SUNNY",
        "PSE QRS",
        "73 ES GUD DX",
        "CUL",
        "GOOD MORNING",
        "THANK YOU",
        "SEE YOU SOON",
        "HAVE A NICE DAY",
        "HOW ARE YOU",
        "ALL IS WELL",
    )

    val sentences = listOf(
        "THE QUICK BROWN FOX JUMPS OVER THE LAZY DOG",
        "A JOURNEY OF A THOUSAND MILES BEGINS WITH ONE STEP",
        "PRACTICE MAKES PERFECT",
        "WHERE THERE IS A WILL THERE IS A WAY",
        "ACTIONS SPEAK LOUDER THAN WORDS",
        "THE EARLY BIRD CATCHES THE WORM",
        "FORTUNE FAVOURS THE BOLD",
        "KNOWLEDGE IS POWER",
        "EVERY CLOUD HAS A SILVER LINING",
        "SLOW AND STEADY WINS THE RACE",
        "MY ANTENNA IS A DIPOLE",
        "THE WEATHER HERE IS COLD AND WET",
        "I HAVE BEEN A RADIO AMATEUR FOR TEN YEARS",
        "THANK YOU FOR THE NICE CONTACT",
    )
}

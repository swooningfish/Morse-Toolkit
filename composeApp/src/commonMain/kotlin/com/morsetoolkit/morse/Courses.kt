package com.morsetoolkit.morse

import com.morsetoolkit.data.CourseOrder

sealed interface Level {
    val index: Int
    val title: String
    val number: Int get() = index + 1
}

/** A level that teaches one or two new characters and reviews the ones before. */
data class CharLevel(
    override val index: Int,
    val newChars: List<Char>,
    val chars: List<Char>,
) : Level {
    override val title: String get() = newChars.joinToString(" & ")
}

/** A level after all characters are learnt, built from one category. */
data class TopicLevel(
    override val index: Int,
    override val title: String,
    val category: Category,
    val filter: (Item) -> Boolean = { true },
) : Level {
    fun items(): List<Item> = category.items().filter(filter)
}

object Courses {
    const val PASS_PERCENT = 90

    private val courseChars: List<Char> =
        MorseCode.letters.keys.toList() + MorseCode.numbers.keys + listOf('.', ',', '?', '/', '=')

    fun order(order: CourseOrder): List<Char> = when (order) {
        CourseOrder.Koch -> "KMURESNAPTLWI.JZ=FOY,VG5/Q92H38B?47C1D60X".toList()
        CourseOrder.Alphabetical -> courseChars
        CourseOrder.Easiest -> courseChars.sortedWith(
            compareBy<Char>({ MorseCode.chars.getValue(it).length }, { MorseCode.chars.getValue(it).count { e -> e == '-' } }),
        )
    }

    fun levels(order: CourseOrder): List<Level> {
        val chars = order(order)
        val levels = mutableListOf<Level>(CharLevel(0, chars.take(2), chars.take(2)))
        for (n in 3..chars.size) levels += CharLevel(levels.size, listOf(chars[n - 1]), chars.take(n))
        fun topic(title: String, category: Category, filter: (Item) -> Boolean = { true }) {
            levels += TopicLevel(levels.size, title, category, filter)
        }
        topic("Prosigns", Category.Prosigns)
        topic("Q-codes", Category.QCodes)
        topic("Abbreviations", Category.Abbreviations)
        topic("Callsigns", Category.Callsigns)
        topic("Short words", Category.Words) { it.text.length <= 4 }
        topic("Longer words", Category.Words) { it.text.length > 4 }
        topic("Phrases", Category.Phrases)
        topic("Sentences", Category.Sentences)
        return levels
    }
}

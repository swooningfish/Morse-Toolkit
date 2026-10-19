package com.morsetoolkit.morse

import kotlin.random.Random

enum class Direction(val label: String, val description: String) {
    Listen("Listen", "Hear Morse and pick or type what it says"),
    Send("Send", "See text and tap it out in Morse"),
    Both("Both", "A mix of listening and sending"),
}

/** One question in a drill. [choices] is null when the answer is typed. */
data class Question(val item: Item, val send: Boolean, val choices: List<Item>?)

/** Everything needed to run a drill and report the result. */
class DrillRequest(
    val title: String,
    /** Called for each attempt so a retry gets fresh questions. */
    val makeQuestions: () -> List<Question>,
    /** Pass mark for levels; null for free practice. */
    val passPercent: Int? = null,
    val onResult: (percent: Int, passed: Boolean) -> Unit = { _, _ -> },
    /** Builds the drill for the following level, if there is one. */
    val next: (() -> DrillRequest)? = null,
)

object Drills {
    fun build(
        pool: List<Item>,
        count: Int,
        direction: Direction,
        answerStyle: AnswerStyle,
        focus: List<Item> = emptyList(),
        random: Random = Random,
    ): List<Question> {
        if (pool.isEmpty()) return emptyList()
        var previous: Item? = null
        return List(count) { i ->
            // Pick from the new items 40% of the time, never the same item twice in a row.
            var item: Item
            do {
                item = if (focus.isNotEmpty() && random.nextInt(100) < 40) focus.random(random) else pool.random(random)
            } while (pool.size > 1 && item == previous)
            previous = item
            val send = when (direction) {
                Direction.Listen -> false
                Direction.Send -> true
                Direction.Both -> i % 2 == 1
            }
            val choices = if (answerStyle == AnswerStyle.Choices && !send) choicesFor(item, pool, random) else null
            Question(item, send, choices)
        }
    }

    private fun choicesFor(answer: Item, pool: List<Item>, random: Random): List<Item> {
        val others = pool.filter { it.text != answer.text }.distinctBy { it.text }.shuffled(random).take(3)
        return (others + answer).shuffled(random)
    }

    /** Compares a typed answer, ignoring case and extra spaces. */
    fun matches(typed: String, item: Item): Boolean = normalize(typed) == normalize(item.text)

    private fun normalize(s: String) = s.trim().uppercase().split(Regex("\\s+")).joinToString(" ")
}

package com.morsetoolkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.morsetoolkit.AppServices
import com.morsetoolkit.LocalServices
import com.morsetoolkit.data.CourseOrder
import com.morsetoolkit.morse.AnswerStyle
import com.morsetoolkit.morse.CharLevel
import com.morsetoolkit.morse.Content
import com.morsetoolkit.morse.Courses
import com.morsetoolkit.morse.Direction
import com.morsetoolkit.morse.DrillRequest
import com.morsetoolkit.morse.Drills
import com.morsetoolkit.morse.Item
import com.morsetoolkit.morse.Level
import com.morsetoolkit.morse.MorseCode
import com.morsetoolkit.morse.Question
import com.morsetoolkit.morse.TopicLevel
import com.morsetoolkit.platform.PlatformBackHandler

private sealed interface LearnPage {
    data object Levels : LearnPage
    data class Intro(val level: Level) : LearnPage
    data object Custom : LearnPage
}

@Composable
fun LearnScreen() {
    var page by remember { mutableStateOf<LearnPage>(LearnPage.Levels) }
    PlatformBackHandler(enabled = page != LearnPage.Levels) { page = LearnPage.Levels }
    when (val p = page) {
        LearnPage.Levels -> LevelList(onOpen = { page = LearnPage.Intro(it) }, onCustom = { page = LearnPage.Custom })
        is LearnPage.Intro -> LevelIntro(p.level, onBack = { page = LearnPage.Levels })
        LearnPage.Custom -> CustomPicker(onBack = { page = LearnPage.Levels })
    }
}

@Composable
private fun LevelList(onOpen: (Level) -> Unit, onCustom: () -> Unit) {
    val settings = LocalServices.current.settings
    val order = settings.courseOrder
    val levels = remember(order) { Courses.levels(order) }
    val unlocked = if (settings.unlockAll) levels.size else settings.unlockedLevels(order)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Learn")
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Learning order", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                ChoiceRow(CourseOrder.entries, order, { it.label }, { settings.courseOrder = it })
                Text(order.description, Modifier.padding(top = 8.dp, bottom = 8.dp), style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Surface(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onCustom),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Custom practice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Pick the characters you find hardest", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            items(levels, key = { it.index }) { level ->
                val locked = level.index >= unlocked
                LevelRow(level, locked, settings.bestScore(order, level.index), onClick = { onOpen(level) })
            }
        }
    }
}

@Composable
private fun LevelRow(level: Level, locked: Boolean, best: Int?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val passed = best != null && best >= Courses.PASS_PERCENT
    Surface(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(enabled = !locked, onClick = onClick),
        color = colors.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(if (locked) colors.surfaceVariant else colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (locked) {
                    Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                } else {
                    Text("${level.number}", color = colors.onPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (level is CharLevel) "New: ${level.title}" else level.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (locked) colors.onSurfaceVariant else colors.onSurface,
                )
                val detail = when (level) {
                    is CharLevel -> "${level.chars.size} characters"
                    is TopicLevel -> level.category.subtitle
                }
                Text(
                    if (best != null) "$detail · Best $best%" else detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (passed) Icon(Icons.Filled.CheckCircle, contentDescription = "Passed", tint = LocalResultColors.current.correct)
        }
    }
}

@Composable
private fun LevelIntro(level: Level, onBack: () -> Unit) {
    val services = LocalServices.current
    val player = services.player
    var direction by remember { mutableStateOf(Direction.Both) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Level ${level.number}", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            when (level) {
                is CharLevel -> {
                    SectionTitle(if (level.newChars.size > 1) "New characters" else "New character")
                    level.newChars.forEach { c ->
                        ItemRow(Item(c.toString()), onPlay = { player.play(c.toString()) })
                    }
                    SectionTitle("In this level · tap to hear")
                    CharGrid(level.chars, isSelected = { it in level.newChars }, onClick = { player.play(it.toString()) })
                }
                is TopicLevel -> {
                    SectionTitle("Examples")
                    level.items().take(6).forEach { item ->
                        ItemRow(item, onPlay = { player.playCodes(item.codes) })
                    }
                }
            }
            SectionTitle("How do you want to practise?")
            ChoiceRow(Direction.entries, direction, { it.label }, { direction = it })
            Text(direction.description, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
            Text(
                "Score ${Courses.PASS_PERCENT}% or more to unlock the next level.",
                Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = {
                val order = services.settings.courseOrder
                services.startDrill(levelRequest(services, order, Courses.levels(order), level, direction))
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Start level", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun levelRequest(services: AppServices, order: CourseOrder, levels: List<Level>, level: Level, direction: Direction): DrillRequest {
    val makeQuestions: () -> List<Question> = {
        when (level) {
            is CharLevel -> Drills.build(
                level.chars.map { Item(it.toString(), Content.punctuationNames[it]) },
                count = 20,
                direction = direction,
                answerStyle = AnswerStyle.Choices,
                focus = level.newChars.map { Item(it.toString(), Content.punctuationNames[it]) },
            )
            is TopicLevel -> Drills.build(
                level.items(),
                count = if (level.category.answerStyle == AnswerStyle.Typing) 10 else 15,
                direction = direction,
                answerStyle = level.category.answerStyle,
            )
        }
    }
    val nextLevel = levels.getOrNull(level.index + 1)
    return DrillRequest(
        title = "Level ${level.number}",
        makeQuestions = makeQuestions,
        passPercent = Courses.PASS_PERCENT,
        onResult = { percent, passed -> services.settings.recordResult(order, level.index, percent, passed) },
        next = nextLevel?.let { n -> { levelRequest(services, order, levels, n, direction) } },
    )
}

@Composable
private fun CustomPicker(onBack: () -> Unit) {
    val services = LocalServices.current
    val settings = services.settings
    val all = remember { (MorseCode.letters.keys + MorseCode.numbers.keys + MorseCode.punctuation.keys).toList() }
    val selected = settings.customChars.toSet()
    var direction by remember { mutableStateOf(Direction.Both) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Custom practice", onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text("Tap the characters you want to practise.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            CharGrid(
                all,
                isSelected = { it in selected },
                onClick = { c ->
                    val updated = if (c in selected) selected - c else selected + c
                    settings.customChars = all.filter { it in updated }.joinToString("")
                },
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { settings.customChars = MorseCode.letters.keys.joinToString("") }, Modifier.weight(1f)) { Text("All letters") }
                OutlinedButton(onClick = { settings.customChars = "" }, Modifier.weight(1f)) { Text("Clear") }
            }
            SectionTitle("How do you want to practise?")
            ChoiceRow(Direction.entries, direction, { it.label }, { direction = it })
            Text(direction.description, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = {
                val pool = settings.customChars.map { Item(it.toString(), Content.punctuationNames[it]) }
                services.startDrill(
                    DrillRequest(
                        title = "Custom",
                        makeQuestions = { Drills.build(pool, 20, direction, AnswerStyle.Choices) },
                    ),
                )
            },
            enabled = selected.size >= 2,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(if (selected.size >= 2) "Start with ${selected.size} characters" else "Pick at least 2", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** One code with its Morse, meaning and a play button. */
@Composable
fun ItemRow(item: Item, onPlay: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (item.prosign) {
                    Text(
                        "  prosign",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                item.codes.joinToString("  ") { it?.let(MorseCode::prettyCode) ?: "/" },
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            item.meaning?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        IconButton(onClick = onPlay) { Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${item.text}") }
    }
}

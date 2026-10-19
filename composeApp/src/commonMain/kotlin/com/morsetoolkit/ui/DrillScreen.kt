package com.morsetoolkit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.morsetoolkit.LocalServices
import com.morsetoolkit.morse.DrillRequest
import com.morsetoolkit.morse.Drills
import com.morsetoolkit.morse.Item
import com.morsetoolkit.morse.MorseCode
import com.morsetoolkit.morse.Question
import kotlinx.coroutines.delay

@Composable
fun DrillScreen(request: DrillRequest, onClose: () -> Unit) {
    val services = LocalServices.current
    var current by remember(request) { mutableStateOf(request) }
    var attempt by remember(current) { mutableStateOf(0) }
    val questions = remember(current, attempt) { current.makeQuestions() }
    var index by remember(questions) { mutableStateOf(0) }
    var correct by remember(questions) { mutableStateOf(0) }
    val finished = index >= questions.size

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { services.player.stop(); onClose() }) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
            Text(current.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (!finished) {
                Text("${index + 1} / ${questions.size}", Modifier.padding(end = 16.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
        LinearProgressIndicator(
            progress = { if (questions.isEmpty()) 1f else index.toFloat() / questions.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        if (finished) {
            val percent = if (questions.isEmpty()) 100 else correct * 100 / questions.size
            val pass = current.passPercent
            val passed = pass == null || percent >= pass
            LaunchedEffect(questions) { current.onResult(percent, passed) }
            ResultView(
                percent = percent,
                correct = correct,
                total = questions.size,
                passPercent = pass,
                passed = passed,
                onRetry = { attempt++ },
                onNext = current.next?.takeIf { passed }?.let { next -> { current = next() } },
                onDone = onClose,
            )
        } else {
            key(questions, index) {
                QuestionView(questions[index]) { ok ->
                    if (ok) correct++
                    index++
                }
            }
        }
    }
}

@Composable
private fun QuestionView(question: Question, onDone: (Boolean) -> Unit) {
    val services = LocalServices.current
    var result by remember { mutableStateOf<Boolean?>(null) }
    var sent by remember { mutableStateOf("") }
    val answer: (Boolean, String) -> Unit = { ok, given ->
        if (result == null) {
            services.player.stop()
            result = ok
            sent = given
        }
    }
    LaunchedEffect(result) {
        if (result == true) {
            delay(900)
            onDone(true)
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (question.send) {
            SendQuestion(question.item, result, answer)
        } else {
            ListenQuestion(question, result, answer)
        }
        result?.let { ok ->
            Feedback(question.item, ok, sent, onNext = { onDone(ok) })
        }
    }
}

@Composable
private fun ColumnScope.ListenQuestion(question: Question, result: Boolean?, answer: (Boolean, String) -> Unit) {
    val player = LocalServices.current.player
    val item = question.item
    LaunchedEffect(question) {
        delay(400)
        player.playCodes(item.codes)
    }
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text("What did you hear?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(
            onClick = { player.playCodes(item.codes) },
            modifier = Modifier.size(104.dp),
            shape = CircleShape,
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play again", Modifier.size(48.dp))
        }
        Text("Play again", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(28.dp))

        val choices = question.choices
        if (choices != null) {
            var picked by remember { mutableStateOf<String?>(null) }
            val results = LocalResultColors.current
            choices.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { choice ->
                        val border = when {
                            result == null -> MaterialTheme.colorScheme.outline
                            choice.text == item.text -> results.correct
                            choice.text == picked -> results.wrong
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        }
                        OutlinedButton(
                            onClick = {
                                picked = choice.text
                                answer(choice.text == item.text, choice.text)
                            },
                            enabled = result == null,
                            modifier = Modifier.weight(1f).height(76.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(if (result != null && border != MaterialTheme.colorScheme.outline) 3.dp else 1.dp, border),
                            colors = ButtonDefaults.outlinedButtonColors(disabledContentColor = MaterialTheme.colorScheme.onSurface),
                        ) {
                            Text(choice.text, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else if (result == null) {
            var typed by remember { mutableStateOf("") }
            val check = { if (typed.isNotBlank()) answer(Drills.matches(typed, item), typed.trim().uppercase()) }
            OutlinedTextField(
                value = typed,
                onValueChange = { typed = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Type what you heard") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { check() }),
                textStyle = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { answer(false, "") }, Modifier.weight(1f).height(52.dp)) { Text("I don't know") }
                Button(onClick = check, Modifier.weight(1f).height(52.dp), enabled = typed.isNotBlank()) { Text("Check") }
            }
        }
    }
}

@Composable
private fun ColumnScope.SendQuestion(item: Item, result: Boolean?, answer: (Boolean, String) -> Unit) {
    val services = LocalServices.current
    val player = services.player
    val keyer = rememberKeyer()
    val target = item.parts.filterNotNull()
    val userCodes = keyer.codes.filterNotNull()

    SideEffect {
        keyer.onLetter = {
            val sentCodes = keyer.codes.filterNotNull()
            if (sentCodes.size >= target.size) {
                val ok = sentCodes.take(target.size) == target.map { it.code }
                answer(ok, keyer.text.trim())
                keyer.stop()
            }
        }
    }

    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Send this", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        TargetText(item, userCodes)
        item.meaning?.let {
            Text(it, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        if (result == null) {
            val next = target.getOrNull(userCodes.size)
            var reveal by remember { mutableStateOf(false) }
            if (next != null) {
                if (services.settings.showHints || reveal) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${next.label}  ", style = MaterialTheme.typography.titleLarge)
                        MorseText(next.code, large = true)
                    }
                } else {
                    TextButton(onClick = { reveal = true }) { Text("Show hint") }
                }
            }
        }
    }
    if (result == null) {
        CurrentLetter(keyer)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ActionButton(Icons.Filled.PlayArrow, "Hear it", onClick = { player.playCodes(item.codes) })
            ActionButton(Icons.Filled.Clear, "Delete", onClick = keyer::backspace)
            ActionButton(Icons.AutoMirrored.Filled.ArrowForward, "Skip", onClick = { keyer.stop(); answer(false, keyer.text.trim()) })
        }
        Spacer(Modifier.height(8.dp))
        KeyPad(keyer)
    }
}

/** The text to send, with letters turning green or red as they are keyed. */
@Composable
private fun TargetText(item: Item, userCodes: List<String>) {
    val results = LocalResultColors.current
    val normal = MaterialTheme.colorScheme.onSurface
    val currentColor = MaterialTheme.colorScheme.primary
    val text = buildAnnotatedString {
        var i = 0
        item.parts.forEach { part ->
            if (part == null) {
                append(" ")
                return@forEach
            }
            val style = when {
                i < userCodes.size -> SpanStyle(color = if (userCodes[i] == part.code) results.correct else results.wrong)
                i == userCodes.size -> SpanStyle(color = currentColor, textDecoration = TextDecoration.Underline)
                else -> SpanStyle(color = normal)
            }
            withStyle(style) { append(part.label) }
            i++
        }
    }
    val long = item.text.length > 12
    Text(
        text,
        style = if (long) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Feedback(item: Item, ok: Boolean, given: String, onNext: () -> Unit) {
    val results = LocalResultColors.current
    val color = if (ok) results.correct else results.wrong
    Surface(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(2.dp, color),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(if (ok) "Correct!" else "Not quite", color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!ok) {
                if (given.isNotBlank()) Text("You: $given", style = MaterialTheme.typography.bodyLarge)
                Text("Answer: ${item.text}", style = MaterialTheme.typography.titleMedium)
                Text(
                    MorseCode.pretty(item.codes),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            item.meaning?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (!ok) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onNext, Modifier.fillMaxWidth().height(52.dp)) { Text("Next") }
            }
        }
    }
}

@Composable
private fun ResultView(
    percent: Int,
    correct: Int,
    total: Int,
    passPercent: Int?,
    passed: Boolean,
    onRetry: () -> Unit,
    onNext: (() -> Unit)?,
    onDone: () -> Unit,
) {
    val results = LocalResultColors.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "$percent%",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = if (passed) results.correct else results.wrong,
        )
        Text("$correct of $total correct", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        val message = when {
            passPercent == null -> "Nice work. Keep practising!"
            passed -> "Level passed!"
            else -> "You need $passPercent% to pass. Have another go."
        }
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Color.Unspecified)
        Spacer(Modifier.height(32.dp))
        if (onNext != null) {
            Button(onClick = onNext, Modifier.fillMaxWidth().height(56.dp)) { Text("Next level", style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(onClick = onRetry, Modifier.fillMaxWidth().height(52.dp)) { Text("Try again") }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onDone, Modifier.fillMaxWidth().height(52.dp)) { Text("Done") }
    }
}

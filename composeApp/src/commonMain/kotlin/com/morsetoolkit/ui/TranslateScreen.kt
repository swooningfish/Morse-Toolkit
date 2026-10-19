package com.morsetoolkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.morsetoolkit.LocalServices
import com.morsetoolkit.data.KeyType
import com.morsetoolkit.data.LightMode
import com.morsetoolkit.morse.Keyer
import com.morsetoolkit.morse.MorseCode

private enum class TranslateMode(val label: String) { Tap("Tap → Text"), Type("Text → Morse") }

@Composable
fun TranslateScreen() {
    var mode by remember { mutableStateOf(TranslateMode.Tap) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Morse key")
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            ChoiceRow(TranslateMode.entries, mode, { it.label }, { mode = it })
            Spacer(Modifier.height(12.dp))
            when (mode) {
                TranslateMode.Tap -> TapPanel()
                TranslateMode.Type -> TypePanel()
            }
        }
    }
}

@Composable
private fun ColumnScope.TapPanel() {
    val services = LocalServices.current
    val player = services.player
    val keyer = rememberKeyer()
    val clipboard = LocalClipboardManager.current
    val empty = keyer.codes.isEmpty() && keyer.pattern.isEmpty()

    Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (empty) {
                    Text(
                        "Tap the key below.\nLetters appear here as you send.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                } else {
                    Text(keyer.text, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        MorseCode.pretty(keyer.codes),
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            CurrentLetter(keyer)
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        ActionButton(Icons.Filled.PlayArrow, if (player.isPlaying) "Stop" else "Play", enabled = keyer.codes.isNotEmpty(), onClick = {
            if (player.isPlaying) player.stop() else player.playCodes(keyer.codes.toList())
        })
        ActionButton(Icons.Filled.Clear, "Delete", enabled = !empty, onClick = keyer::backspace)
        ActionButton(Icons.Filled.Delete, "Clear", enabled = !empty, onClick = keyer::clear)
        ActionButton(Icons.Filled.Share, "Copy", enabled = keyer.codes.isNotEmpty(), onClick = {
            clipboard.setText(AnnotatedString(keyer.text.trim()))
        })
    }
    KeyTypeSelector(keyer)
    Spacer(Modifier.height(10.dp))
    KeyPad(keyer)
    Spacer(Modifier.height(12.dp))
}

/** Shows the dits and dahs of the letter being keyed and what it will become. */
@Composable
fun CurrentLetter(keyer: Keyer, modifier: Modifier = Modifier) {
    val signal = LocalServices.current.signal
    Row(modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(14.dp).clip(CircleShape)
                .background(if (signal.isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        if (keyer.pattern.isNotEmpty()) {
            MorseText(keyer.pattern, large = true)
            Spacer(Modifier.width(12.dp))
            Text("→ ${MorseCode.decode(keyer.pattern)}", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

/** Quick switch between the three key styles. */
@Composable
fun KeyTypeSelector(keyer: Keyer) {
    val settings = LocalServices.current.settings
    ChoiceRow(
        KeyType.entries,
        settings.keyType,
        label = {
            when (it) {
                KeyType.Straight -> "Straight"
                KeyType.Buttons -> "Dit + Dah"
                KeyType.Iambic -> "Paddles"
            }
        },
        onSelect = {
            keyer.stop()
            settings.keyType = it
        },
    )
}

@Composable
private fun ColumnScope.TypePanel() {
    val services = LocalServices.current
    val settings = services.settings
    val player = services.player
    var text by rememberSaveable { mutableStateOf("") }
    val codes = MorseCode.encode(text)

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Type a message") },
        minLines = 2,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
    )
    Spacer(Modifier.height(12.dp))
    Surface(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
            Text(
                if (codes.isEmpty()) "The Morse code appears here." else MorseCode.pretty(codes),
                color = if (codes.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                fontFamily = if (codes.isEmpty()) null else FontFamily.Monospace,
                style = if (codes.isEmpty()) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    SoundAndLight()
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { if (player.isPlaying) player.stop() else player.playCodes(codes) },
        enabled = codes.isNotEmpty() || player.isPlaying,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(if (player.isPlaying) "Stop" else "Send", style = MaterialTheme.typography.titleMedium)
    }
    Spacer(Modifier.height(12.dp))
}

/** Sound switch and light choice, used wherever Morse is sent. */
@Composable
fun SoundAndLight() {
    val services = LocalServices.current
    val settings = services.settings
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Sound", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(8.dp))
        Switch(checked = settings.sound, onCheckedChange = { settings.sound = it })
        Spacer(Modifier.width(16.dp))
        Text("Light", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(8.dp))
        val lights = if (services.signal.torchAvailable) LightMode.entries else listOf(LightMode.Off, LightMode.Screen)
        ChoiceRow(lights, settings.light, { it.label }, { settings.light = it }, Modifier.weight(1f))
    }
}

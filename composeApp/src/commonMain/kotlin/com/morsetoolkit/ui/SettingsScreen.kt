package com.morsetoolkit.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morsetoolkit.LocalServices
import com.morsetoolkit.data.KeyType
import com.morsetoolkit.data.LightMode
import com.morsetoolkit.data.ThemeMode
import kotlin.math.roundToInt

@Composable
fun SettingsScreen() {
    val services = LocalServices.current
    val settings = services.settings
    var confirmReset by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settings")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("Appearance")
            ChoiceRow(ThemeMode.entries, settings.theme, { it.label }, { settings.theme = it })

            SectionTitle("Morse key")
            ChoiceRow(KeyType.entries, settings.keyType, {
                when (it) {
                    KeyType.Straight -> "Straight"
                    KeyType.Buttons -> "Dit + Dah"
                    KeyType.Iambic -> "Paddles"
                }
            }, { settings.keyType = it })
            Text(settings.keyType.description, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
            if (settings.keyType == KeyType.Buttons) {
                SliderSetting(
                    "Pause that ends a letter",
                    "${settings.letterPauseMs} ms",
                    settings.letterPauseMs, 300..2000, step = 100,
                ) { settings.letterPauseMs = it }
            } else {
                SliderSetting(
                    "Your keying speed",
                    "${settings.tapWpm} WPM",
                    settings.tapWpm, 5..35,
                ) { settings.tapWpm = it }
                Text(
                    "Lower this if your dahs are read as dits or letters run together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle("Playback speed")
            SliderSetting("Letter speed", "${settings.charWpm} WPM", settings.charWpm, 5..40) {
                settings.charWpm = it
                if (settings.effectiveWpm > it) settings.effectiveWpm = it
            }
            SliderSetting("Overall speed (extra gaps)", "${settings.effectiveWpm} WPM", settings.effectiveWpm, 3..settings.charWpm.coerceAtLeast(4)) {
                settings.effectiveWpm = it
            }
            Text(
                "Letters play at full speed with longer gaps between them. This is the easiest way to learn by ear.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("Sound and light")
            SwitchSetting("Sound", settings.sound) { settings.sound = it }
            SliderSetting("Tone pitch", "${settings.toneHz} Hz", settings.toneHz, 400..1000, step = 50) { settings.toneHz = it }
            Text("Light", Modifier.padding(top = 8.dp, bottom = 8.dp), style = MaterialTheme.typography.bodyLarge)
            val lights = if (services.signal.torchAvailable) LightMode.entries else listOf(LightMode.Off, LightMode.Screen)
            ChoiceRow(lights, settings.light, { it.label }, { settings.light = it })
            Text(
                when (settings.light) {
                    LightMode.Off -> "Morse plays as sound only."
                    LightMode.Screen -> "The screen flashes white while Morse plays."
                    LightMode.Torch -> "The camera flashlight flashes with each dit and dah."
                },
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { services.player.play("SOS") }, Modifier.padding(top = 12.dp)) { Text("Test: play SOS") }

            SectionTitle("Learning")
            SwitchSetting("Show code hints when sending", settings.showHints) { settings.showHints = it }
            SwitchSetting("Unlock all levels", settings.unlockAll) { settings.unlockAll = it }
            OutlinedButton(onClick = { confirmReset = true }, Modifier.padding(top = 8.dp)) { Text("Reset progress") }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset progress?") },
            text = { Text("This locks all levels again and clears your best scores.") },
            confirmButton = {
                TextButton(onClick = {
                    settings.resetProgress()
                    confirmReset = false
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SliderSetting(title: String, value: String, current: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        val stepsCount = ((range.last - range.first) / step - 1).coerceAtLeast(0)
        Slider(
            value = current.toFloat(),
            onValueChange = { v -> onChange(((v - range.first) / step).roundToInt() * step + range.first) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = stepsCount,
        )
    }
}

@Composable
private fun SwitchSetting(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

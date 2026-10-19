package com.morsetoolkit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.morsetoolkit.LocalServices
import com.morsetoolkit.data.KeyType
import com.morsetoolkit.morse.Keyer

@Composable
fun rememberKeyer(): Keyer {
    val services = LocalServices.current
    val scope = rememberCoroutineScope()
    val keyer = remember { Keyer(scope, services.signal, services.settings) }
    DisposableEffect(keyer) { onDispose { keyer.stop() } }
    return keyer
}

/** The Morse key in whichever style the user picked in Settings. */
@Composable
fun KeyPad(keyer: Keyer, modifier: Modifier = Modifier, height: Dp = 140.dp) {
    val services = LocalServices.current
    val player = services.player
    val keyType = services.settings.keyType
    Column(modifier.fillMaxWidth()) {
        when (keyType) {
            KeyType.Straight -> KeyButton(
                label = "Tap · or hold –",
                description = "Morse key. Short press for dit, long press for dah",
                modifier = Modifier.fillMaxWidth().height(height),
                onDown = { player.stop(); keyer.straightDown() },
                onUp = { keyer.straightUp() },
            )
            KeyType.Buttons -> Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KeyButton("·", "Dit", Modifier.weight(1f).fillMaxHeight(), big = true, onDown = { player.stop(); keyer.tap('.') })
                KeyButton("–", "Dah", Modifier.weight(1f).fillMaxHeight(), big = true, onDown = { player.stop(); keyer.tap('-') })
            }
            KeyType.Iambic -> Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KeyButton(
                    "·", "Dit paddle", Modifier.weight(1f).fillMaxHeight(), big = true,
                    onDown = { player.stop(); keyer.paddle(isDit = true, down = true) },
                    onUp = { keyer.paddle(isDit = true, down = false) },
                )
                KeyButton(
                    "–", "Dah paddle", Modifier.weight(1f).fillMaxHeight(), big = true,
                    onDown = { player.stop(); keyer.paddle(isDit = false, down = true) },
                    onUp = { keyer.paddle(isDit = false, down = false) },
                )
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    description: String,
    modifier: Modifier,
    big: Boolean = false,
    onDown: () -> Unit,
    onUp: () -> Unit = {},
) {
    val down by rememberUpdatedState(onDown)
    val up by rememberUpdatedState(onUp)
    var pressed by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (pressed) colors.primary else colors.primaryContainer)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    down()
                    waitForUpOrCancellation()
                    pressed = false
                    up()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (pressed) colors.onPrimary else colors.onPrimaryContainer,
            style = if (big) MaterialTheme.typography.displayLarge else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

package com.morsetoolkit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.morsetoolkit.data.AppSettings
import com.morsetoolkit.data.LightMode
import com.morsetoolkit.data.ThemeMode
import com.morsetoolkit.morse.DrillRequest
import com.morsetoolkit.morse.MorsePlayer
import com.morsetoolkit.morse.Signal
import com.morsetoolkit.platform.PlatformBackHandler
import com.morsetoolkit.ui.CodesScreen
import com.morsetoolkit.ui.DrillScreen
import com.morsetoolkit.ui.LearnScreen
import com.morsetoolkit.ui.MorseTheme
import com.morsetoolkit.ui.PracticeScreen
import com.morsetoolkit.ui.SettingsScreen
import com.morsetoolkit.ui.TranslateScreen
import kotlinx.coroutines.CoroutineScope

/** Shared objects every screen can reach through [LocalServices]. */
class AppServices(scope: CoroutineScope) {
    val settings = AppSettings()
    val signal = Signal(settings)
    val player = MorsePlayer(scope, signal, settings)

    /** The drill being shown full screen, if any. */
    var drill by mutableStateOf<DrillRequest?>(null)

    fun startDrill(request: DrillRequest) {
        player.stop()
        drill = request
    }
}

val LocalServices = staticCompositionLocalOf<AppServices> { error("AppServices not provided") }

private enum class Tab(val label: String, val icon: ImageVector) {
    Translate("Key", Icons.Filled.Edit),
    Learn("Learn", Icons.Filled.Star),
    Practice("Practice", Icons.Filled.PlayArrow),
    Codes("Codes", Icons.AutoMirrored.Filled.List),
    Settings("Settings", Icons.Filled.Settings),
}

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val services = remember { AppServices(scope) }
    val dark = when (services.settings.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MorseTheme(dark) {
        CompositionLocalProvider(LocalServices provides services) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                val drill = services.drill
                if (drill != null) {
                    PlatformBackHandler { services.drill = null }
                    Box(Modifier.safeDrawingPadding()) {
                        DrillScreen(drill, onClose = { services.drill = null })
                    }
                } else {
                    MainTabs()
                }
                LightOverlay()
            }
        }
    }
}

@Composable
private fun MainTabs() {
    var tab by remember { mutableStateOf(Tab.Translate) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            when (tab) {
                Tab.Translate -> TranslateScreen()
                Tab.Learn -> LearnScreen()
                Tab.Practice -> PracticeScreen()
                Tab.Codes -> CodesScreen()
                Tab.Settings -> SettingsScreen()
            }
        }
    }
}

/** With the Screen light on, the whole screen becomes a signal lamp while Morse plays. */
@Composable
private fun LightOverlay() {
    val services = LocalServices.current
    if (services.settings.light != LightMode.Screen || !services.player.isPlaying) return
    Box(
        Modifier.fillMaxSize()
            .background(if (services.signal.screenFlash) Color.White else Color.Black)
            .clickable { services.player.stop() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            "Tap anywhere to stop",
            Modifier.padding(48.dp),
            color = Color.Gray,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

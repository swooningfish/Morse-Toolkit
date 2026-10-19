package com.morsetoolkit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.morsetoolkit.LocalServices
import com.morsetoolkit.morse.Category
import com.morsetoolkit.morse.Direction
import com.morsetoolkit.morse.DrillRequest
import com.morsetoolkit.morse.Drills
import com.morsetoolkit.platform.PlatformBackHandler

@Composable
fun PracticeScreen() {
    var category by remember { mutableStateOf<Category?>(null) }
    PlatformBackHandler(enabled = category != null) { category = null }
    val selected = category
    if (selected == null) {
        CategoryList(onPick = { category = it })
    } else {
        PracticeSetup(selected, onBack = { category = null })
    }
}

@Composable
private fun CategoryList(onPick: (Category) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Practice")
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    "Choose what to practise.",
                    Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(Category.entries) { category ->
                Surface(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onPick(category) },
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(category.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeSetup(category: Category, onBack: () -> Unit) {
    val services = LocalServices.current
    var direction by remember { mutableStateOf(Direction.Both) }
    var count by remember { mutableStateOf(10) }
    val examples = remember(category) { category.items().take(5) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(category.title, onBack = onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("How do you want to practise?")
            ChoiceRow(Direction.entries, direction, { it.label }, { direction = it })
            Text(direction.description, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
            SectionTitle("How many questions?")
            ChoiceRow(listOf(10, 20, 50), count, { it.toString() }, { count = it })
            SectionTitle("Examples")
            examples.forEach { item -> ItemRow(item, onPlay = { services.player.playCodes(item.codes) }) }
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = {
                services.startDrill(
                    DrillRequest(
                        title = category.title,
                        makeQuestions = { Drills.build(category.items(), count, direction, category.answerStyle) },
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("Start", style = MaterialTheme.typography.titleMedium)
        }
    }
}

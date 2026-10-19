package com.morsetoolkit.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morsetoolkit.LocalServices
import com.morsetoolkit.morse.Category

/** Look-up list of every code with its meaning. */
@Composable
fun CodesScreen() {
    val player = LocalServices.current.player
    var category by remember { mutableStateOf(Category.Letters) }
    var refresh by remember { mutableStateOf(0) }
    val items = remember(category, refresh) { category.items() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Codes") {
            if (category == Category.Callsigns) {
                IconButton(onClick = { refresh++ }) { Icon(Icons.Filled.Refresh, contentDescription = "New callsigns") }
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Category.entries.forEach { c ->
                FilterChip(selected = c == category, onClick = { category = c }, label = { Text(c.title) })
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 8.dp, bottom = 24.dp)) {
            if (category == Category.Callsigns) {
                item {
                    Text(
                        "A callsign is a prefix showing the country, a number, then a few letters. Here are some made-up examples.",
                        Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(items) { item ->
                ItemRow(item, onPlay = { player.playCodes(item.codes) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    }
}

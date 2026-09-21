package com.lughlammas.mime.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lughlammas.mime.data.MapIndexEntry

@Composable
fun LibraryScreen(
    maps: List<MapIndexEntry>,
    error: String?,
    loading: Boolean,
    onStart: (path: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text("MIME", style = MaterialTheme.typography.headlineLarge)
        Text(
            "copy the line until the line is yours",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(20.dp))
        Text("Library", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Show → Mime. Wrong move resets to ply 0. No hints.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(12.dp))

        if (error != null) {
            Text(
                "Failed to load library: $error",
                color = MaterialTheme.colorScheme.error,
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(maps, key = { it.id }) { entry ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(entry.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text(
                                    entry.sideToLearn.name.lowercase(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            Text(
                                "${entry.length} plies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onStart(entry.path) },
                            enabled = !loading,
                        ) {
                            Text(if (loading) "Loading…" else "Start Mime")
                        }
                    }
                }
            }
        }
    }
}

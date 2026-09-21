package com.lughlammas.mime.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lughlammas.mime.data.Line
import com.lughlammas.mime.data.MimeSnapshot
import com.lughlammas.mime.data.Phase

@Composable
fun PlayScreen(
    line: Line,
    snapshot: MimeSnapshot?,
    onLibrary: () -> Unit,
    onRetry: () -> Unit,
    onUserMove: (from: String, to: String, promotion: String?) -> Unit,
) {
    val snap = snapshot
    val phase = snap?.phase ?: Phase.SHOW
    val flash = snap?.flash == true
    val interactive = isInteractive(phase)
    val fen = snap?.fen ?: line.startFen
    val dests = if (interactive) legalDestsFromFen(fen) else emptyMap()

    val flashBg by animateColorAsState(
        targetValue = if (flash) Color(0x33FF2222) else Color.Transparent,
        label = "flash",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(flashBg)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onLibrary) { Text("← Library") }
            Column(horizontalAlignment = Alignment.End) {
                Text(line.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    line.epithet.ifBlank { line.id },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                phase.name,
                style = MaterialTheme.typography.labelLarge,
                color = when (phase) {
                    Phase.MIME -> MaterialTheme.colorScheme.primary
                    Phase.FAIL -> MaterialTheme.colorScheme.error
                    Phase.COMPLETE -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                "${minOf(snap?.cursorPly ?: 0, line.length)} / ${line.length}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                "mistakes ${snap?.mistakes ?: 0}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }

        Spacer(Modifier.height(12.dp))

        ChessBoard(
            fen = fen,
            orientation = line.sideToLearn,
            lastMove = snap?.lastMove,
            interactive = interactive,
            legalDests = dests,
            flash = flash,
            onUserMove = onUserMove,
            modifier = Modifier.fillMaxWidth(),
        )

        if (phase == Phase.COMPLETE) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Line complete", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onRetry) { Text("Retry") }
                        OutlinedButton(onClick = onLibrary) { Text("Library") }
                    }
                }
            }
        }
    }
}

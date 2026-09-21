package com.lughlammas.mime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.lughlammas.mime.data.Side

/**
 * Dumb Compose Canvas board: renders FEN, returns click square names (a1–h8).
 * No chessground / WebView.
 */
@Composable
fun ChessBoard(
    fen: String,
    orientation: Side,
    lastMove: Pair<String, String>?,
    interactive: Boolean,
    legalDests: Map<String, List<String>>,
    flash: Boolean,
    onUserMove: (from: String, to: String, promotion: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by remember(fen, interactive) { mutableStateOf<String?>(null) }
    val destsForSelected = selected?.let { legalDests[it].orEmpty() }.orEmpty()

    val light = Color(0xFFEEEED2)
    val dark = Color(0xFF769656)
    val lastLight = Color(0xFFCDD26A)
    val lastDark = Color(0xFFAAA23A)
    val selectColor = Color(0xFFBACA2B)
    val destDot = Color(0x66000000)
    val flashOverlay = Color(0x66FF2222)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(fen, interactive, orientation, selected, legalDests) {
                detectTapGestures { offset ->
                    if (!interactive) return@detectTapGestures
                    val sq = offsetToSquare(offset.x, offset.y, size.width.toFloat(), orientation)
                        ?: return@detectTapGestures
                    val sel = selected
                    if (sel == null) {
                        if (legalDests.containsKey(sq)) {
                            selected = sq
                        }
                    } else if (sel == sq) {
                        selected = null
                    } else if (destsForSelected.contains(sq) || legalDests[sel].orEmpty().contains(sq)) {
                        val promo = defaultPromotion(fen, sel, sq)
                        onUserMove(sel, sq, promo)
                        selected = null
                    } else if (legalDests.containsKey(sq)) {
                        selected = sq
                    } else {
                        selected = null
                    }
                }
            },
    ) {
        val side = size.minDimension
        val sq = side / 8f

        for (rank in 0 until 8) {
            for (file in 0 until 8) {
                val squareName = boardSquare(file, rank, orientation)
                val isLight = (file + rank) % 2 == 0
                val isLast = lastMove != null &&
                    (squareName == lastMove.first || squareName == lastMove.second)
                val color = when {
                    selected == squareName -> selectColor
                    isLast && isLight -> lastLight
                    isLast && !isLight -> lastDark
                    isLight -> light
                    else -> dark
                }
                drawRect(
                    color = color,
                    topLeft = Offset(file * sq, rank * sq),
                    size = Size(sq, sq),
                )
            }
        }

        // Destination dots
        if (interactive && selected != null) {
            for (dest in legalDests[selected].orEmpty()) {
                val (f, r) = squareToFileRank(dest, orientation) ?: continue
                drawCircle(
                    color = destDot,
                    radius = sq * 0.16f,
                    center = Offset(f * sq + sq / 2f, r * sq + sq / 2f),
                )
            }
        }

        // Pieces from FEN
        val placement = fen.substringBefore(' ')
        drawPieces(placement, orientation, sq)

        if (flash) {
            drawRect(color = flashOverlay, size = Size(side, side))
        }
    }
}

private fun defaultPromotion(fen: String, from: String, to: String): String? {
    val placement = fen.substringBefore(' ')
    val board = Array(8) { Array<Char?>(8) { null } }
    var r = 0
    var f = 0
    for (c in placement) {
        when {
            c == '/' -> { r++; f = 0 }
            c.isDigit() -> f += c.digitToInt()
            else -> {
                if (r in 0..7 && f in 0..7) board[r][f] = c
                f++
            }
        }
    }
    val fromFile = from[0] - 'a'
    val fromRank = 8 - (from[1] - '0') // 0 = rank 8
    if (fromFile !in 0..7 || fromRank !in 0..7) return null
    val piece = board[fromRank][fromFile] ?: return null
    val toRankChar = to[1]
    return when {
        piece == 'P' && toRankChar == '8' -> "q"
        piece == 'p' && toRankChar == '1' -> "q"
        else -> null
    }
}

private fun DrawScope.drawPieces(placement: String, orientation: Side, sq: Float) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = sq * 0.72f
    }
    var rank = 0
    var file = 0
    for (c in placement) {
        when {
            c == '/' -> {
                rank++
                file = 0
            }
            c.isDigit() -> file += c.digitToInt()
            else -> {
                val glyph = pieceGlyph(c)
                val (df, dr) = fenToDraw(file, rank, orientation)
                val x = df * sq + sq / 2f
                val y = dr * sq + sq / 2f - (paint.descent() + paint.ascent()) / 2f
                // Soft shadow for contrast
                paint.color = android.graphics.Color.argb(60, 0, 0, 0)
                drawContext.canvas.nativeCanvas.drawText(glyph, x + 1.5f, y + 1.5f, paint)
                paint.color = if (c.isUpperCase()) {
                    android.graphics.Color.WHITE
                } else {
                    android.graphics.Color.rgb(20, 20, 20)
                }
                // Outline for white pieces
                if (c.isUpperCase()) {
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = sq * 0.03f
                    paint.color = android.graphics.Color.rgb(40, 40, 40)
                    drawContext.canvas.nativeCanvas.drawText(glyph, x, y, paint)
                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.WHITE
                }
                drawContext.canvas.nativeCanvas.drawText(glyph, x, y, paint)
                file++
            }
        }
    }
}

private fun pieceGlyph(c: Char): String = when (c) {
    'K' -> "♔"
    'Q' -> "♕"
    'R' -> "♖"
    'B' -> "♗"
    'N' -> "♘"
    'P' -> "♙"
    'k' -> "♚"
    'q' -> "♛"
    'r' -> "♜"
    'b' -> "♝"
    'n' -> "♞"
    'p' -> "♟"
    else -> c.toString()
}

/** FEN file/rank (0,0 = a8) → draw file/rank from top-left given orientation. */
private fun fenToDraw(file: Int, rank: Int, orientation: Side): Pair<Int, Int> {
    return if (orientation == Side.white) {
        file to rank
    } else {
        (7 - file) to (7 - rank)
    }
}

private fun boardSquare(drawFile: Int, drawRank: Int, orientation: Side): String {
    val (file, rankFromTop) = if (orientation == Side.white) {
        drawFile to drawRank
    } else {
        (7 - drawFile) to (7 - drawRank)
    }
    val fileChar = ('a' + file)
    val rankChar = ('8' - rankFromTop)
    return "$fileChar$rankChar"
}

private fun squareToFileRank(square: String, orientation: Side): Pair<Int, Int>? {
    if (square.length < 2) return null
    val file = square[0] - 'a'
    val rankFromTop = '8' - square[1]
    if (file !in 0..7 || rankFromTop !in 0..7) return null
    return if (orientation == Side.white) {
        file to rankFromTop
    } else {
        (7 - file) to (7 - rankFromTop)
    }
}

private fun offsetToSquare(x: Float, y: Float, width: Float, orientation: Side): String? {
    val sq = width / 8f
    if (sq <= 0f) return null
    val file = (x / sq).toInt().coerceIn(0, 7)
    val rank = (y / sq).toInt().coerceIn(0, 7)
    return boardSquare(file, rank, orientation)
}

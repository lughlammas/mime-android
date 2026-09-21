package com.lughlammas.mime.mime

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.Piece
import com.github.bhlangonijr.chesslib.Side as LibSide
import com.github.bhlangonijr.chesslib.Square
import com.github.bhlangonijr.chesslib.move.Move
import com.lughlammas.mime.data.Line
import com.lughlammas.mime.data.MimeSnapshot
import com.lughlammas.mime.data.Phase
import com.lughlammas.mime.data.Side
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MIME session state machine — parity with web src/mime/loop.ts.
 * SHOW → (opponent auto | learner demo+undo) → MIME → correct advances / wrong FAIL→ply0
 */
class MimeLoop(
    val line: Line,
    private val scope: CoroutineScope,
    private val onSnapshot: (MimeSnapshot) -> Unit,
) {
    companion object {
        const val SHOW_PAUSE_MS = 700L
        const val DEMO_HOLD_MS = 900L
        const val FAIL_FLASH_MS = 550L
    }

    private var board = Board()
    private var phase: Phase = Phase.SHOW
    private var cursorPly = 0
    private var mistakes = 0
    private var lastMove: Pair<String, String>? = null
    private var flash = false
    private var stopped = false
    private var timerJob: Job? = null

    val side: Side get() = line.sideToLearn

    init {
        board.loadFromFen(line.startFen)
    }

    fun start() {
        resetBoard(0)
        phase = Phase.SHOW
        emit()
        runShow()
    }

    fun stop() {
        stopped = true
        clearTimer()
    }

    /** User attempt during MIME. Returns true if accepted. */
    fun tryMove(from: String, to: String, promotion: String? = null): Boolean {
        if (phase != Phase.MIME || stopped) return false

        val expected = line.movesUci.getOrNull(cursorPly) ?: return false

        val probe = Board()
        probe.loadFromFen(board.fen)
        val move = buildMove(probe, from, to, promotion) ?: run {
            // Illegal geometry / piece — ignore without failing (parity: chess.js catch → false)
            return false
        }
        if (!probe.isMoveLegal(move, true)) return false

        probe.doMove(move)
        val got = move.toString().lowercase()
        if (got != expected.lowercase()) {
            onFail()
            return false
        }

        board.doMove(move)
        lastMove = from to to
        cursorPly += 1

        if (cursorPly >= line.movesUci.size) {
            phase = Phase.COMPLETE
            emit()
            return true
        }

        phase = Phase.SHOW
        emit()
        runShow()
        return true
    }

    fun snapshot(): MimeSnapshot {
        val expected = when (phase) {
            Phase.MIME, Phase.SHOW -> line.movesUci.getOrNull(cursorPly)
            else -> null
        }
        return MimeSnapshot(
            phase = phase,
            cursorPly = cursorPly,
            fen = board.fen,
            lastMove = lastMove,
            mistakes = mistakes,
            expectedUci = expected,
            flash = flash,
        )
    }

    private fun isLearnerPly(ply: Int): Boolean {
        val c = Board()
        c.loadFromFen(line.startFen)
        for (i in 0 until ply) {
            val u = line.movesUci.getOrNull(i) ?: break
            applyUciTo(c, u)
        }
        val turn = if (c.sideToMove == LibSide.WHITE) Side.white else Side.black
        return turn == line.sideToLearn
    }

    private fun runShow() {
        if (stopped || phase != Phase.SHOW) return
        val ply = cursorPly
        val uci = line.movesUci.getOrNull(ply)
        if (uci == null) {
            phase = Phase.COMPLETE
            emit()
            return
        }

        if (isLearnerPly(ply)) {
            applyUci(uci)
            emit()
            schedule(DEMO_HOLD_MS) {
                if (stopped) return@schedule
                undoLast()
                phase = Phase.MIME
                emit()
            }
        } else {
            applyUci(uci)
            cursorPly += 1
            emit()
            schedule(SHOW_PAUSE_MS) {
                if (stopped) return@schedule
                if (cursorPly >= line.movesUci.size) {
                    phase = Phase.COMPLETE
                    emit()
                    return@schedule
                }
                phase = Phase.SHOW
                emit()
                runShow()
            }
        }
    }

    private fun onFail() {
        mistakes += 1
        phase = Phase.FAIL
        flash = true
        emit()
        schedule(FAIL_FLASH_MS) {
            if (stopped) return@schedule
            flash = false
            resetBoard(0)
            phase = Phase.SHOW
            emit()
            runShow()
        }
    }

    private fun applyUci(uci: String) {
        applyUciTo(board, uci)
        val parts = parseUci(uci)
        lastMove = parts.from to parts.to
    }

    private fun applyUciTo(b: Board, uci: String) {
        val parts = parseUci(uci)
        val move = buildMove(b, parts.from, parts.to, parts.promotion)
            ?: error("Invalid UCI in line: $uci")
        b.doMove(move)
    }

    private fun undoLast() {
        board.undoMove()
        // Demo does not advance cursorPly; restore lastMove to prior ply highlight.
        lastMove = if (cursorPly > 0) {
            val parts = parseUci(line.movesUci[cursorPly - 1])
            parts.from to parts.to
        } else {
            null
        }
    }

    private fun resetBoard(ply: Int) {
        board = Board()
        board.loadFromFen(line.startFen)
        cursorPly = 0
        lastMove = null
        for (i in 0 until ply) {
            val u = line.movesUci.getOrNull(i) ?: break
            applyUci(u)
            cursorPly = i + 1
        }
    }

    private fun buildMove(
        b: Board,
        from: String,
        to: String,
        promotion: String?,
    ): Move? {
        return try {
            val fromSq = Square.fromValue(from.uppercase())
            val toSq = Square.fromValue(to.uppercase())
            val promoChar = promotion?.lowercase()?.firstOrNull()
            if (promoChar != null) {
                val piece = promotionPiece(b.sideToMove, promoChar) ?: return null
                Move(fromSq, toSq, piece)
            } else {
                // Auto-queen if pawn reaches back rank without explicit promo (web parity)
                val moving = b.getPiece(fromSq)
                val isPawn = moving == Piece.WHITE_PAWN || moving == Piece.BLACK_PAWN
                val toRank = to[1]
                if (isPawn && (toRank == '8' || toRank == '1')) {
                    val piece = promotionPiece(b.sideToMove, 'q') ?: return null
                    Move(fromSq, toSq, piece)
                } else {
                    Move(fromSq, toSq)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun promotionPiece(side: LibSide, c: Char): Piece? {
        return when (side) {
            LibSide.WHITE -> when (c) {
                'q' -> Piece.WHITE_QUEEN
                'r' -> Piece.WHITE_ROOK
                'b' -> Piece.WHITE_BISHOP
                'n' -> Piece.WHITE_KNIGHT
                else -> null
            }
            LibSide.BLACK -> when (c) {
                'q' -> Piece.BLACK_QUEEN
                'r' -> Piece.BLACK_ROOK
                'b' -> Piece.BLACK_BISHOP
                'n' -> Piece.BLACK_KNIGHT
                else -> null
            }
        }
    }

    private fun schedule(ms: Long, fn: () -> Unit) {
        clearTimer()
        timerJob = scope.launch {
            delay(ms)
            fn()
        }
    }

    private fun clearTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun emit() {
        onSnapshot(snapshot())
    }
}

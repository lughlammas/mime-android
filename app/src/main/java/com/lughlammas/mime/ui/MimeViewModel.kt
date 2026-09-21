package com.lughlammas.mime.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lughlammas.mime.data.Line
import com.lughlammas.mime.data.MapIndexEntry
import com.lughlammas.mime.data.MapRepository
import com.lughlammas.mime.data.MimeSnapshot
import com.lughlammas.mime.data.Phase
import com.lughlammas.mime.mime.MimeLoop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class Screen {
    data object Library : Screen()
    data class Play(val line: Line) : Screen()
}

data class UiState(
    val screen: Screen = Screen.Library,
    val maps: List<MapIndexEntry> = emptyList(),
    val mapsError: String? = null,
    val loadingMap: Boolean = false,
    val snapshot: MimeSnapshot? = null,
)

class MimeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MapRepository(app.applicationContext)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loop: MimeLoop? = null
    private var currentLine: Line? = null

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        try {
            val maps = repo.listMaps()
            _state.update { it.copy(maps = maps, mapsError = null, screen = Screen.Library) }
        } catch (e: Exception) {
            _state.update { it.copy(mapsError = e.message ?: e.toString(), maps = emptyList()) }
        }
    }

    fun startLine(path: String) {
        _state.update { it.copy(loadingMap = true) }
        try {
            val line = repo.getMap(path)
            beginSession(line)
        } catch (e: Exception) {
            _state.update { it.copy(loadingMap = false, mapsError = e.message ?: e.toString()) }
        }
    }

    fun goLibrary() {
        tearDown()
        _state.update {
            it.copy(screen = Screen.Library, snapshot = null, loadingMap = false)
        }
        loadLibrary()
    }

    fun retry() {
        val line = currentLine ?: return
        beginSession(line)
    }

    fun onUserMove(from: String, to: String, promotion: String? = null) {
        loop?.tryMove(from, to, promotion)
    }

    private fun beginSession(line: Line) {
        tearDown()
        currentLine = line
        val mime = MimeLoop(line, viewModelScope) { snap ->
            _state.update { it.copy(snapshot = snap) }
        }
        loop = mime
        _state.update {
            it.copy(
                screen = Screen.Play(line),
                loadingMap = false,
                snapshot = mime.snapshot(),
                mapsError = null,
            )
        }
        mime.start()
    }

    private fun tearDown() {
        loop?.stop()
        loop = null
    }

    override fun onCleared() {
        tearDown()
        super.onCleared()
    }
}

/** Legal destination squares for interactive MIME phase (for board highlights). */
fun legalDestsFromFen(fen: String): Map<String, List<String>> {
    return try {
        val board = com.github.bhlangonijr.chesslib.Board()
        board.loadFromFen(fen)
        val dests = mutableMapOf<String, MutableList<String>>()
        for (m in board.legalMoves()) {
            val from = m.from.toString().lowercase()
            val to = m.to.toString().lowercase()
            dests.getOrPut(from) { mutableListOf() }.add(to)
        }
        dests
    } catch (_: Exception) {
        emptyMap()
    }
}

fun turnFromFen(fen: String): com.lughlammas.mime.data.Side {
    return if (fen.contains(" w ")) {
        com.lughlammas.mime.data.Side.white
    } else {
        com.lughlammas.mime.data.Side.black
    }
}

fun isInteractive(phase: Phase?): Boolean = phase == Phase.MIME

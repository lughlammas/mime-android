package com.lughlammas.mime.data

import com.squareup.moshi.Json

enum class Side {
    white,
    black,
}

enum class Phase {
    SHOW, MIME, FAIL, COMPLETE
}

data class Line(
    val id: String,
    val name: String,
    val epithet: String = "",
    @Json(name = "side_to_learn") val sideToLearn: Side,
    @Json(name = "start_fen") val startFen: String,
    @Json(name = "moves_uci") val movesUci: List<String>,
    val length: Int,
    val tags: List<String> = emptyList(),
    val difficulty: Int = 1,
    val thesis: String = "",
    @Json(name = "validated_at") val validatedAt: String = "",
)

data class MapIndexEntry(
    val id: String,
    val name: String,
    val path: String,
    @Json(name = "side_to_learn") val sideToLearn: Side,
    val length: Int,
)

data class MapIndex(
    val maps: List<MapIndexEntry>,
)

data class UciParts(
    val from: String,
    val to: String,
    val promotion: String? = null,
)

data class MimeSnapshot(
    val phase: Phase,
    val cursorPly: Int,
    val fen: String,
    val lastMove: Pair<String, String>?,
    val mistakes: Int,
    val expectedUci: String?,
    val flash: Boolean,
)

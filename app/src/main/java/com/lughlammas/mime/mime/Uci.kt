package com.lughlammas.mime.mime

import com.lughlammas.mime.data.UciParts

fun parseUci(uci: String): UciParts {
    return UciParts(
        from = uci.substring(0, 2),
        to = uci.substring(2, 4),
        promotion = if (uci.length >= 5) uci.substring(4, 5) else null,
    )
}

fun toUci(from: String, to: String, promotion: String? = null): String {
    return from + to + (promotion ?: "")
}

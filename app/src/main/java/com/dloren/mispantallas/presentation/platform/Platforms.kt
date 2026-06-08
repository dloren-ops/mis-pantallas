package com.dloren.mispantallas.presentation.platform

import androidx.compose.ui.graphics.Color

/** Una plataforma de streaming con su color de marca. */
data class StreamingPlatform(val name: String, val color: Color)

/** Catálogo de plataformas que se ofrecen en el selector del formulario. */
object Platforms {

    val all: List<StreamingPlatform> = listOf(
        StreamingPlatform("Netflix", Color(0xFFE50914)),
        StreamingPlatform("Amazon Prime Video", Color(0xFF00A8E1)),
        StreamingPlatform("Disney+", Color(0xFF113CCF)),
        StreamingPlatform("Max", Color(0xFF5A2EBB)),
        StreamingPlatform("Star+", Color(0xFF6E2EF4)),
        StreamingPlatform("Paramount+", Color(0xFF0064FF)),
        StreamingPlatform("Apple TV+", Color(0xFF1C1C1E)),
        StreamingPlatform("Spotify", Color(0xFF1DB954)),
        StreamingPlatform("YouTube Premium", Color(0xFFFF0000)),
        StreamingPlatform("Crunchyroll", Color(0xFFF47521)),
        StreamingPlatform("Vix", Color(0xFFFF5A00)),
        StreamingPlatform("HBO", Color(0xFF7B2FF7)),
        StreamingPlatform("DirecTV GO", Color(0xFF00B0E6)),
        StreamingPlatform("IPTV", Color(0xFF2E7D32))
    )

    private val byName: Map<String, StreamingPlatform> =
        all.associateBy { it.name.lowercase() }

    /** Color de la plataforma por nombre; gris neutro si no está en el catálogo. */
    fun colorFor(name: String): Color {
        val key = name.trim().lowercase()
        byName[key]?.let { return it.color }
        // Coincidencia parcial (ej. "netflix premium").
        all.firstOrNull { key.contains(it.name.lowercase()) }?.let { return it.color }
        return Color(0xFF6750A4)
    }
}

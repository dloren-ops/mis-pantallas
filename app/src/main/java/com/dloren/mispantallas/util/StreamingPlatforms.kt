package com.dloren.mispantallas.util

import androidx.compose.ui.graphics.Color

/** Una plataforma de streaming disponible en el selector, con un color para distinguirla. */
data class StreamingPlatform(val name: String, val color: Color)

/** Plataformas que generalmente se revenden como pantallas/perfiles en alquiler. */
val STREAMING_PLATFORMS = listOf(
    StreamingPlatform("Netflix", Color(0xFFE50914)),
    StreamingPlatform("Disney+ Estándar", Color(0xFF0F4C9E)),
    StreamingPlatform("Disney+ Premium", Color(0xFF01153C)),
    StreamingPlatform("Max (HBO)", Color(0xFF8A2BE2)),
    StreamingPlatform("Prime Video", Color(0xFF00A8E1)),
    StreamingPlatform("Paramount+", Color(0xFF0064FF)),
    StreamingPlatform("Star+", Color(0xFF2D2D2D)),
    StreamingPlatform("Apple TV+", Color(0xFF000000)),
    StreamingPlatform("Crunchyroll", Color(0xFFF47521)),
    StreamingPlatform("Spotify", Color(0xFF1DB954))
)

/**
 * Color para mostrar junto a una plataforma en la lista de cuentas: usa el de
 * [STREAMING_PLATFORMS] si coincide, o uno generado a partir del nombre para que
 * cualquier otra plataforma ("Otra", o texto libre de un respaldo importado) también
 * se vea distinta y consistente entre pantallas.
 */
fun colorForPlatform(name: String): Color {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return Color(0xFF9E9E9E)
    STREAMING_PLATFORMS.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        ?.let { return it.color }
    val hue = ((trimmed.hashCode() % 360) + 360) % 360
    return Color.hsv(hue.toFloat(), 0.55f, 0.85f)
}

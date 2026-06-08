package com.dloren.mispantallas.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Purple = Color(0xFF6750A4)
private val PurpleDark = Color(0xFFD0BCFF)

private val LightColors = lightColorScheme(
    primary = Purple,
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260)
)

private val DarkColors = darkColorScheme(
    primary = PurpleDark,
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8)
)

@Composable
fun MisPantallasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

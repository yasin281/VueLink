package com.tekhmos.vuelinghelp.ui

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

val VuelingColorScheme = darkColorScheme(
    primary = VuelingYellow,       // Color principal: Amarillo Vueling
    onPrimary = VuelingBlack,      // Texto sobre primary: negro (buen contraste)
    background = VuelingBlack,             // Fondo general: blanco
    onBackground = Color.White,   // Texto sobre fondo: negro
    surface = Color.White,                // Superficies (tarjetas, etc): blanco
    onSurface = Color.White       // Texto sobre superficies: negro
)


val mainScreen = lightColorScheme(
    primary = Color.Black,           // Texto en negro
    onPrimary = Color.White,         // Fondo blanco
    background = Color.White,        // Fondo blanco
    surface = Color.White,           // Superficie blanca
    onBackground = Color.Black,      // Texto en negro para el fondo
    onSurface = Color.Black          // Texto en negro para la superficie
)

@Composable
fun VuelingHelpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> VuelingColorScheme
        else -> mainScreen
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
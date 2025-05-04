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

val VuelingDarkColorScheme = darkColorScheme(
    primary = VuelingYellow,           // Amarillo Vueling en el tema oscuro
    onPrimary = VuelingBlack,          // Texto en negro sobre el amarillo
    secondary = VuelingGray,           // Gris oscuro para los elementos secundarios
    onSecondary = VuelingWhite,        // Blanco para el texto en los elementos secundarios
    background = VuelingBlack,         // Fondo negro para el tema oscuro
    onBackground = VuelingLightGray,   // Texto blanco sobre fondo negro
    surface = VuelingGray,             // Superficies en gris oscuro
    onSurface = VuelingWhite,          // Texto blanco en las superficies
    surfaceVariant = VuelingLightGray, // Superficie variante en gris claro
    onSurfaceVariant = VuelingGray,    // Texto gris oscuro en la variante de superficie
    primaryContainer = VuelingYellow,  // Contenedor primario amarillo
    onPrimaryContainer = VuelingGray,  // Texto gris oscuro sobre el contenedor amarillo
    secondaryContainer = VuelingGray,  // Contenedor secundario gris oscuro
    onSecondaryContainer = VuelingWhite // Texto blanco en el contenedor secundario
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
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> VuelingDarkColorScheme
        else -> mainScreen
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
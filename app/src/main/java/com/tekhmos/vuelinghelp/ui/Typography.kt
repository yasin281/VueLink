package com.tekhmos.vuelinghelp.ui


import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tekhmos.vuelinghelp.R

val fontSource = FontFamily(
    Font(R.font.vuelingpilcrow)
)

val myTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = fontSource,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontSource,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.5).sp
    )
)

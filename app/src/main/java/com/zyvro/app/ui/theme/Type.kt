package com.zyvro.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zyvro.app.R

// Use system default (Roboto on Android) for reliability across all devices
private val NovaFont = FontFamily.Default

val Typography = Typography(
    // Display-level for hero sections
    displayLarge  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Black,    fontSize = 48.sp,  lineHeight = 54.sp,  letterSpacing = (-1.5).sp),
    displayMedium = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 46.sp,  letterSpacing = (-1.0).sp),
    displaySmall  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Bold,     fontSize = 32.sp,  lineHeight = 38.sp,  letterSpacing = (-0.5).sp),

    // Headlines
    headlineLarge  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Bold,    fontSize = 28.sp,  lineHeight = 34.sp,  letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Bold,    fontSize = 24.sp,  lineHeight = 30.sp,  letterSpacing = (-0.2).sp),
    headlineSmall  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),

    // Titles
    titleLarge  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),

    // Body
    bodyLarge  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

    // Labels
    labelLarge  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall  = TextStyle(fontFamily = NovaFont, fontWeight = FontWeight.Medium,   fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp)
)

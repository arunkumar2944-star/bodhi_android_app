package com.prodigit.bodhiattendance.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val appFont = FontFamily.Default

val Typography = Typography(
    displaySmall = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

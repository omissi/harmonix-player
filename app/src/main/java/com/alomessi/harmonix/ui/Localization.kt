package com.alomessi.harmonix.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalLanguage = staticCompositionLocalOf { "ar" }

@Composable
fun tr(arabic: String, english: String): String =
    if (LocalLanguage.current == "ar") arabic else english

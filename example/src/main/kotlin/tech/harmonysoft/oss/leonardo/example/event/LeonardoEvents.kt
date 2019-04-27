package tech.harmonysoft.oss.leonardo.example.event

import tech.harmonysoft.oss.leonardo.example.settings.ActiveTheme

data class ThemeChangedEvent(val currentTheme: ActiveTheme, val currentStyle: Int)
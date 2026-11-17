package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.ThemeAppearanceMode

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun ShujalMusicTheme(
    themeId: String = "obsidian_glass",
    appearanceMode: ThemeAppearanceMode = ThemeAppearanceMode.DARK,
    customAccentArgb: Long? = null,
    darkTheme: Boolean = when (appearanceMode) {
        ThemeAppearanceMode.DARK -> true
        ThemeAppearanceMode.LIGHT -> false
        ThemeAppearanceMode.SYSTEM -> isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
    val preset = ThemeCatalog.findById(themeId)
    val accent = customAccentArgb?.let { Color(it) } ?: preset.primary

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF040710),
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = Color.White,
            secondary = preset.secondary,
            onSecondary = Color(0xFF050812),
            secondaryContainer = preset.secondary.copy(alpha = 0.22f),
            onSecondaryContainer = Color.White,
            tertiary = preset.tertiary,
            background = preset.darkBg,
            onBackground = Color(0xFFF3F6FC),
            surface = preset.darkSurface,
            onSurface = Color(0xFFF3F6FC),
            surfaceVariant = preset.darkSurface.copy(alpha = 0.85f),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color.White.copy(alpha = 0.14f),
            error = Color(0xFFFB7185)
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.14f),
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = preset.secondary,
            onSecondary = Color.White,
            secondaryContainer = preset.secondary.copy(alpha = 0.15f),
            onSecondaryContainer = Color(0xFF0F172A),
            tertiary = preset.tertiary,
            background = preset.lightBg,
            onBackground = Color(0xFF0F172A),
            surface = preset.lightSurface,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFEAF0F8),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFF0F172A).copy(alpha = 0.12f),
            error = Color(0xFFE11D48)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}

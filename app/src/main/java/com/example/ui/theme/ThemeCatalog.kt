package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class ShujalThemePreset(
    val id: String,
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val darkBg: Color = Color(0xFF0A0D14),
    val darkSurface: Color = Color(0xFF131823),
    val lightBg: Color = Color(0xFFD2DDD9),
    val lightSurface: Color = Color(0xFFE3ECE9)
)

object ThemeCatalog {
    private val classicMist = ShujalThemePreset(
        id = "classic_mist",
        name = "Classic Mist",
        primary = Color(0xFF5D736F),
        secondary = Color(0xFF7D938F),
        tertiary = Color(0xFF3E4E4B),
        darkBg = Color(0xFF0B0E14),
        darkSurface = Color(0xFF151A24),
        lightBg = Color(0xFFCFDAD6),
        lightSurface = Color(0xFFE2EBE8)
    )

    private val basePalettes = listOf(
        Triple("Obsidian Glass", Color(0xFF38BDF8), Color(0xFF818CF8)) to Color(0xFFF472B6),
        Triple("Apple Crimson", Color(0xFFFA2D48), Color(0xFFFF6B81)) to Color(0xFFFF9F0A),
        Triple("Spotify Emerald", Color(0xFF1DB954), Color(0xFF34D399)) to Color(0xFF22D3EE),
        Triple("Midnight Cyan", Color(0xFF06B6D4), Color(0xFF3B82F6)) to Color(0xFFA855F7),
        Triple("Violet Nebula", Color(0xFFA855F7), Color(0xFFEC4899)) to Color(0xFF38BDF8),
        Triple("Solar Amber", Color(0xFFF59E0B), Color(0xFFFB7185)) to Color(0xFF38BDF8),
        Triple("Rose Titanium", Color(0xFFF43F5E), Color(0xFFE879F9)) to Color(0xFF60A5FA),
        Triple("Arctic Ice", Color(0xFF7DD3FC), Color(0xFFA5B4FC)) to Color(0xFF5EEAD4),
        Triple("Electric Lime", Color(0xFFA3E635), Color(0xFF2DD4BF)) to Color(0xFF38BDF8),
        Triple("Coral Sunset", Color(0xFFFF5E5B), Color(0xFFFFB563)) to Color(0xFFE056FD),
        Triple("Deep Cobalt", Color(0xFF2563EB), Color(0xFF6366F1)) to Color(0xFF06B6D4),
        Triple("Jade Aurora", Color(0xFF10B981), Color(0xFF0EA5E9)) to Color(0xFFA78BFA),
        Triple("Magenta Pulse", Color(0xFFD946EF), Color(0xFFF43F5E)) to Color(0xFF38BDF8),
        Triple("Gold Champagne", Color(0xFFEAB308), Color(0xFFF97316)) to Color(0xFFEC4899),
        Triple("Velvet Plum", Color(0xFF9333EA), Color(0xFFC084FC)) to Color(0xFFF472B6),
        Triple("Oceanic Teal", Color(0xFF14B8A6), Color(0xFF06B6D4)) to Color(0xFF818CF8),
        Triple("Neon Tangerine", Color(0xFFF97316), Color(0xFFFACC15)) to Color(0xFFEC4899),
        Triple("Monochrome Silver", Color(0xFFE2E8F0), Color(0xFF94A3B8)) to Color(0xFF38BDF8)
    )

    private val variants = listOf(
        "Core" to Color(0xFF080B12),
        "Velvet" to Color(0xFF0B0814),
        "Abyss" to Color(0xFF060E18)
    )

    val allThemes: List<ShujalThemePreset> = buildList {
        add(classicMist)
        basePalettes.forEachIndexed { idx, (triple, tertiary) ->
            val (baseName, primary, secondary) = triple
            variants.forEachIndexed { vIdx, (variantName, bg) ->
                val suffix = if (vIdx == 0) "" else " $variantName"
                val id = (baseName + suffix).lowercase().replace(" ", "_")
                val surfaceShift = when (vIdx) {
                    1 -> Color(0xFF161124)
                    2 -> Color(0xFF0D1724)
                    else -> Color(0xFF121826)
                }
                add(
                    ShujalThemePreset(
                        id = if (idx == 0 && vIdx == 0) "obsidian_glass" else id,
                        name = "$baseName$suffix",
                        primary = primary,
                        secondary = secondary,
                        tertiary = tertiary,
                        darkBg = bg,
                        darkSurface = surfaceShift
                    )
                )
            }
        }
    }

    fun findById(id: String): ShujalThemePreset =
        allThemes.firstOrNull { it.id == id } ?: classicMist
}

package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.UserPreferences
import com.example.model.DynamicIslandStyle
import com.example.model.RingLightAction
import com.example.model.RingLightTheme
import com.example.model.ThemeAppearanceMode
import com.example.model.Track
import com.example.ui.components.AdMobBanner
import com.example.ui.components.DynamicIslandCapsule
import com.example.ui.components.GlassCard
import com.example.ui.components.formatTimeMs
import com.example.ui.components.ringLightThemePalette
import com.example.ui.theme.ThemeCatalog
import kotlin.math.roundToInt

private val ACCENT_SWATCHES = listOf(
    null to "Theme",
    0xFF5D7A76L to "Mist Sage",
    0xFF38BDF8L to "Cyan",
    0xFFFA2D48L to "Apple Red",
    0xFF1DB954L to "Spotify",
    0xFFA855F7L to "Violet",
    0xFFF59E0BL to "Amber"
)

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    sleepTimerRemainingMs: Long,
    excludedFolders: List<ExcludedFolderEntity>,
    duplicates: List<List<Track>>,
    onSelectTheme: (String) -> Unit,
    onSetAppearance: (ThemeAppearanceMode) -> Unit,
    onSetCustomAccent: (Long?) -> Unit,
    onSetDynamicIslandOverlay: (Boolean) -> Unit,
    onSetDynamicIslandInApp: (Boolean) -> Unit,
    onSelectDynamicIslandStyle: (DynamicIslandStyle) -> Unit,
    onSetDynamicIslandAlwaysExtended: (Boolean) -> Unit,
    onSetRingLightEnabled: (Boolean) -> Unit,
    onStartRingLightCursorEdit: () -> Unit,
    onResetRingLightPosition: () -> Unit,
    onUpdateRingLightGeometry: (xFraction: Float?, yDp: Float?, sizeDp: Float?, thicknessDp: Float?) -> Unit,
    onSelectRingLightTheme: (RingLightTheme) -> Unit,
    onUpdateRingLightActions: (
        tap: RingLightAction?,
        swipeLeft: RingLightAction?,
        swipeRight: RingLightAction?,
        swipeUp: RingLightAction?,
        swipeDown: RingLightAction?
    ) -> Unit,
    onPreviewFloatingIsland: () -> Unit,
    onSetGestures: (Boolean) -> Unit,
    onSetLockScreenControls: (Boolean) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onUpdateAudioSettings: (
        balance: Float?,
        speed: Float?,
        pitch: Float?,
        gapless: Boolean?,
        crossfadeSec: Int?,
        normalization: Boolean?,
        fadePlayPause: Boolean?
    ) -> Unit,
    onIncludeFolder: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAllThemes by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var editingGestureSlot by remember { mutableStateOf<String?>(null) }

    var crossfadeSec by remember(prefs.crossfadeSeconds) { mutableIntStateOf(prefs.crossfadeSeconds) }
    var balance by remember(prefs.stereoBalance) { mutableFloatStateOf(prefs.stereoBalance) }
    var speed by remember(prefs.playbackSpeed) { mutableFloatStateOf(prefs.playbackSpeed) }
    var pitch by remember(prefs.playbackPitch) { mutableFloatStateOf(prefs.playbackPitch) }
    var ringX by remember(prefs.ringLightXFraction) { mutableFloatStateOf(prefs.ringLightXFraction) }
    var ringY by remember(prefs.ringLightYDp) { mutableFloatStateOf(prefs.ringLightYDp) }
    var ringSize by remember(prefs.ringLightSizeDp) { mutableFloatStateOf(prefs.ringLightSizeDp) }
    var ringThickness by remember(prefs.ringLightThicknessDp) { mutableFloatStateOf(prefs.ringLightThicknessDp) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        // Audio Configuration: Crossfade, Gapless, Normalization, Fade, Balance, Speed/Pitch
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Audio Configuration", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Crossfade Smooth Transitions", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (crossfadeSec == 0) "Off" else "${crossfadeSec}s",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = crossfadeSec.toFloat(),
                    onValueChange = { crossfadeSec = it.roundToInt() },
                    onValueChangeFinished = {
                        onUpdateAudioSettings(null, null, null, null, crossfadeSec, null, null)
                    },
                    valueRange = 0f..12f,
                    steps = 11,
                    modifier = Modifier.testTag("crossfade_slider")
                )

                SettingsToggle(
                    label = "Gapless Playback",
                    checked = prefs.gaplessEnabled,
                    onCheckedChange = { onUpdateAudioSettings(null, null, null, it, null, null, null) }
                )
                SettingsToggle(
                    label = "Volume Normalization",
                    checked = prefs.volumeNormalization,
                    onCheckedChange = { onUpdateAudioSettings(null, null, null, null, null, it, null) }
                )
                SettingsToggle(
                    label = "Fade In / Out on Play & Pause",
                    checked = prefs.fadeOnPlayPause,
                    onCheckedChange = { onUpdateAudioSettings(null, null, null, null, null, null, it) }
                )

                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Stereo Balance", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when {
                            balance < -0.05f -> "L ${((-balance) * 100).roundToInt()}%"
                            balance > 0.05f -> "R ${(balance * 100).roundToInt()}%"
                            else -> "Center"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = balance,
                    onValueChange = { balance = it },
                    onValueChangeFinished = {
                        onUpdateAudioSettings(balance, null, null, null, null, null, null)
                    },
                    valueRange = -1f..1f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Playback Speed", style = MaterialTheme.typography.bodyLarge)
                    Text("${"%.2f".format(speed)}x", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    onValueChangeFinished = {
                        onUpdateAudioSettings(null, speed, null, null, null, null, null)
                    },
                    valueRange = 0.5f..2.0f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Playback Pitch", style = MaterialTheme.typography.bodyLarge)
                    Text("${"%.2f".format(pitch)}x", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = pitch,
                    onValueChange = { pitch = it },
                    onValueChangeFinished = {
                        onUpdateAudioSettings(null, null, pitch, null, null, null, null)
                    },
                    valueRange = 0.5f..2.0f
                )

                TextButton(
                    onClick = {
                        balance = 0f
                        speed = 1f
                        pitch = 1f
                        onUpdateAudioSettings(0f, 1f, 1f, null, null, null, null)
                    }
                ) {
                    Text("Reset Balance, Speed & Pitch")
                }
            }
        }

        // Sleep Timer Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Sleep Timer", style = MaterialTheme.typography.titleMedium)
                    }
                    if (sleepTimerRemainingMs > 0L) {
                        Text(
                            text = formatTimeMs(sleepTimerRemainingMs),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                val sleepOptions = listOf(
                    0 to "Off",
                    10 to "10 min",
                    15 to "15 min",
                    30 to "30 min",
                    45 to "45 min",
                    60 to "60 min",
                    -1 to "End of Track"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sleepOptions) { (minutes, label) ->
                        FilterChip(
                            selected = minutes == 0 && sleepTimerRemainingMs == 0L,
                            onClick = { onSetSleepTimer(minutes) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("sleep_chip_$minutes")
                        )
                    }
                }
            }
        }

        // Theme Customization (Classic Mist & Theme Gallery + Dark/Light + Custom Accent)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Appearance & Themes", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAllThemes = !showAllThemes }) {
                        Text(if (showAllThemes) "Compact" else "See All")
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ThemeAppearanceMode.entries) { mode ->
                        FilterChip(
                            selected = prefs.appearanceMode == mode,
                            onClick = { onSetAppearance(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Custom Accent Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ACCENT_SWATCHES) { (argb, label) ->
                        val selected = prefs.customAccentArgb == argb
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onSetCustomAccent(argb) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(argb?.let { Color(it) } ?: MaterialTheme.colorScheme.primary)
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                val visibleThemes = if (showAllThemes) ThemeCatalog.allThemes else ThemeCatalog.allThemes.take(12)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(visibleThemes, key = { it.id }) { theme ->
                        val selected = prefs.themeId == theme.id
                        Column(
                            modifier = Modifier
                                .width(98.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(theme.darkBg)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) theme.primary else Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectTheme(theme.id) }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(theme.primary, theme.secondary)))
                            )
                            Text(theme.name, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Dynamic Island & Controls (NO Android permission names)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Dynamic Island & Controls", style = MaterialTheme.typography.titleMedium)
                SettingsToggle("Floating Player in Background", prefs.dynamicIslandOverlayEnabled, onSetDynamicIslandOverlay)
                SettingsToggle("In-App Dynamic Island Capsule", prefs.dynamicIslandInAppEnabled, onSetDynamicIslandInApp)
                SettingsToggle(
                    "Always Extended Capsule (Long Bar)",
                    prefs.dynamicIslandAlwaysExtended,
                    onSetDynamicIslandAlwaysExtended
                )
                SettingsToggle("Swipe Gestures on Player", prefs.gesturesEnabled, onSetGestures)
                SettingsToggle("Lock Screen & Notification Controls", prefs.lockScreenControls, onSetLockScreenControls)

                Spacer(Modifier.height(10.dp))
                Text("Popup Island Style", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))

                // Interactive Live Preview of Selected Popup Island Style
                val samplePreviewTrack = remember {
                    Track(
                        id = "preview_island",
                        title = "Shujal Vibes",
                        artist = "Now Playing Preview",
                        album = "Dynamic Island",
                        durationMs = 210_000L,
                        uri = ""
                    )
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicIslandCapsule(
                        track = samplePreviewTrack,
                        isPlaying = true,
                        positionMs = 84_000L,
                        durationMs = 210_000L,
                        visualizerLevels = listOf(0.45f, 0.85f, 0.60f, 0.95f, 0.50f, 0.78f, 0.40f),
                        style = prefs.dynamicIslandStyle,
                        alwaysExtended = prefs.dynamicIslandAlwaysExtended,
                        isFloatingOverlay = false,
                        onPlayPause = {},
                        onNext = {},
                        onPrevious = {},
                        onOpenPlayer = {}
                    )
                }

                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(DynamicIslandStyle.entries, key = { it.name }) { style ->
                        val selected = prefs.dynamicIslandStyle == style
                        val swatchColors = when (style) {
                            DynamicIslandStyle.CLASSIC_PILL -> listOf(Color(0xFF0A0D16), Color(0xFF1F2937))
                            DynamicIslandStyle.WIDE_CAPSULE -> listOf(Color(0xFF0B111E), Color(0xFF38BDF8))
                            DynamicIslandStyle.LIQUID_GLASS -> listOf(Color(0xFF28354D), Color(0xFF7DD3FC))
                            DynamicIslandStyle.NEON_PULSE -> listOf(Color(0xFFFF2E93), Color(0xFF00F5D4))
                            DynamicIslandStyle.VINYL_DISC -> listOf(Color(0xFF261F1B), Color(0xFFF59E0B))
                            DynamicIslandStyle.AURORA_WAVE -> listOf(Color(0xFF34D399), Color(0xFFA78BFA))
                            DynamicIslandStyle.MINIMAL_NOTCH -> listOf(Color(0xFF000000), Color(0xFF334155))
                        }
                        Column(
                            modifier = Modifier
                                .width(136.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable { onSelectDynamicIslandStyle(style) }
                                .padding(10.dp)
                                .testTag("island_style_${style.name.lowercase()}"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Brush.horizontalGradient(swatchColors))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(999.dp)),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "Selected style",
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                            Text(
                                text = style.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onPreviewFloatingIsland) {
                    Text("Preview Floating Capsule")
                }
            }
        }

        // Camera Ring Light Customization
        item {
            val primary = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.CameraFront, contentDescription = null, tint = primary)
                    Text("Camera Ring Light & Gestures", style = MaterialTheme.typography.titleMedium)
                }
                SettingsToggle(
                    label = "Permanent Camera Ring Light",
                    checked = prefs.ringLightEnabled,
                    onCheckedChange = onSetRingLightEnabled
                )
                Text(
                    "Place a glowing ring permanently around your front camera cutout. Tap once to open Song Controls or swipe in any direction.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStartRingLightCursorEdit,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ring_light_cursor_button")
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Set with Cursor")
                    }
                    OutlinedButton(
                        onClick = onResetRingLightPosition,
                        modifier = Modifier.testTag("ring_light_reset_button")
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Horizontal Camera X", style = MaterialTheme.typography.bodyMedium)
                    Text("${(ringX * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = primary)
                }
                Slider(
                    value = ringX,
                    onValueChange = { ringX = it },
                    onValueChangeFinished = { onUpdateRingLightGeometry(ringX, null, null, null) },
                    valueRange = 0.05f..0.95f
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vertical Camera Y", style = MaterialTheme.typography.bodyMedium)
                    Text("${ringY.roundToInt()} dp", style = MaterialTheme.typography.labelLarge, color = primary)
                }
                Slider(
                    value = ringY,
                    onValueChange = { ringY = it },
                    onValueChangeFinished = { onUpdateRingLightGeometry(null, ringY, null, null) },
                    valueRange = -12f..160f
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ring Diameter", style = MaterialTheme.typography.bodyMedium)
                    Text("${ringSize.roundToInt()} dp", style = MaterialTheme.typography.labelLarge, color = primary)
                }
                Slider(
                    value = ringSize,
                    onValueChange = { ringSize = it },
                    onValueChangeFinished = { onUpdateRingLightGeometry(null, null, ringSize, null) },
                    valueRange = 22f..96f
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ring Thickness", style = MaterialTheme.typography.bodyMedium)
                    Text("${"%.1f".format(ringThickness)} dp", style = MaterialTheme.typography.labelLarge, color = primary)
                }
                Slider(
                    value = ringThickness,
                    onValueChange = { ringThickness = it },
                    onValueChangeFinished = { onUpdateRingLightGeometry(null, null, null, ringThickness) },
                    valueRange = 1.5f..9f
                )

                Spacer(Modifier.height(8.dp))
                Text("Ring Light Themes", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(RingLightTheme.entries, key = { it.name }) { theme ->
                        val selected = prefs.ringLightTheme == theme
                        val palette = ringLightThemePalette(theme, primary, secondary)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .width(88.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) primary else Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectRingLightTheme(theme) }
                                .padding(10.dp)
                                .testTag("ring_theme_${theme.name.lowercase()}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .border(4.dp, Brush.sweepGradient(palette), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = theme.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Ring Light Touch & Swipe Actions", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                SettingsActionRow(
                    icon = Icons.Rounded.TouchApp,
                    title = "Tap (1st Click)",
                    subtitle = prefs.ringLightTapAction.label,
                    testTag = "ring_gesture_tap",
                    onClick = { editingGestureSlot = "TAP" }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsActionRow(
                    icon = Icons.Rounded.TouchApp,
                    title = "Swipe Left",
                    subtitle = prefs.ringLightSwipeLeftAction.label,
                    testTag = "ring_gesture_left",
                    onClick = { editingGestureSlot = "LEFT" }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsActionRow(
                    icon = Icons.Rounded.TouchApp,
                    title = "Swipe Right",
                    subtitle = prefs.ringLightSwipeRightAction.label,
                    testTag = "ring_gesture_right",
                    onClick = { editingGestureSlot = "RIGHT" }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsActionRow(
                    icon = Icons.Rounded.TouchApp,
                    title = "Swipe Up",
                    subtitle = prefs.ringLightSwipeUpAction.label,
                    testTag = "ring_gesture_up",
                    onClick = { editingGestureSlot = "UP" }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsActionRow(
                    icon = Icons.Rounded.TouchApp,
                    title = "Swipe Down",
                    subtitle = prefs.ringLightSwipeDownAction.label,
                    testTag = "ring_gesture_down",
                    onClick = { editingGestureSlot = "DOWN" }
                )
            }
        }

        // Storage Management: Duplicate Detector & Excluded Folders
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Storage & Folder Management", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Detected Duplicate Groups: ${duplicates.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                duplicates.take(3).forEach { group ->
                    Text(
                        "• ${group.first().title} (${group.size} copies)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (excludedFolders.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Excluded Folders:", style = MaterialTheme.typography.labelLarge)
                    excludedFolders.forEach { folder ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(folder.folderPath, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onIncludeFolder(folder.folderPath) }) { Text("Restore") }
                        }
                    }
                }
            }
        }

        // Share App & Privacy Policy
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("App & Legal", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                SettingsActionRow(
                    icon = Icons.Rounded.Share,
                    title = "Share App",
                    subtitle = "Share Shujal Music with friends & family",
                    testTag = "settings_share_app",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Shujal Music Player")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Listen to offline & online full songs with iOS Dynamic Island on Shujal Music Player!"
                            )
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Shujal Music"))
                        }.onFailure {
                            onShowMessage("Unable to open share dialog")
                        }
                    }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsActionRow(
                    icon = Icons.Rounded.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "How your local audio, playlists & privacy are protected",
                    testTag = "settings_privacy_policy",
                    onClick = { showPrivacyDialog = true }
                )
            }
        }

        // Contact Us (Developer Support)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Contact Us", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Developer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Shubham Kumar", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Creator & Developer — Shujal Music",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.08f)
                )
                SettingsActionRow(
                    icon = Icons.Rounded.Chat,
                    title = "WhatsApp",
                    subtitle = "+916260273863",
                    testTag = "contact_whatsapp",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/916260273863"))
                        runCatching { context.startActivity(intent) }
                            .onFailure { onShowMessage("WhatsApp could not be opened") }
                    }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsActionRow(
                    icon = Icons.Rounded.Email,
                    title = "Email",
                    subtitle = "kumaruikeshubham@gmail.com",
                    testTag = "contact_email",
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:kumaruikeshubham@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Shujal Music Player Support")
                        }
                        runCatching { context.startActivity(emailIntent) }
                            .onFailure { onShowMessage("No email app found") }
                    }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                SettingsActionRow(
                    icon = Icons.Rounded.AlternateEmail,
                    title = "Instagram",
                    subtitle = "@msrshubham_k",
                    testTag = "contact_instagram",
                    onClick = {
                        val igIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/msrshubham_k"))
                        runCatching { context.startActivity(igIntent) }
                            .onFailure { onShowMessage("Instagram could not be opened") }
                    }
                )
            }
        }

        // About App
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("About Shujal Music", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Version 1.0 • Developed by Shubham Kumar.\nAll-in-one offline & online music player featuring iOS Dynamic Island styles, synced LRC lyrics, crossfade audio engine, and background foreground playback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "settings_ad_banner") {
            AdMobBanner()
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    "Shujal Music respects your privacy:\n\n" +
                        "1. Local Device Audio: Songs on your phone or SD card are scanned locally on-device solely for playback and are never uploaded anywhere.\n\n" +
                        "2. Playlists & Preferences: Your playlists, favourites, theme settings, and island style preferences are stored locally on your device.\n\n" +
                        "3. Online Music & Ads: Online song searches, synced lyrics, and Google AdMob banners/interstitials use standard internet connections only when loading online streams or displaying ads.\n\n" +
                        "For questions or support, contact Shubham Kumar at kumaruikeshubham@gmail.com."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    editingGestureSlot?.let { slot ->
        val slotTitle = when (slot) {
            "TAP" -> "Tap (1st Click)"
            "LEFT" -> "Swipe Left"
            "RIGHT" -> "Swipe Right"
            "UP" -> "Swipe Up"
            else -> "Swipe Down"
        }
        val currentSelection = when (slot) {
            "TAP" -> prefs.ringLightTapAction
            "LEFT" -> prefs.ringLightSwipeLeftAction
            "RIGHT" -> prefs.ringLightSwipeRightAction
            "UP" -> prefs.ringLightSwipeUpAction
            else -> prefs.ringLightSwipeDownAction
        }
        AlertDialog(
            onDismissRequest = { editingGestureSlot = null },
            title = { Text("Choose Action: $slotTitle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    RingLightAction.entries.forEach { action ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    when (slot) {
                                        "TAP" -> onUpdateRingLightActions(action, null, null, null, null)
                                        "LEFT" -> onUpdateRingLightActions(null, action, null, null, null)
                                        "RIGHT" -> onUpdateRingLightActions(null, null, action, null, null)
                                        "UP" -> onUpdateRingLightActions(null, null, null, action, null)
                                        "DOWN" -> onUpdateRingLightActions(null, null, null, null, action)
                                    }
                                    editingGestureSlot = null
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSelection == action,
                                onClick = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(action.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { editingGestureSlot = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DynamicIslandStyle
import com.example.model.RepeatModeState
import com.example.model.RingLightAction
import com.example.model.RingLightTheme
import com.example.model.ThemeAppearanceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.musicDataStore: DataStore<Preferences> by preferencesDataStore(name = "shujal_music_prefs")

data class UserPreferences(
    val themeId: String = "classic_mist",
    val appearanceMode: ThemeAppearanceMode = ThemeAppearanceMode.DARK,
    val customAccentArgb: Long? = null,
    val onboardingPermissionsDone: Boolean = false,
    val dynamicIslandOverlayEnabled: Boolean = true,
    val dynamicIslandInAppEnabled: Boolean = true,
    val dynamicIslandStyle: DynamicIslandStyle = DynamicIslandStyle.CLASSIC_PILL,
    val dynamicIslandAlwaysExtended: Boolean = false,
    val ringLightEnabled: Boolean = false,
    val ringLightXFraction: Float = 0.5f,
    val ringLightYDp: Float = 10f,
    val ringLightSizeDp: Float = 42f,
    val ringLightThicknessDp: Float = 3.5f,
    val ringLightTheme: RingLightTheme = RingLightTheme.RAINBOW_FLOW,
    val ringLightTapAction: RingLightAction = RingLightAction.SONG_POPUP,
    val ringLightSwipeLeftAction: RingLightAction = RingLightAction.PREVIOUS_SONG,
    val ringLightSwipeRightAction: RingLightAction = RingLightAction.NEXT_SONG,
    val ringLightSwipeUpAction: RingLightAction = RingLightAction.VOLUME_UP,
    val ringLightSwipeDownAction: RingLightAction = RingLightAction.VOLUME_DOWN,
    val stereoBalance: Float = 0f,
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val gaplessEnabled: Boolean = true,
    val crossfadeSeconds: Int = 3,
    val volumeNormalization: Boolean = true,
    val fadeOnPlayPause: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val lockScreenControls: Boolean = true,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val jamendoClientId: String = "",
    val customStreamEndpoint: String = ""
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val CUSTOM_ACCENT = longPreferencesKey("custom_accent_argb")
        val HAS_CUSTOM_ACCENT = booleanPreferencesKey("has_custom_accent")
        val ONBOARDING_PERMS_DONE = booleanPreferencesKey("onboarding_perms_done")
        val ISLAND_OVERLAY = booleanPreferencesKey("dynamic_island_overlay")
        val ISLAND_IN_APP = booleanPreferencesKey("dynamic_island_in_app")
        val ISLAND_STYLE = stringPreferencesKey("dynamic_island_style")
        val ISLAND_ALWAYS_EXTENDED = booleanPreferencesKey("dynamic_island_always_extended")
        val RING_ENABLED = booleanPreferencesKey("ring_light_enabled")
        val RING_X_FRACTION = floatPreferencesKey("ring_light_x_fraction")
        val RING_Y_DP = floatPreferencesKey("ring_light_y_dp")
        val RING_SIZE_DP = floatPreferencesKey("ring_light_size_dp")
        val RING_THICKNESS_DP = floatPreferencesKey("ring_light_thickness_dp")
        val RING_THEME = stringPreferencesKey("ring_light_theme")
        val RING_TAP_ACTION = stringPreferencesKey("ring_light_tap_action")
        val RING_SWIPE_LEFT = stringPreferencesKey("ring_light_swipe_left")
        val RING_SWIPE_RIGHT = stringPreferencesKey("ring_light_swipe_right")
        val RING_SWIPE_UP = stringPreferencesKey("ring_light_swipe_up")
        val RING_SWIPE_DOWN = stringPreferencesKey("ring_light_swipe_down")
        val STEREO_BALANCE = floatPreferencesKey("stereo_balance")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
        val GAPLESS = booleanPreferencesKey("gapless")
        val CROSSFADE = intPreferencesKey("crossfade_sec")
        val NORMALIZATION = booleanPreferencesKey("volume_normalization")
        val FADE_ON_PLAY_PAUSE = booleanPreferencesKey("fade_play_pause")
        val GESTURES = booleanPreferencesKey("gestures")
        val LOCK_SCREEN_CONTROLS = booleanPreferencesKey("lock_screen_controls")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val JAMENDO_CLIENT_ID = stringPreferencesKey("jamendo_client_id")
        val CUSTOM_STREAM_ENDPOINT = stringPreferencesKey("custom_stream_endpoint")
    }

    private fun parseAction(raw: String?, fallback: RingLightAction): RingLightAction =
        runCatching { RingLightAction.valueOf(raw ?: fallback.name) }.getOrDefault(fallback)

    val preferencesFlow: Flow<UserPreferences> = context.musicDataStore.data.map { prefs ->
        val hasCustomAccent = prefs[Keys.HAS_CUSTOM_ACCENT] ?: false
        val appearanceName = prefs[Keys.APPEARANCE_MODE] ?: ThemeAppearanceMode.DARK.name
        UserPreferences(
            themeId = prefs[Keys.THEME_ID] ?: "classic_mist",
            appearanceMode = runCatching { ThemeAppearanceMode.valueOf(appearanceName) }
                .getOrDefault(ThemeAppearanceMode.DARK),
            customAccentArgb = if (hasCustomAccent) prefs[Keys.CUSTOM_ACCENT] else null,
            onboardingPermissionsDone = prefs[Keys.ONBOARDING_PERMS_DONE] ?: false,
            dynamicIslandOverlayEnabled = prefs[Keys.ISLAND_OVERLAY] ?: true,
            dynamicIslandInAppEnabled = prefs[Keys.ISLAND_IN_APP] ?: true,
            dynamicIslandStyle = runCatching {
                DynamicIslandStyle.valueOf(prefs[Keys.ISLAND_STYLE] ?: DynamicIslandStyle.CLASSIC_PILL.name)
            }.getOrDefault(DynamicIslandStyle.CLASSIC_PILL),
            dynamicIslandAlwaysExtended = prefs[Keys.ISLAND_ALWAYS_EXTENDED] ?: false,
            ringLightEnabled = prefs[Keys.RING_ENABLED] ?: false,
            ringLightXFraction = (prefs[Keys.RING_X_FRACTION] ?: 0.5f).coerceIn(0.05f, 0.95f),
            ringLightYDp = (prefs[Keys.RING_Y_DP] ?: 10f).coerceIn(-12f, 160f),
            ringLightSizeDp = (prefs[Keys.RING_SIZE_DP] ?: 42f).coerceIn(22f, 96f),
            ringLightThicknessDp = (prefs[Keys.RING_THICKNESS_DP] ?: 3.5f).coerceIn(1.5f, 9f),
            ringLightTheme = runCatching {
                RingLightTheme.valueOf(prefs[Keys.RING_THEME] ?: RingLightTheme.RAINBOW_FLOW.name)
            }.getOrDefault(RingLightTheme.RAINBOW_FLOW),
            ringLightTapAction = parseAction(prefs[Keys.RING_TAP_ACTION], RingLightAction.SONG_POPUP),
            ringLightSwipeLeftAction = parseAction(prefs[Keys.RING_SWIPE_LEFT], RingLightAction.PREVIOUS_SONG),
            ringLightSwipeRightAction = parseAction(prefs[Keys.RING_SWIPE_RIGHT], RingLightAction.NEXT_SONG),
            ringLightSwipeUpAction = parseAction(prefs[Keys.RING_SWIPE_UP], RingLightAction.VOLUME_UP),
            ringLightSwipeDownAction = parseAction(prefs[Keys.RING_SWIPE_DOWN], RingLightAction.VOLUME_DOWN),
            stereoBalance = prefs[Keys.STEREO_BALANCE] ?: 0f,
            playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1f,
            playbackPitch = prefs[Keys.PLAYBACK_PITCH] ?: 1f,
            gaplessEnabled = prefs[Keys.GAPLESS] ?: true,
            crossfadeSeconds = prefs[Keys.CROSSFADE] ?: 3,
            volumeNormalization = prefs[Keys.NORMALIZATION] ?: true,
            fadeOnPlayPause = prefs[Keys.FADE_ON_PLAY_PAUSE] ?: true,
            gesturesEnabled = prefs[Keys.GESTURES] ?: true,
            lockScreenControls = prefs[Keys.LOCK_SCREEN_CONTROLS] ?: true,
            shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
            repeatMode = runCatching {
                RepeatModeState.valueOf(prefs[Keys.REPEAT_MODE] ?: RepeatModeState.OFF.name)
            }.getOrDefault(RepeatModeState.OFF),
            jamendoClientId = prefs[Keys.JAMENDO_CLIENT_ID] ?: "",
            customStreamEndpoint = prefs[Keys.CUSTOM_STREAM_ENDPOINT] ?: ""
        )
    }

    suspend fun setTheme(themeId: String) {
        context.musicDataStore.edit { it[Keys.THEME_ID] = themeId }
    }

    suspend fun setAppearanceMode(mode: ThemeAppearanceMode) {
        context.musicDataStore.edit { it[Keys.APPEARANCE_MODE] = mode.name }
    }

    suspend fun setCustomAccent(argb: Long?) {
        context.musicDataStore.edit {
            if (argb == null) {
                it[Keys.HAS_CUSTOM_ACCENT] = false
            } else {
                it[Keys.HAS_CUSTOM_ACCENT] = true
                it[Keys.CUSTOM_ACCENT] = argb
            }
        }
    }

    suspend fun setOnboardingPermissionsDone(done: Boolean) {
        context.musicDataStore.edit { it[Keys.ONBOARDING_PERMS_DONE] = done }
    }

    suspend fun setDynamicIslandOverlay(enabled: Boolean) {
        context.musicDataStore.edit { it[Keys.ISLAND_OVERLAY] = enabled }
    }

    suspend fun setDynamicIslandInApp(enabled: Boolean) {
        context.musicDataStore.edit { it[Keys.ISLAND_IN_APP] = enabled }
    }

    suspend fun setDynamicIslandStyle(style: DynamicIslandStyle) {
        context.musicDataStore.edit { it[Keys.ISLAND_STYLE] = style.name }
    }

    suspend fun setDynamicIslandAlwaysExtended(alwaysExtended: Boolean) {
        context.musicDataStore.edit { it[Keys.ISLAND_ALWAYS_EXTENDED] = alwaysExtended }
    }

    suspend fun setRingLightEnabled(enabled: Boolean) {
        context.musicDataStore.edit { it[Keys.RING_ENABLED] = enabled }
    }

    suspend fun updateRingLightGeometry(
        xFraction: Float? = null,
        yDp: Float? = null,
        sizeDp: Float? = null,
        thicknessDp: Float? = null
    ) {
        context.musicDataStore.edit { prefs ->
            xFraction?.let { prefs[Keys.RING_X_FRACTION] = it.coerceIn(0.05f, 0.95f) }
            yDp?.let { prefs[Keys.RING_Y_DP] = it.coerceIn(-12f, 160f) }
            sizeDp?.let { prefs[Keys.RING_SIZE_DP] = it.coerceIn(22f, 96f) }
            thicknessDp?.let { prefs[Keys.RING_THICKNESS_DP] = it.coerceIn(1.5f, 9f) }
        }
    }

    suspend fun setRingLightTheme(theme: RingLightTheme) {
        context.musicDataStore.edit { it[Keys.RING_THEME] = theme.name }
    }

    suspend fun updateRingLightActions(
        tap: RingLightAction? = null,
        swipeLeft: RingLightAction? = null,
        swipeRight: RingLightAction? = null,
        swipeUp: RingLightAction? = null,
        swipeDown: RingLightAction? = null
    ) {
        context.musicDataStore.edit { prefs ->
            tap?.let { prefs[Keys.RING_TAP_ACTION] = it.name }
            swipeLeft?.let { prefs[Keys.RING_SWIPE_LEFT] = it.name }
            swipeRight?.let { prefs[Keys.RING_SWIPE_RIGHT] = it.name }
            swipeUp?.let { prefs[Keys.RING_SWIPE_UP] = it.name }
            swipeDown?.let { prefs[Keys.RING_SWIPE_DOWN] = it.name }
        }
    }

    suspend fun updateAudioSettings(
        balance: Float? = null,
        speed: Float? = null,
        pitch: Float? = null,
        gapless: Boolean? = null,
        crossfadeSec: Int? = null,
        normalization: Boolean? = null,
        fadePlayPause: Boolean? = null
    ) {
        context.musicDataStore.edit { prefs ->
            balance?.let { prefs[Keys.STEREO_BALANCE] = it }
            speed?.let { prefs[Keys.PLAYBACK_SPEED] = it }
            pitch?.let { prefs[Keys.PLAYBACK_PITCH] = it }
            gapless?.let { prefs[Keys.GAPLESS] = it }
            crossfadeSec?.let { prefs[Keys.CROSSFADE] = it }
            normalization?.let { prefs[Keys.NORMALIZATION] = it }
            fadePlayPause?.let { prefs[Keys.FADE_ON_PLAY_PAUSE] = it }
        }
    }

    suspend fun setGesturesEnabled(enabled: Boolean) {
        context.musicDataStore.edit { it[Keys.GESTURES] = enabled }
    }

    suspend fun setLockScreenControls(enabled: Boolean) {
        context.musicDataStore.edit { it[Keys.LOCK_SCREEN_CONTROLS] = enabled }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.musicDataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
    }

    suspend fun setRepeatMode(mode: RepeatModeState) {
        context.musicDataStore.edit { it[Keys.REPEAT_MODE] = mode.name }
    }

    suspend fun setApiCredentials(jamendoClientId: String, customEndpoint: String) {
        context.musicDataStore.edit {
            it[Keys.JAMENDO_CLIENT_ID] = jamendoClientId.trim()
            it[Keys.CUSTOM_STREAM_ENDPOINT] = customEndpoint.trim()
        }
    }
}

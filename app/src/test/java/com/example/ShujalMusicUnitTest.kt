package com.example

import com.example.data.local.UserPreferences
import com.example.data.network.OnlineMusicRepository
import com.example.player.PlaybackStateHolder
import com.example.model.Track
import com.example.ui.theme.ThemeCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShujalMusicUnitTest {

    @Test
    fun themeCatalog_containsAtLeast50ThemesAndClassicMist() {
        assertTrue(ThemeCatalog.allThemes.size >= 50)
        assertEquals("classic_mist", ThemeCatalog.findById("classic_mist").id)
        assertEquals("obsidian_glass", ThemeCatalog.findById("obsidian_glass").id)
    }

    @Test
    fun parseLrcLines_parsesSyncedTimestampsCorrectly() {
        val sample = """
            [00:12.50]First line of song
            [01:05.250]Chorus drops now
        """.trimIndent()
        val lines = OnlineMusicRepository.parseLrcLines(sample)
        assertEquals(2, lines.size)
        assertEquals(12_500L, lines[0].timeMs)
        assertEquals("First line of song", lines[0].text)
        assertEquals(65_250L, lines[1].timeMs)
        assertEquals("Chorus drops now", lines[1].text)
    }

    @Test
    fun playbackStateHolder_tracksForegroundAndOverlayDismissal() {
        val track = Track(
            id = "test_1",
            title = "Midnight Drive",
            artist = "Shujal Audio",
            album = "Neon",
            durationMs = 180_000L,
            uri = "content://media/external/audio/media/1"
        )
        PlaybackStateHolder.update {
            it.copy(currentTrack = track, isPlaying = true, isAppInForeground = false)
        }
        assertEquals("test_1", PlaybackStateHolder.state.value.currentTrack?.id)
        PlaybackStateHolder.dismissOverlayForCurrentTrack()
        assertEquals("test_1", PlaybackStateHolder.state.value.overlayDismissedForTrackId)
        PlaybackStateHolder.resetOverlayDismissal()
        assertEquals(null, PlaybackStateHolder.state.value.overlayDismissedForTrackId)
    }

    @Test
    fun parseLrcLines_handlesMultiTimestampAndOffsetTags() {
        val sample = """
            [offset:500]
            [00:10.00][01:00.50]Repeated chorus line
            [00:25]Simple seconds line
        """.trimIndent()
        val lines = OnlineMusicRepository.parseLrcLines(sample)
        assertEquals(3, lines.size)
        assertEquals(10_500L, lines[0].timeMs)
        assertEquals("Repeated chorus line", lines[0].text)
        assertEquals(25_500L, lines[1].timeMs)
        assertEquals("Simple seconds line", lines[1].text)
        assertEquals(61_000L, lines[2].timeMs)
        assertEquals("Repeated chorus line", lines[2].text)
    }

    @Test
    fun userPreferences_defaultsAreCleanWithCrossfade() {
        val prefs = UserPreferences()
        assertEquals("classic_mist", prefs.themeId)
        assertEquals(3, prefs.crossfadeSeconds)
        assertFalse(prefs.onboardingPermissionsDone)
    }

    @Test
    fun libraryTabs_startWithAllSongsThenOnlineMusic() {
        val tabs = com.example.ui.screens.LibraryTab.entries
        assertEquals(com.example.ui.screens.LibraryTab.SONGS, tabs[0])
        assertEquals(com.example.ui.screens.LibraryTab.ONLINE, tabs[1])
    }

    @Test
    fun onlineSearch_buildsRelatedHindiQueriesAndRanksExactFirst() {
        val related = OnlineMusicRepository.buildRelatedQueries("Tum Hi Ho Song")
        assertTrue(related.any { it.contains("tum hi ho") })

        val t1 = Track("1", "Random Beat", "DJ X", "Mix", durationMs = 180_000, uri = "https://a/1")
        val t2 = Track("2", "Tum Hi Ho Lofi", "Arijit Remix", "Hindi", durationMs = 200_000, uri = "https://a/2")
        val ranked = OnlineMusicRepository.rankAndDeduplicateTracks(listOf(t1, t2), "tum hi ho")
        assertEquals("2", ranked.first().id)
    }

    @Test
    fun dynamicIslandStyle_hasWideCapsuleAndStylishPresets() {
        val styles = com.example.model.DynamicIslandStyle.entries
        assertTrue(styles.size >= 7)
        assertTrue(com.example.model.DynamicIslandStyle.WIDE_CAPSULE in styles)
        assertEquals(com.example.model.DynamicIslandStyle.CLASSIC_PILL, UserPreferences().dynamicIslandStyle)
        assertFalse(UserPreferences().dynamicIslandAlwaysExtended)
    }

    @Test
    fun cameraRingLight_defaultsAndEnumsAreValid() {
        val prefs = UserPreferences()
        assertFalse(prefs.ringLightEnabled)
        assertEquals(0.5f, prefs.ringLightXFraction, 0.001f)
        assertEquals(com.example.model.RingLightTheme.RAINBOW_FLOW, prefs.ringLightTheme)
        assertEquals(com.example.model.RingLightAction.SONG_POPUP, prefs.ringLightTapAction)
        assertEquals(com.example.model.RingLightAction.PREVIOUS_SONG, prefs.ringLightSwipeLeftAction)
        assertEquals(com.example.model.RingLightAction.NEXT_SONG, prefs.ringLightSwipeRightAction)
        assertEquals(com.example.model.RingLightAction.VOLUME_UP, prefs.ringLightSwipeUpAction)
        assertEquals(com.example.model.RingLightAction.VOLUME_DOWN, prefs.ringLightSwipeDownAction)
        assertTrue(com.example.model.RingLightTheme.entries.size >= 8)
        assertTrue(com.example.model.RingLightAction.entries.size >= 9)
    }

    @Test
    fun adMobManager_exposesValidTestAdUnitIdsByDefault() {
        assertTrue(com.example.ads.AdMobManager.bannerUnitId.startsWith("ca-app-pub-"))
        assertTrue(com.example.ads.AdMobManager.interstitialUnitId.startsWith("ca-app-pub-"))
    }

    @Test
    fun videoItem_formatsResolutionAndFileSizeCorrectly() {
        val uhd = com.example.model.VideoItem(
            id = "v1",
            title = "Concert 4K",
            uri = "content://video/1",
            width = 3840,
            height = 2160,
            sizeBytes = 2L * 1_073_741_824L
        )
        assertEquals("4K UHD", uhd.resolutionLabel)
        assertEquals("2.0 GB", uhd.formattedSize)

        val hd = com.example.model.VideoItem(
            id = "v2",
            title = "Clip HD",
            uri = "https://example.com/clip.mp4",
            width = 1280,
            height = 720,
            sizeBytes = 50L * 1_048_576L,
            isOnline = true
        )
        assertEquals("720p HD", hd.resolutionLabel)
        assertEquals("50.0 MB", hd.formattedSize)
    }

    @Test
    fun onlineVideoRepository_buildsDirectAndYouTubeUrlsCorrectly() {
        val repo = com.example.data.network.OnlineVideoRepository()
        val hls = repo.buildDirectUrlVideo("https://cdn.example.com/live/Hindi_Hits.m3u8?token=1")
        assertEquals("Hindi Hits", hls.title)
        assertEquals("application/x-mpegURL", hls.mimeType)
        assertTrue(hls.isOnline)

        val mp4 = repo.buildDirectUrlVideo("https://cdn.example.com/video.mp4", customTitle = "Custom Song Video")
        assertEquals("Custom Song Video", mp4.title)
        assertEquals("video/mp4", mp4.mimeType)

        val ytWatch = repo.buildDirectUrlVideo("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=RD1")
        assertEquals("dQw4w9WgXcQ", ytWatch.youtubeId)
        assertTrue(ytWatch.isOnline)

        val ytShort = repo.buildDirectUrlVideo("https://youtu.be/AbCdEf12345")
        assertEquals("AbCdEf12345", ytShort.youtubeId)

        val ytShorts = repo.buildDirectUrlVideo("https://youtube.com/shorts/XyZ98765432")
        assertEquals("XyZ98765432", ytShorts.youtubeId)
    }

    @Test
    fun videosSectionTab_containsOfflineOnlineAndSavedTabs() {
        val tabs = com.example.ui.screens.VideosSectionTab.entries
        assertEquals(3, tabs.size)
        assertEquals(com.example.ui.screens.VideosSectionTab.OFFLINE, tabs[0])
        assertEquals(com.example.ui.screens.VideosSectionTab.ONLINE, tabs[1])
        assertEquals(com.example.ui.screens.VideosSectionTab.SAVED, tabs[2])
    }
}

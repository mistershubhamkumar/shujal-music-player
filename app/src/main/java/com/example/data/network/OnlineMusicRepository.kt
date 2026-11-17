package com.example.data.network

import com.example.model.LyricLine
import com.example.model.LyricsData
import com.example.model.Track
import com.example.model.TrackSourceType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class OnlineMusicRepository {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "ShujalMusicPlayer/1.0 (Android)")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val lrcApi: LrcLibApi = Retrofit.Builder()
        .baseUrl("https://lrclib.net/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(LrcLibApi::class.java)

    private val jamendoApi: JamendoApi = Retrofit.Builder()
        .baseUrl("https://api.jamendo.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(JamendoApi::class.java)

    private val audiusPrimaryApi: AudiusApi = Retrofit.Builder()
        .baseUrl("https://api.audius.co/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AudiusApi::class.java)

    private val audiusBootstrapApi: AudiusBootstrapApi = Retrofit.Builder()
        .baseUrl("https://api.audius.co/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(AudiusBootstrapApi::class.java)

    private val hearThisApi: HearThisApi = Retrofit.Builder()
        .baseUrl("https://api-v2.hearthis.at/")
        .client(client)
        .build()
        .create(HearThisApi::class.java)

    private val archiveApi: ArchiveOrgApi = Retrofit.Builder()
        .baseUrl("https://archive.org/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ArchiveOrgApi::class.java)

    private fun AudiusTrackDto.toFullTrack(streamBase: String): Track? {
        val trackId = id?.takeIf { it.isNotBlank() } ?: return null
        if (isStreamable == false) return null
        val durationSecLong = (durationSec ?: 0.0).toLong()
        if (durationSecLong < 45L) return null
        val art = artwork?.get("480x480") ?: artwork?.get("1000x1000") ?: artwork?.get("150x150")
        return Track(
            id = "audius_$trackId",
            title = title?.ifBlank { "Untitled Track" } ?: "Untitled Track",
            artist = user?.name?.ifBlank { user.handle ?: "Artist" } ?: "Independent Artist",
            album = genre?.ifBlank { "Full Track" } ?: "Full Track",
            genre = genre ?: "Music",
            durationMs = durationSecLong * 1000L,
            uri = "${streamBase.trimEnd('/')}/v1/tracks/$trackId/stream?app_name=ShujalMusicPlayer",
            artworkUri = art,
            sourceType = TrackSourceType.ONLINE_FULL
        )
    }

    private fun parseHearThisTracks(rawJson: String): List<Track> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return emptyList()
        val array: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("data") ?: obj.optJSONArray("results") ?: return emptyList()
            }
            else -> return emptyList()
        }
        val list = mutableListOf<Track>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
            val streamUrl = item.optString("stream_url").takeIf { it.startsWith("http") } ?: continue
            val durationSec = item.optString("duration").toDoubleOrNull()?.toLong()
                ?: item.optLong("duration", 0L)
            if (durationSec < 45L) continue
            val title = item.optString("title").ifBlank { "Untitled Track" }
            val userObj = item.optJSONObject("user")
            val artist = userObj?.optString("username").orEmpty()
                .ifBlank { userObj?.optString("fullname").orEmpty() }
                .ifBlank { "Independent Artist" }
            val genre = item.optString("genre").ifBlank { "Hindi & Music" }
            val art = item.optString("artwork_url")
                .ifBlank { item.optString("background_url") }
                .ifBlank { userObj?.optString("avatar_url").orEmpty() }
                .takeIf { it.startsWith("http") }
            list += Track(
                id = "hearthis_$id",
                title = title,
                artist = artist,
                album = genre,
                genre = genre,
                durationMs = durationSec * 1000L,
                uri = streamUrl,
                artworkUri = art,
                sourceType = TrackSourceType.ONLINE_FULL
            )
        }
        return list
    }

    private suspend fun fetchHearThisTracks(query: String): List<Track> {
        val body = runCatching {
            if (query.isBlank()) {
                hearThisApi.searchTracks(query = "hindi bollywood", count = 20)
            } else {
                hearThisApi.searchTracks(query = query, count = 20)
            }
        }.getOrNull() ?: return emptyList()

        val raw = runCatching { body.use { it.string() } }.getOrNull().orEmpty()
        return runCatching { parseHearThisTracks(raw) }.getOrElse { emptyList() }
    }

    private suspend fun fetchAudiusTracks(query: String): List<Track> {
        val primary = runCatching {
            val resp = if (query.isBlank()) {
                audiusPrimaryApi.getTrendingTracks()
            } else {
                audiusPrimaryApi.searchTracks(query = query)
            }
            resp.data.mapNotNull { it.toFullTrack("https://api.audius.co") }
        }.getOrElse { emptyList() }
        if (primary.isNotEmpty()) return primary

        return runCatching {
            val hosts = audiusBootstrapApi.getDiscoveryHosts().data
                .filter { it.startsWith("https://") }
                .take(2)
            for (host in hosts) {
                val nodeApi = Retrofit.Builder()
                    .baseUrl(if (host.endsWith("/")) host else "$host/")
                    .client(client)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(AudiusApi::class.java)
                val resp = if (query.isBlank()) {
                    nodeApi.getTrendingTracks()
                } else {
                    nodeApi.searchTracks(query = query)
                }
                val mapped = resp.data.mapNotNull { it.toFullTrack(host) }
                if (mapped.isNotEmpty()) return@runCatching mapped
            }
            emptyList()
        }.getOrElse { emptyList() }
    }

    private fun Any?.asDisplayText(fallback: String): String {
        return when (this) {
            is String -> this.ifBlank { fallback }
            is List<*> -> this.firstOrNull()?.toString()?.ifBlank { fallback } ?: fallback
            else -> fallback
        }
    }

    private suspend fun fetchArchiveFullTracks(query: String): List<Track> = coroutineScope {
        val clean = query.replace(Regex("[^a-zA-Z0-9\\s]"), " ").trim()
        val tokens = clean.split(Regex("\\s+")).filter { it.length >= 2 }
        val lucene = when {
            tokens.isEmpty() -> "(subject:(hindi) OR title:(hindi) OR subject:(bollywood)) AND mediatype:audio"
            tokens.size == 1 -> "(title:(${tokens[0]}) OR creator:(${tokens[0]}) OR subject:(${tokens[0]})) AND mediatype:audio"
            else -> {
                val allTerms = tokens.joinToString(" OR ")
                "(title:($clean) OR creator:($clean) OR title:($allTerms) OR subject:($allTerms)) AND mediatype:audio"
            }
        }
        val searchRes = runCatching { archiveApi.searchAudio(query = lucene, rows = 8) }.getOrNull()
            ?: return@coroutineScope emptyList()

        val tracks = searchRes.response?.docs.orEmpty().mapNotNull { doc ->
            val identifier = doc.identifier?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            async {
                val files = runCatching { archiveApi.getItemFiles(identifier).result }.getOrNull().orEmpty()
                val audioFiles = files.filter { file ->
                    val name = file.name.orEmpty().lowercase()
                    val lenSec = file.length?.toDoubleOrNull()?.toLong() ?: 90L
                    (name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".flac") || name.endsWith(".m4a")) &&
                        lenSec >= 45L
                }.take(4)

                audioFiles.mapNotNull { audioFile ->
                    val rawFileName = audioFile.name ?: return@mapNotNull null
                    val encodedPath = rawFileName.split("/").joinToString("/") {
                        URLEncoder.encode(it, "UTF-8").replace("+", "%20")
                    }
                    val durationMs = ((audioFile.length?.toDoubleOrNull() ?: 180.0) * 1000.0).toLong()
                    val itemTitle = audioFile.title?.takeIf { it.isNotBlank() }
                        ?: rawFileName.substringAfterLast('/').substringBeforeLast('.')
                            .replace("_", " ").replace("-", " ").trim()
                            .ifBlank { doc.title.asDisplayText("Full Track") }
                    Track(
                        id = "archive_${identifier}_${rawFileName.hashCode()}",
                        title = itemTitle,
                        artist = doc.creator.asDisplayText("Archive Artist"),
                        album = doc.title.asDisplayText("Internet Archive"),
                        genre = "Hindi & Open Audio",
                        durationMs = durationMs,
                        uri = "https://archive.org/download/$identifier/$encodedPath",
                        artworkUri = "https://archive.org/services/img/$identifier",
                        sourceType = TrackSourceType.ONLINE_FULL
                    )
                }
            }
        }.awaitAll().flatten()

        tracks
    }

    suspend fun searchOnlineMusic(
        query: String,
        jamendoClientId: String = ""
    ): Result<List<Track>> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = query.trim()
            val relatedQueries = buildRelatedQueries(trimmed)

            val combined = coroutineScope {
                val hearThisPrimary = async { fetchHearThisTracks(trimmed) }
                val audiusPrimary = async { fetchAudiusTracks(trimmed) }
                val archivePrimary = async { fetchArchiveFullTracks(trimmed) }

                // Related ("milte julte") searches run concurrently for YouTube-style breadth
                val relatedHearThis = relatedQueries.map { rq ->
                    async { fetchHearThisTracks(rq) }
                }
                val relatedAudius = relatedQueries.map { rq ->
                    async { fetchAudiusTracks(rq) }
                }
                val jamendoAsync = async {
                    if (jamendoClientId.isBlank()) emptyList()
                    else runCatching {
                        jamendoApi.searchTracks(
                            clientId = jamendoClientId,
                            query = trimmed.ifBlank { "hindi" }
                        ).results
                            .filter { it.audioUrl.isNotBlank() && it.durationSec >= 45L }
                            .map { dto ->
                                Track(
                                    id = "jamendo_${dto.id}",
                                    title = dto.name.ifBlank { "Untitled" },
                                    artist = dto.artistName.ifBlank { "Independent Artist" },
                                    album = dto.albumName.orEmpty().ifBlank { "Jamendo CC" },
                                    genre = "Creative Commons",
                                    durationMs = dto.durationSec * 1000L,
                                    uri = dto.audioUrl,
                                    artworkUri = dto.imageUrl,
                                    sourceType = TrackSourceType.ONLINE_FULL
                                )
                            }
                    }.getOrElse { emptyList() }
                }

                buildList {
                    addAll(jamendoAsync.await())
                    addAll(hearThisPrimary.await())
                    addAll(audiusPrimary.await())
                    addAll(archivePrimary.await())
                    relatedHearThis.awaitAll().forEach { addAll(it) }
                    relatedAudius.awaitAll().forEach { addAll(it) }
                }
            }

            val ranked = rankAndDeduplicateTracks(combined, trimmed)
            if (ranked.isEmpty()) {
                error("No full-length songs matched \"$trimmed\". Try another song name or artist.")
            }
            ranked.take(60)
        }
    }

    suspend fun fetchTrendingTracks(jamendoClientId: String = ""): Result<List<Track>> =
        searchOnlineMusic(query = "", jamendoClientId = jamendoClientId)

    suspend fun fetchSyncedLyrics(
        trackKey: String,
        title: String,
        artist: String,
        album: String
    ): Result<LyricsData> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = title.replace(Regex("\\(.*?\\)|\\[.*?]"), "").trim()
            val cleanArtist = artist.substringBefore("•").trim()
            val direct = runCatching {
                lrcApi.getLyrics(
                    trackName = cleanTitle,
                    artistName = cleanArtist,
                    albumName = album.takeIf {
                        it.isNotBlank() && it != "Single" && it != "Full Track" && it != "Internet Archive"
                    }
                )
            }.getOrNull()

            val best = if (direct != null && (!direct.syncedLyrics.isNullOrBlank() || !direct.plainLyrics.isNullOrBlank())) {
                direct
            } else {
                val searchResults = runCatching {
                    lrcApi.searchLyrics("$cleanTitle $cleanArtist")
                }.getOrNull().orEmpty()
                searchResults.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
                    ?: searchResults.firstOrNull { !it.plainLyrics.isNullOrBlank() }
                    ?: runCatching { lrcApi.searchLyrics(cleanTitle) }.getOrNull()
                        ?.firstOrNull { !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank() }
            } ?: error("No lyrics found for $title")

            val syncedRaw = best.syncedLyrics.orEmpty()
            val plainRaw = best.plainLyrics.orEmpty().ifBlank {
                syncedRaw.replace(Regex("\\[\\d{1,2}:\\d{2}(?:\\.\\d{1,3})?]"), "").trim()
            }
            LyricsData(
                trackKey = trackKey,
                plainLyrics = plainRaw,
                syncedLines = parseLrcLines(syncedRaw),
                isManual = false
            )
        }
    }

    companion object {
        private val stopWords = setOf(
            "song", "songs", "video", "videos", "full", "hd", "4k", "official", "audio",
            "music", "lyrics", "lyric", "status", "mp3", "new", "latest", "best", "hit", "hits"
        )

        fun buildRelatedQueries(rawQuery: String): List<String> {
            val clean = rawQuery.lowercase().trim()
            if (clean.isBlank()) {
                return listOf("hindi romantic", "bollywood hits", "arijit singh lofi")
            }
            val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
            val coreWords = words.filterNot { it in stopWords }.ifEmpty { words }
            val core = coreWords.joinToString(" ")
            val candidates = linkedSetOf<String>()
            if (core.isNotBlank() && core != clean) {
                candidates += core
            }
            if (!clean.contains("hindi") && !clean.contains("bollywood")) {
                candidates += "$core hindi"
            }
            if (coreWords.size >= 2) {
                candidates += coreWords.take(2).joinToString(" ")
            }
            if (!clean.contains("lofi") && !clean.contains("remix")) {
                candidates += "$core lofi"
            }
            return candidates.filter { it != clean }.take(2)
        }

        fun rankAndDeduplicateTracks(tracks: List<Track>, query: String): List<Track> {
            val deduped = tracks.distinctBy { track ->
                "${track.title.lowercase().replace(Regex("[^a-z0-9]"), "")}_${track.artist.lowercase().replace(Regex("[^a-z0-9]"), "")}"
            }
            val clean = query.lowercase().trim()
            if (clean.isBlank()) return deduped
            val tokens = clean.split(Regex("\\s+")).filter { it.length >= 2 && it !in stopWords }
            return deduped.sortedByDescending { track ->
                val titleLower = track.title.lowercase()
                val haystack = "${track.title} ${track.artist} ${track.album} ${track.genre}".lowercase()
                var score = 0
                if (titleLower == clean) score += 100
                if (titleLower.contains(clean)) score += 60
                if (haystack.contains(clean)) score += 40
                tokens.forEach { tok ->
                    if (titleLower.contains(tok)) score += 20
                    else if (haystack.contains(tok)) score += 10
                }
                if (haystack.contains("hindi") || haystack.contains("bollywood") || haystack.contains("punjabi")) {
                    score += 8
                }
                score
            }
        }

        fun parseLrcLines(lrc: String): List<LyricLine> {
            if (lrc.isBlank()) return emptyList()
            val offsetMatch = Regex("\\[offset:([+-]?\\d+)]", RegexOption.IGNORE_CASE).find(lrc)
            val offsetMs = offsetMatch?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            val timeTagRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")

            val parsed = mutableListOf<LyricLine>()
            lrc.lines().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isBlank()) return@forEach
                val matches = timeTagRegex.findAll(line).toList()
                if (matches.isEmpty()) return@forEach

                val text = timeTagRegex.replace(line, "").trim()
                if (text.isBlank()) return@forEach

                matches.forEach { m ->
                    val min = m.groupValues[1].toLongOrNull() ?: 0L
                    val sec = m.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = m.groupValues[3]
                    val ms = when (fracStr.length) {
                        0 -> 0L
                        1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                        2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                        else -> fracStr.take(3).toLongOrNull() ?: 0L
                    }
                    val totalMs = ((min * 60_000L) + (sec * 1_000L) + ms + offsetMs).coerceAtLeast(0L)
                    parsed += LyricLine(totalMs, text)
                }
            }
            return parsed.sortedBy { it.timeMs }
        }
    }
}
